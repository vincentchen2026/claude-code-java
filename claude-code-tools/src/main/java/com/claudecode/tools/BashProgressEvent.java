package com.claudecode.tools;

import java.time.Instant;
import java.util.concurrent.Flow;

public record BashProgressEvent(
    String taskId,
    BashProgressType type,
    String message,
    double progress,
    long timestamp
) implements Flow.Publisher<BashProgressEvent> {

    public static final double PROGRESS_START = 0.0;
    public static final double PROGRESS_OUTPUT = 0.3;
    public static final double PROGRESS_COMPLETE = 0.9;
    public static final double PROGRESS_DONE = 1.0;

    @Override
    public void subscribe(Flow.Subscriber<? super BashProgressEvent> subscriber) {
        BashProgressSubscription subscription = new BashProgressSubscription(subscriber);
        subscriber.onSubscribe(subscription);
    }

    public enum BashProgressType {
        STARTED,
        OUTPUT,
        OUTPUT_COMPLETE,
        EXIT_CODE,
        ERROR,
        COMPLETED,
        TIMEOUT,
        CANCELLED
    }

    private static class BashProgressSubscription implements Flow.Subscription {
        private final Flow.Subscriber<? super BashProgressEvent> subscriber;
        private volatile boolean cancelled;

        BashProgressSubscription(Flow.Subscriber<? super BashProgressEvent> subscriber) {
            this.subscriber = subscriber;
            this.cancelled = false;
        }

        @Override
        public void request(long n) {
            if (cancelled) return;
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException("demand must be positive"));
                return;
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    public static BashProgressEvent started(String taskId) {
        return new BashProgressEvent(taskId, BashProgressType.STARTED, "Command started", PROGRESS_START, System.currentTimeMillis());
    }

    public static BashProgressEvent output(String taskId, String line, double progress) {
        return new BashProgressEvent(taskId, BashProgressType.OUTPUT, line, progress, System.currentTimeMillis());
    }

    public static BashProgressEvent outputComplete(String taskId, int lineCount) {
        return new BashProgressEvent(taskId, BashProgressType.OUTPUT_COMPLETE, 
            "Output complete: " + lineCount + " lines", PROGRESS_COMPLETE, System.currentTimeMillis());
    }

    public static BashProgressEvent exitCode(String taskId, int exitCode) {
        return new BashProgressEvent(taskId, BashProgressType.EXIT_CODE, 
            "Exit code: " + exitCode, exitCode == 0 ? PROGRESS_DONE : PROGRESS_COMPLETE, System.currentTimeMillis());
    }

    public static BashProgressEvent error(String taskId, String error) {
        return new BashProgressEvent(taskId, BashProgressType.ERROR, error, PROGRESS_COMPLETE, System.currentTimeMillis());
    }

    public static BashProgressEvent completed(String taskId, String summary) {
        return new BashProgressEvent(taskId, BashProgressType.COMPLETED, summary, PROGRESS_DONE, System.currentTimeMillis());
    }

    public static BashProgressEvent timeout(String taskId, int timeoutSeconds) {
        return new BashProgressEvent(taskId, BashProgressType.TIMEOUT, 
            "Command timed out after " + timeoutSeconds + " seconds", PROGRESS_COMPLETE, System.currentTimeMillis());
    }

    public static BashProgressEvent cancelled(String taskId) {
        return new BashProgressEvent(taskId, BashProgressType.CANCELLED, "Command cancelled", PROGRESS_COMPLETE, System.currentTimeMillis());
    }
}