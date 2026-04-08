package com.claudecode.services.lsp;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LspDiagnosticRegistry {

    private final Map<String, CopyOnWriteArrayList<Diagnostic>> diagnostics;
    private final List<DiagnosticListener> listeners;

    public LspDiagnosticRegistry() {
        this.diagnostics = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void addListener(DiagnosticListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DiagnosticListener listener) {
        listeners.remove(listener);
    }

    public void registerDiagnostics(String fileUri, List<Diagnostic> diags) {
        diagnostics.put(fileUri, new CopyOnWriteArrayList<>(diags));
        notifyListeners(fileUri, diags);
    }

    public void clearDiagnostics(String fileUri) {
        diagnostics.remove(fileUri);
        notifyListeners(fileUri, List.of());
    }

    public List<Diagnostic> getDiagnostics(String fileUri) {
        return diagnostics.getOrDefault(fileUri, new CopyOnWriteArrayList<>());
    }

    public Map<String, List<Diagnostic>> getAllDiagnostics() {
        return Map.copyOf(diagnostics);
    }

    public int getTotalDiagnosticCount() {
        return diagnostics.values().stream().mapToInt(List::size).sum();
    }

    public void clearAll() {
        diagnostics.clear();
    }

    private void notifyListeners(String fileUri, List<Diagnostic> diags) {
        for (DiagnosticListener listener : listeners) {
            try {
                listener.onDiagnosticsChanged(fileUri, diags);
            } catch (Exception e) {
                // Log but don't fail
            }
        }
    }

    public interface DiagnosticListener {
        void onDiagnosticsChanged(String fileUri, List<Diagnostic> diagnostics);
    }
}