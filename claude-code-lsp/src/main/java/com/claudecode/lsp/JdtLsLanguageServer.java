package com.claudecode.lsp;

import com.claudecode.services.lsp.Diagnostic;
import com.claudecode.services.lsp.Diagnostic.Severity;
import com.claudecode.services.lsp.LspServerInstance;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Eclipse jdt.ls language server implementation.
 * Communicates with jdt.ls via stdio using LSP4J.
 */
public class JdtLsLanguageServer implements LspServerInstance {

    private static final Logger LOG = LoggerFactory.getLogger(JdtLsLanguageServer.class);
    private static final int TIMEOUT_SECONDS = 30;

    private final Path jdtlsLauncher;
    private final Path workspaceRoot;
    private Process process;
    private LanguageClient languageClient;
    private LanguageServer languageServer;
    private volatile boolean running = false;
    private volatile boolean initialized = false;
    private String languageId = "java";

    // Diagnostic cache keyed by file URI
    private final Map<String, List<Diagnostic>> diagnosticsCache = new ConcurrentHashMap<>();

    private ExecutorService executor;

    public JdtLsLanguageServer(Path workspaceRoot) {
        this(null, workspaceRoot);
    }

    public JdtLsLanguageServer(Path jdtlsLauncher, Path workspaceRoot) {
        this.jdtlsLauncher = jdtlsLauncher;
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public String languageId() {
        return languageId;
    }

    @Override
    public void initialize(Path workspaceRoot) {
        if (running) {
            LOG.warn("jdt.ls already initialized");
            return;
        }

        try {
            Path launcher = findJdtLs();

            LOG.info("Starting jdt.ls from: {}", launcher);

            // Start the jdt.ls process
            ProcessBuilder pb = new ProcessBuilder(launcher.toString());
            pb.directory(workspaceRoot.toFile());
            pb.redirectErrorStream();
            process = pb.start();

            // Create our client implementation
            languageClient = new JdtLanguageClient();

            // Create the launcher
            Launcher<LanguageServer> serverLauncher = Launcher.createLauncher(
                languageClient,
                LanguageServer.class,
                process.getInputStream(),
                process.getOutputStream()
            );

            languageServer = serverLauncher.getRemoteProxy();

            running = true;

            // Start stderr reader thread
            Thread.ofVirtual().start(this::readStderr);

            // Start listening for server messages
            executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> { 
                try {
                    serverLauncher.startListening().get();
                } catch (Exception e) {
                    LOG.error("LSP listener error", e);
                }
                return null;
            });

            // Initialize the server
            InitializeParams params = new InitializeParams();
            params.setRootUri(workspaceRoot.toUri().toString());
            params.setProcessId((int) ProcessHandle.current().pid());
            params.setCapabilities(createClientCapabilities());

            InitializeResult result = languageServer.initialize(params).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            initialized = true;

            LOG.info("jdt.ls initialized, server info: {}",
                result.getServerInfo() != null ? result.getServerInfo().getName() : "unknown");
            LOG.debug("Server capabilities: {}", result.getCapabilities());

        } catch (Exception e) {
            LOG.error("Failed to initialize jdt.ls", e);
            running = false;
            initialized = false;
            throw new RuntimeException("Failed to start jdt.ls", e);
        }
    }

    private ClientCapabilities createClientCapabilities() {
        ClientCapabilities caps = new ClientCapabilities();

        // Workspace capabilities
        WorkspaceClientCapabilities wsCaps = new WorkspaceClientCapabilities();
        wsCaps.setApplyEdit(true);
        caps.setWorkspace(wsCaps);

        // Text document capabilities
        TextDocumentClientCapabilities tdCaps = new TextDocumentClientCapabilities();
        
        // Synchronization - basic document sync
        SynchronizationCapabilities syncCaps = new SynchronizationCapabilities();
        syncCaps.setWillSave(true);
        syncCaps.setWillSaveWaitUntil(true);
        syncCaps.setDidSave(true);
        tdCaps.setSynchronization(syncCaps);

        caps.setTextDocument(tdCaps);

        // Window capabilities
        WindowClientCapabilities windowCaps = new WindowClientCapabilities();
        windowCaps.setWorkDoneProgress(true);
        caps.setWindow(windowCaps);

        return caps;
    }

    private void readStderr() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOG.debug("jdt.ls: {}", line);
            }
        } catch (IOException e) {
            LOG.debug("jdt.ls stderr closed");
        }
    }

    @Override
    public void shutdown() {
        if (!running) return;

        try {
            if (languageServer != null && initialized) {
                languageServer.shutdown().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            LOG.warn("Error during jdt.ls shutdown", e);
        }

        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }

        if (executor != null) {
            executor.shutdownNow();
        }

        running = false;
        initialized = false;
        languageServer = null;
        languageClient = null;
        diagnosticsCache.clear();
        LOG.info("jdt.ls shutdown complete");
    }

    @Override
    public boolean isRunning() {
        return running && initialized && process != null && process.isAlive();
    }

    @Override
    public List<Diagnostic> getDiagnostics(Path filePath) {
        if (!isRunning()) {
            return Collections.emptyList();
        }
        return diagnosticsCache.getOrDefault(filePath.toUri().toString(), Collections.emptyList());
    }

    @Override
    public void didOpen(Path filePath, String content) {
        if (!isRunning()) return;

        try {
            String uri = filePath.toUri().toString();
            TextDocumentItem textDocItem = new TextDocumentItem(uri, "java", 1, content);

            languageServer.getTextDocumentService()
                .didOpen(new DidOpenTextDocumentParams(textDocItem));
            LOG.debug("Sent didOpen for {}", uri);
        } catch (Exception e) {
            LOG.warn("Failed to send didOpen for {}: {}", filePath, e.getMessage());
        }
    }

    @Override
    public void didChange(Path filePath, String content) {
        if (!isRunning()) return;

        try {
            String uri = filePath.toUri().toString();
            Range range = new Range(new Position(0, 0), new Position(Integer.MAX_VALUE, Integer.MAX_VALUE));
            TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent(range, null, content);
            VersionedTextDocumentIdentifier textDocId = new VersionedTextDocumentIdentifier(uri, 1);

            languageServer.getTextDocumentService().didChange(
                new DidChangeTextDocumentParams(textDocId, List.of(change))
            );
            LOG.debug("Sent didChange for {}", uri);
        } catch (Exception e) {
            LOG.warn("Failed to send didChange for {}: {}", filePath, e.getMessage());
        }
    }

    @Override
    public void didClose(Path filePath) {
        if (!isRunning()) return;

        try {
            String uri = filePath.toUri().toString();
            VersionedTextDocumentIdentifier textDocId = new VersionedTextDocumentIdentifier(uri, 1);
            languageServer.getTextDocumentService().didClose(
                new DidCloseTextDocumentParams(textDocId)
            );
            diagnosticsCache.remove(uri);
            LOG.debug("Sent didClose for {}", uri);
        } catch (Exception e) {
            LOG.warn("Failed to send didClose for {}: {}", filePath, e.getMessage());
        }
    }

    // ========== Diagnostic handling ==========

    /**
     * Handle publishDiagnostics notification from jdt.ls.
     */
    public void onPublishDiagnostics(PublishDiagnosticsParams params) {
        String uri = params.getUri();
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (org.eclipse.lsp4j.Diagnostic lspDiag : params.getDiagnostics()) {
            diagnostics.add(convertDiagnostic(uri, lspDiag));
        }

        diagnosticsCache.put(uri, diagnostics);
        LOG.debug("Cached {} diagnostics for {}", diagnostics.size(), uri);
    }

    private Diagnostic convertDiagnostic(String uri, org.eclipse.lsp4j.Diagnostic lspDiag) {
        Severity severity = mapSeverity(lspDiag.getSeverity());
        String message = lspDiag.getMessage();
        String source = lspDiag.getSource();
        String code = lspDiag.getCode() != null ? lspDiag.getCode().toString() : null;

        Range range = lspDiag.getRange();
        return new Diagnostic(
            uri,
            range.getStart().getLine(),
            range.getStart().getCharacter(),
            range.getEnd().getLine(),
            range.getEnd().getCharacter(),
            severity, message, source, code
        );
    }

    private Severity mapSeverity(org.eclipse.lsp4j.DiagnosticSeverity severity) {
        if (severity == null) return Severity.INFORMATION;
        return switch (severity) {
            case Error -> Severity.ERROR;
            case Warning -> Severity.WARNING;
            case Information -> Severity.INFORMATION;
            case Hint -> Severity.HINT;
            default -> Severity.INFORMATION;
        };
    }

    // ========== Language client implementation ==========

    /**
     * Language client implementation for receiving server notifications.
     */
    private class JdtLanguageClient implements LanguageClient {

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams params) {
            JdtLsLanguageServer.this.onPublishDiagnostics(params);
        }

        @Override
        public void logMessage(MessageParams message) {
            LOG.debug("jdt.ls log: {}", message.getMessage());
        }

        @Override
        public void showMessage(MessageParams messageParams) {
            LOG.info("jdt.ls message [{}]: {}", messageParams.getType(), messageParams.getMessage());
        }

        @Override
        public void telemetryEvent(Object object) {
            LOG.debug("jdt.ls telemetry: {}", object);
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
            LOG.debug("jdt.ls message request: {}", requestParams.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    // ========== Utility methods ==========

    /**
     * Find jdt.ls launcher script.
     */
    private Path findJdtLs() {
        if (jdtlsLauncher != null && Files.exists(jdtlsLauncher)) {
            return jdtlsLauncher;
        }

        // Check environment variable first
        String envPath = System.getenv("JDT_LS_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            Path p = Path.of(envPath);
            if (Files.exists(p)) {
                LOG.info("Found jdt.ls from JDT_LS_PATH: {}", envPath);
                return p;
            }
        }

        // Common locations for jdt.ls
        List<Path> candidates = List.of(
            // VS Code extension (various versions)
            Path.of(System.getProperty("user.home"), ".vscode", "extensions", "redhat.java",
                "1.0.0", "server", "jdt.ls", "bin", "jdt.ls.sh"),
            Path.of(System.getProperty("user.home"), ".vscode", "extensions", "redhat.java",
                "1.30.0", "server", "jdt.ls", "bin", "jdt.ls.sh"),
            Path.of(System.getProperty("user.home"), ".vscode", "extensions", "redhat.java",
                "server", "jdt.ls", "bin", "jdt.ls.sh"),
            // Linux/macOS common installs
            Path.of(System.getProperty("user.home"), ".local", "share", "jdt.ls", "bin", "jdt.ls.sh"),
            Path.of("/usr/local/lib/jdt.ls/bin/jdt.ls.sh"),
            Path.of("/opt/jdt.ls/bin/jdt.ls.sh"),
            // Standalone jdt.ls
            Path.of(System.getProperty("user.home"), "jdt-language-server", "jdt.ls", "bin", "jdt.ls.sh")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                LOG.info("Found jdt.ls at: {}", candidate);
                return candidate;
            }
        }

        throw new RuntimeException(
            "jdt.ls not found. Please install Eclipse jdt.ls:\n" +
            "1. Download from https://github.com/eclipse-jdtls/eclipse.jdt.ls/releases\n" +
            "2. Or install VS Code 'Language Support for Java' extension\n" +
            "3. Set JDT_LS_PATH environment variable to the launcher path"
        );
    }

    /**
     * Get all cached diagnostics for the workspace.
     */
    public Map<String, List<Diagnostic>> getAllDiagnostics() {
        return Collections.unmodifiableMap(diagnosticsCache);
    }

    /**
     * Get diagnostic count for a file.
     */
    public int getDiagnosticCount(Path filePath) {
        return getDiagnostics(filePath).size();
    }

    /**
     * Get error count for a file.
     */
    public int getErrorCount(Path filePath) {
        return (int) getDiagnostics(filePath).stream()
            .filter(d -> d.severity() == Severity.ERROR)
            .count();
    }

    /**
     * Check if jdt.ls is available on this system.
     */
    public static boolean isAvailable() {
        try {
            String envPath = System.getenv("JDT_LS_PATH");
            if (envPath != null && Files.exists(Path.of(envPath))) {
                return true;
            }

            Path vscodePath = Path.of(System.getProperty("user.home"), ".vscode", "extensions",
                "redhat.java", "server", "jdt.ls", "bin", "jdt.ls.sh");
            return Files.exists(vscodePath);
        } catch (Exception ignored) {
            return false;
        }
    }
}
