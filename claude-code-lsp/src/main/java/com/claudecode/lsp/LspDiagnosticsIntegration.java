package com.claudecode.lsp;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.core.engine.ToolResult;
import com.claudecode.services.lsp.Diagnostic;
import com.claudecode.services.lsp.LspService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integrates LSP diagnostics into the tool execution flow.
 * Shows diagnostics after file modifications to provide immediate feedback.
 */
public class LspDiagnosticsIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(LspDiagnosticsIntegration.class);

    private final LspService lspService;
    private final boolean autoShowDiagnostics;

    // Track which files have been modified
    private final Map<String, Long> modifiedFiles = new ConcurrentHashMap<>();

    public LspDiagnosticsIntegration(LspService lspService) {
        this(lspService, true);
    }

    public LspDiagnosticsIntegration(LspService lspService, boolean autoShowDiagnostics) {
        this.lspService = lspService;
        this.autoShowDiagnostics = autoShowDiagnostics;
    }

    /**
     * Notify that a file was modified (tool execution).
     */
    public void onFileModified(Path filePath) {
        if (!isJavaFile(filePath)) return;
        modifiedFiles.put(filePath.toAbsolutePath().toString(), System.currentTimeMillis());
    }

    /**
     * Notify that a file was written.
     */
    public void onFileWritten(Path filePath) {
        if (!isJavaFile(filePath)) return;
        modifiedFiles.put(filePath.toAbsolutePath().toString(), System.currentTimeMillis());
        LOG.debug("File modified for diagnostics: {}", filePath);
    }

    /**
     * Get diagnostics to display after tool execution.
     * Only returns diagnostics for files that were recently modified.
     */
    public String getDiagnosticsToDisplay() {
        if (!autoShowDiagnostics) {
            return "";
        }

        long now = System.currentTimeMillis();
        long cutoff = now - 5000; // Show diagnostics for files modified in last 5 seconds

        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, Long> entry : modifiedFiles.entrySet()) {
            if (entry.getValue() > cutoff) {
                Path filePath = Path.of(entry.getKey());
                List<Diagnostic> diagnostics = lspService.getDiagnostics(filePath);
                
                if (!diagnostics.isEmpty()) {
                    String rendered = DiagnosticRenderer.renderFileDiagnostics(filePath, diagnostics);
                    result.append(rendered);
                }
            }
        }

        // Clean up old entries
        modifiedFiles.entrySet().removeIf(e -> e.getValue() < cutoff - 30000);

        return result.toString();
    }

    /**
     * Get all Java diagnostics for the workspace.
     */
    public String getAllJavaDiagnostics() {
        if (!autoShowDiagnostics || lspService == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (String lang : lspService.getRegisteredLanguages()) {
            // Get diagnostics from registered servers
            // This would need iteration through actual file URIs
        }

        return sb.toString();
    }

    /**
     * Check if there are any errors in recently modified files.
     */
    public boolean hasRecentErrors() {
        long cutoff = System.currentTimeMillis() - 5000;

        for (Map.Entry<String, Long> entry : modifiedFiles.entrySet()) {
            if (entry.getValue() > cutoff) {
                Path filePath = Path.of(entry.getKey());
                List<Diagnostic> diagnostics = lspService.getDiagnostics(filePath);
                
                boolean hasError = diagnostics.stream()
                    .anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
                
                if (hasError) return true;
            }
        }

        return false;
    }

    /**
     * Get summary of diagnostics for recently modified files.
     */
    public String getSummary() {
        long cutoff = System.currentTimeMillis() - 5000;
        
        int totalErrors = 0;
        int totalWarnings = 0;
        int totalFiles = 0;

        for (Map.Entry<String, Long> entry : modifiedFiles.entrySet()) {
            if (entry.getValue() > cutoff) {
                Path filePath = Path.of(entry.getKey());
                List<Diagnostic> diagnostics = lspService.getDiagnostics(filePath);
                
                if (!diagnostics.isEmpty()) {
                    totalFiles++;
                    totalErrors += diagnostics.stream()
                        .filter(d -> d.severity() == Diagnostic.Severity.ERROR)
                        .count();
                    totalWarnings += diagnostics.stream()
                        .filter(d -> d.severity() == Diagnostic.Severity.WARNING)
                        .count();
                }
            }
        }

        if (totalErrors == 0 && totalWarnings == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n[Java Problems] ");
        
        if (totalErrors > 0) {
            sb.append(totalErrors).append(" error").append(totalErrors > 1 ? "s" : "");
        }
        if (totalErrors > 0 && totalWarnings > 0) {
            sb.append(", ");
        }
        if (totalWarnings > 0) {
            sb.append(totalWarnings).append(" warning").append(totalWarnings > 1 ? "s" : "");
        }
        
        sb.append(" in ").append(totalFiles).append(" file").append(totalFiles > 1 ? "s" : "");
        sb.append("\n");

        return sb.toString();
    }

    private boolean isJavaFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".java");
    }

    /**
     * Builder for creating diagnostic integration with custom settings.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LspService lspService;
        private boolean autoShowDiagnostics = true;
        private boolean showWarnings = true;
        private boolean showInfo = false;
        private int maxDiagnosticsPerFile = 100;

        public Builder lspService(LspService lspService) {
            this.lspService = lspService;
            return this;
        }

        public Builder autoShowDiagnostics(boolean autoShowDiagnostics) {
            this.autoShowDiagnostics = autoShowDiagnostics;
            return this;
        }

        public Builder showWarnings(boolean showWarnings) {
            this.showWarnings = showWarnings;
            return this;
        }

        public Builder showInfo(boolean showInfo) {
            this.showInfo = showInfo;
            return this;
        }

        public Builder maxDiagnosticsPerFile(int max) {
            this.maxDiagnosticsPerFile = max;
            return this;
        }

        public LspDiagnosticsIntegration build() {
            return new LspDiagnosticsIntegration(lspService, autoShowDiagnostics);
        }
    }
}
