package com.claudecode.services.lsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages LSP server instances and maintains a diagnostic registry.
 * Provides a unified interface for querying diagnostics across all language servers.
 */
public class LspService {

    private static final Logger LOG = LoggerFactory.getLogger(LspService.class);

    private final Map<String, LspServerInstance> servers;
    private final Map<String, List<Diagnostic>> diagnosticRegistry;

    public LspService() {
        this.servers = new ConcurrentHashMap<>();
        this.diagnosticRegistry = new ConcurrentHashMap<>();
    }

    /**
     * Register an LSP server instance for a language.
     *
     * @param server the server instance
     */
    public void registerServer(LspServerInstance server) {
        servers.put(server.languageId(), server);
        LOG.info("Registered LSP server for language: {}", server.languageId());
    }

    /**
     * Remove an LSP server instance.
     *
     * @param languageId the language ID
     */
    public void unregisterServer(String languageId) {
        LspServerInstance server = servers.remove(languageId);
        if (server != null && server.isRunning()) {
            server.shutdown();
        }
    }

    /**
     * Get diagnostics for a file from all applicable servers.
     *
     * @param filePath the file path
     * @return list of diagnostics from all servers
     */
    public List<Diagnostic> getDiagnostics(Path filePath) {
        // Check registry first
        String key = filePath.toAbsolutePath().toString();
        List<Diagnostic> cached = diagnosticRegistry.get(key);
        if (cached != null) {
            return cached;
        }

        // Query all servers
        List<Diagnostic> allDiagnostics = new ArrayList<>();
        for (LspServerInstance server : servers.values()) {
            if (server.isRunning()) {
                try {
                    allDiagnostics.addAll(server.getDiagnostics(filePath));
                } catch (Exception e) {
                    LOG.warn("Failed to get diagnostics from {} server: {}",
                            server.languageId(), e.getMessage());
                }
            }
        }

        // Cache results
        diagnosticRegistry.put(key, allDiagnostics);
        return allDiagnostics;
    }

    /**
     * Update diagnostics in the registry (called by server push notifications).
     *
     * @param filePath    the file path
     * @param diagnostics the new diagnostics
     */
    public void updateDiagnostics(String filePath, List<Diagnostic> diagnostics) {
        diagnosticRegistry.put(filePath, List.copyOf(diagnostics));
    }

    /**
     * Clear cached diagnostics for a file.
     */
    public void clearDiagnostics(String filePath) {
        diagnosticRegistry.remove(filePath);
    }

    /**
     * Clear all cached diagnostics.
     */
    public void clearAllDiagnostics() {
        diagnosticRegistry.clear();
    }

    /**
     * Get all registered server language IDs.
     */
    public Set<String> getRegisteredLanguages() {
        return Set.copyOf(servers.keySet());
    }

    /**
     * Shut down all servers.
     */
    public void shutdownAll() {
        for (LspServerInstance server : servers.values()) {
            try {
                if (server.isRunning()) {
                    server.shutdown();
                }
            } catch (Exception e) {
                LOG.warn("Error shutting down {} server: {}",
                        server.languageId(), e.getMessage());
            }
        }
        servers.clear();
        diagnosticRegistry.clear();
    }
}
