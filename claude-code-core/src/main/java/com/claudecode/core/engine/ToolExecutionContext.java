package com.claudecode.core.engine;

import java.util.function.Consumer;

/**
 * Context provided to tool executors during execution.
 */
public record ToolExecutionContext(
    AbortController abortController,
    String sessionId,
    String workingDirectory,
    ProgressSink progressSink
) {

    public static ToolExecutionContext of(AbortController abortController, String sessionId) {
        return new ToolExecutionContext(abortController, sessionId, System.getProperty("user.dir"), ProgressSink.NOOP);
    }

    public static ToolExecutionContext of(AbortController abortController, String sessionId, ProgressSink progressSink) {
        return new ToolExecutionContext(abortController, sessionId, System.getProperty("user.dir"), progressSink);
    }

    public void reportProgress(double progress, String message) {
        progressSink.accept(new ProgressUpdate(progress, message, System.currentTimeMillis()));
    }

    public record ProgressUpdate(
        double progress,
        String message,
        long timestamp
    ) {}

    @FunctionalInterface
    public interface ProgressSink extends Consumer<ProgressUpdate> {
        ProgressSink NOOP = update -> {};
    }
}
