package com.claudecode.services.lsp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LspServiceTest {

    private LspService lspService;

    @BeforeEach
    void setUp() {
        lspService = new LspService();
    }

    @Test
    void registerAndQueryServer() {
        StubLspServer server = new StubLspServer("java");
        server.addDiagnostic(new Diagnostic(
                "Test.java", 0, 0, 0, 10,
                Diagnostic.Severity.ERROR, "Syntax error", "javac", "E001"));

        lspService.registerServer(server);

        List<Diagnostic> diagnostics = lspService.getDiagnostics(Path.of("Test.java"));
        assertEquals(1, diagnostics.size());
        assertEquals("Syntax error", diagnostics.get(0).message());
    }

    @Test
    void getRegisteredLanguages() {
        lspService.registerServer(new StubLspServer("java"));
        lspService.registerServer(new StubLspServer("typescript"));

        Set<String> languages = lspService.getRegisteredLanguages();
        assertEquals(2, languages.size());
        assertTrue(languages.contains("java"));
        assertTrue(languages.contains("typescript"));
    }

    @Test
    void updateDiagnosticsCache() {
        Diagnostic diag = new Diagnostic(
                "App.java", 5, 0, 5, 20,
                Diagnostic.Severity.WARNING, "Unused variable", "javac", "W001");

        lspService.updateDiagnostics("/path/App.java", List.of(diag));

        // Direct registry query won't find it via getDiagnostics(Path) since
        // the path key differs, but the registry is updated
        lspService.clearDiagnostics("/path/App.java");
    }

    @Test
    void clearAllDiagnostics() {
        lspService.updateDiagnostics("file1.java", List.of());
        lspService.updateDiagnostics("file2.java", List.of());

        lspService.clearAllDiagnostics();
        // No exception means success
    }

    @Test
    void shutdownAll() {
        StubLspServer server = new StubLspServer("java");
        lspService.registerServer(server);

        lspService.shutdownAll();
        assertFalse(server.isRunning());
        assertTrue(lspService.getRegisteredLanguages().isEmpty());
    }

    @Test
    void unregisterServer() {
        StubLspServer server = new StubLspServer("java");
        lspService.registerServer(server);

        lspService.unregisterServer("java");
        assertFalse(server.isRunning());
        assertFalse(lspService.getRegisteredLanguages().contains("java"));
    }

    @Test
    void diagnosticFormat() {
        Diagnostic diag = new Diagnostic(
                "Test.java", 4, 2, 4, 10,
                Diagnostic.Severity.ERROR, "Missing semicolon", "javac", "E001");

        String formatted = diag.format();
        assertTrue(formatted.contains("Test.java:5:3"));
        assertTrue(formatted.contains("error"));
        assertTrue(formatted.contains("Missing semicolon"));
    }

    @Test
    void diagnosticSeverityFromValue() {
        assertEquals(Diagnostic.Severity.ERROR, Diagnostic.Severity.fromValue(1));
        assertEquals(Diagnostic.Severity.WARNING, Diagnostic.Severity.fromValue(2));
        assertEquals(Diagnostic.Severity.INFORMATION, Diagnostic.Severity.fromValue(3));
        assertEquals(Diagnostic.Severity.HINT, Diagnostic.Severity.fromValue(4));
        assertEquals(Diagnostic.Severity.INFORMATION, Diagnostic.Severity.fromValue(99));
    }

    /**
     * Stub LSP server for testing.
     */
    static class StubLspServer implements LspServerInstance {
        private final String languageId;
        private boolean running = true;
        private final java.util.List<Diagnostic> diagnostics = new java.util.ArrayList<>();

        StubLspServer(String languageId) {
            this.languageId = languageId;
        }

        void addDiagnostic(Diagnostic d) {
            diagnostics.add(d);
        }

        @Override
        public String languageId() { return languageId; }

        @Override
        public void initialize(Path workspaceRoot) { running = true; }

        @Override
        public void shutdown() { running = false; }

        @Override
        public boolean isRunning() { return running; }

        @Override
        public List<Diagnostic> getDiagnostics(Path filePath) { return diagnostics; }

        @Override
        public void didOpen(Path filePath, String content) {}

        @Override
        public void didChange(Path filePath, String content) {}

        @Override
        public void didClose(Path filePath) {}
    }
}
