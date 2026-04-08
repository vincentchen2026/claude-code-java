package com.claudecode.services.lsp;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class PassiveFeedback {

    private final LspDiagnosticRegistry registry;
    private final CopyOnWriteArrayList<FeedbackListener> listeners;
    private final AtomicBoolean enabled;

    public PassiveFeedback(LspDiagnosticRegistry registry) {
        this.registry = registry;
        this.listeners = new CopyOnWriteArrayList<>();
        this.enabled = new AtomicBoolean(true);
        
        this.registry.addListener((uri, diags) -> {
            if (enabled.get()) {
                analyzeAndFeedback(uri, diags);
            }
        });
    }

    public void addListener(FeedbackListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FeedbackListener listener) {
        listeners.remove(listener);
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    private void analyzeAndFeedback(String fileUri, java.util.List<Diagnostic> diags) {
        Feedback feedback = analyzeDiagnostics(fileUri, diags);
        
        for (FeedbackListener listener : listeners) {
            try {
                listener.onFeedback(feedback);
            } catch (Exception e) {
                // Log but don't fail
            }
        }
    }

    private Feedback analyzeDiagnostics(String fileUri, java.util.List<Diagnostic> diags) {
        int errorCount = 0;
        int warningCount = 0;
        int infoCount = 0;
        
        for (Diagnostic diag : diags) {
            switch (diag.severity()) {
                case ERROR -> errorCount++;
                case WARNING -> warningCount++;
                case INFORMATION -> infoCount++;
            }
        }
        
        return new Feedback(
            fileUri,
            errorCount,
            warningCount,
            infoCount,
            diags,
            System.currentTimeMillis()
        );
    }

    public record Feedback(
        String fileUri,
        int errorCount,
        int warningCount,
        int infoCount,
        java.util.List<Diagnostic> diagnostics,
        long timestamp
    ) {
        public boolean hasErrors() {
            return errorCount > 0;
        }
        
        public boolean hasWarnings() {
            return warningCount > 0;
        }
    }

    @FunctionalInterface
    public interface FeedbackListener {
        void onFeedback(Feedback feedback);
    }
}