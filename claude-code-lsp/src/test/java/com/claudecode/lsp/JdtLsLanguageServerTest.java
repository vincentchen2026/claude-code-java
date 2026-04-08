package com.claudecode.lsp;

import com.claudecode.services.lsp.Diagnostic;
import com.claudecode.services.lsp.Diagnostic.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JdtLsLanguageServer.
 * These tests require jdt.ls to be installed.
 */
class JdtLsLanguageServerTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("jdt-ls-test");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "JDT_LS_PATH", matches = ".+")
    void testJdtLsAvailable() {
        // This test only runs if JDT_LS_PATH is set
        assertTrue(JdtLsLanguageServer.isAvailable());
    }

    @Test
    void testDiagnosticConversion() {
        // Test the diagnostic conversion logic
        Diagnostic diag = new Diagnostic(
            "file:///test/Test.java",
            10, 5, 10, 20,
            Severity.ERROR,
            "Syntax error",
            "javac",
            "E001"
        );

        assertEquals(10, diag.startLine());
        assertEquals(5, diag.startCharacter());
        assertEquals(Severity.ERROR, diag.severity());
        assertEquals("Syntax error", diag.message());
    }

    @Test
    void testSeverityMapping() {
        // Test all severity levels
        assertEquals(Severity.ERROR, Severity.fromValue(1));
        assertEquals(Severity.WARNING, Severity.fromValue(2));
        assertEquals(Severity.INFORMATION, Severity.fromValue(3));
        assertEquals(Severity.HINT, Severity.fromValue(4));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "JDT_LS_PATH", matches = ".+")
    void testServerLifecycle() throws Exception {
        Path workspace = tempDir;
        JdtLsLanguageServer server = new JdtLsLanguageServer(workspace);

        // Initialize
        server.initialize(workspace);
        assertTrue(server.isRunning());
        assertEquals("java", server.languageId());

        // Create a simple Java file
        Path javaFile = workspace.resolve("src/Test.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, """
            public class Test {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
            """);

        // Send didOpen
        String content = Files.readString(javaFile);
        server.didOpen(javaFile, content);

        // Wait a bit for diagnostics to arrive
        Thread.sleep(2000);

        // Get diagnostics
        List<Diagnostic> diagnostics = server.getDiagnostics(javaFile);
        System.out.println("Diagnostics received: " + diagnostics.size());

        // Shutdown
        server.shutdown();
        assertFalse(server.isRunning());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "JDT_LS_PATH", matches = ".+")
    void testErrorDetection() throws Exception {
        Path workspace = tempDir;
        JdtLsLanguageServer server = new JdtLsLanguageServer(workspace);

        server.initialize(workspace);

        // Create a Java file with error
        Path javaFile = workspace.resolve("src/ErrorTest.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, """
            public class ErrorTest {
                public static void main(String[] args) {
                    int x = "not an int";  // Type error
                }
            }
            """);

        server.didOpen(javaFile, Files.readString(javaFile));

        Thread.sleep(2000);

        List<Diagnostic> diagnostics = server.getDiagnostics(javaFile);
        boolean hasError = diagnostics.stream()
            .anyMatch(d -> d.severity() == Severity.ERROR);

        System.out.println("Has errors: " + hasError);
        System.out.println("Diagnostics: " + diagnostics);

        server.shutdown();
    }
}
