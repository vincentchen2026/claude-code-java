package com.claudecode.services.sync;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SecretGuard {

    private final SecretScanner scanner;
    private final CopyOnWriteArrayList<GuardListener> listeners;
    private volatile boolean blocking;
    private volatile boolean enabled;

    public SecretGuard(SecretScanner scanner) {
        this.scanner = scanner;
        this.listeners = new CopyOnWriteArrayList<>();
        this.blocking = true;
        this.enabled = true;
    }

    public void addListener(GuardListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GuardListener listener) {
        listeners.remove(listener);
    }

    public GuardResult checkWrite(String content, String filePath) {
        if (!enabled) {
            return GuardResult.allow();
        }

        var matches = scanner.scanFile(filePath, content);

        if (matches.isEmpty()) {
            return GuardResult.allow();
        }

        GuardResult result = new GuardResult(false, matches);

        for (GuardListener listener : listeners) {
            listener.onSecretsDetected(filePath, matches);
        }

        if (blocking) {
            return result;
        }

        return result;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public record GuardResult(
        boolean allowed,
        java.util.List<SecretScanner.SecretMatch> secrets
    ) {
        public static GuardResult allow() {
            return new GuardResult(true, java.util.List.of());
        }
    }

    @FunctionalInterface
    public interface GuardListener {
        void onSecretsDetected(String filePath, java.util.List<SecretScanner.SecretMatch> secrets);
    }
}