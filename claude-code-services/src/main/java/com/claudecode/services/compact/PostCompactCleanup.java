package com.claudecode.services.compact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PostCompactCleanup {

    private static final Logger log = LoggerFactory.getLogger(PostCompactCleanup.class);

    private final List<CleanupAction> actions;

    public PostCompactCleanup() {
        this.actions = new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    public void addAction(CleanupAction action) {
        actions.add(action);
    }

    public CompletableFuture<Void> execute(PostCompactContext context) {
        return CompletableFuture.runAsync(() -> {
            for (CleanupAction action : actions) {
                try {
                    action.execute(context);
                } catch (Exception e) {
                    log.error("Cleanup action {} failed", action.name(), e);
                }
            }
        });
    }

    public void executeSync(PostCompactContext context) {
        for (CleanupAction action : actions) {
            try {
                action.execute(context);
            } catch (Exception e) {
                log.error("Cleanup action {} failed", action.name(), e);
            }
        }
    }

    public void clear() {
        actions.clear();
    }

    public int getActionCount() {
        return actions.size();
    }

    public interface CleanupAction {
        String name();
        void execute(PostCompactContext context);
    }

    public record PostCompactContext(
        String sessionId,
        int preCompactMessageCount,
        int postCompactMessageCount,
        long preCompactTokenCount,
        long postCompactTokenCount,
        long elapsedMs,
        CompactionResult result
    ) {}
}