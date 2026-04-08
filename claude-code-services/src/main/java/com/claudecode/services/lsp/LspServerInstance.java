package com.claudecode.services.lsp;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for LSP server communication.
 * Stub interface — actual protocol communication to be implemented later.
 */
public interface LspServerInstance {

    /**
     * Get the language ID this server handles (e.g., "java", "typescript").
     */
    String languageId();

    /**
     * Initialize the LSP server for the given workspace.
     */
    void initialize(Path workspaceRoot);

    /**
     * Shut down the LSP server.
     */
    void shutdown();

    /**
     * Check if the server is running.
     */
    boolean isRunning();

    /**
     * Request diagnostics for a file.
     *
     * @param filePath the file to get diagnostics for
     * @return list of diagnostics
     */
    List<Diagnostic> getDiagnostics(Path filePath);

    /**
     * Notify the server that a file was opened.
     */
    void didOpen(Path filePath, String content);

    /**
     * Notify the server that a file was changed.
     */
    void didChange(Path filePath, String content);

    /**
     * Notify the server that a file was closed.
     */
    void didClose(Path filePath);
}
