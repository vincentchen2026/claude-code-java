package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class StreamingBashExecutor {

    private static final Map<String, StreamingBashTask> ACTIVE_TASKS = new ConcurrentHashMap<>();

    private final ToolExecutionContext context;
    private final Flow.Subscriber<? super BashProgressEvent> progressSubscriber;

    public StreamingBashExecutor(ToolExecutionContext context, Flow.Subscriber<? super BashProgressEvent> progressSubscriber) {
        this.context = context;
        this.progressSubscriber = progressSubscriber;
    }

    public StreamingBashExecutor(ToolExecutionContext context) {
        this(context, null);
    }

    public BashResult execute(String command, int timeoutSeconds) {
        String taskId = "bash_" + System.currentTimeMillis();
        
        publish(BashProgressEvent.started(taskId));

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.directory(Path.of(context.workingDirectory()).toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();
            
            StreamingBashTask task = new StreamingBashTask(taskId, command, process, context);
            ACTIVE_TASKS.put(taskId, task);

            AtomicLong lineCount = new AtomicLong(0);
            AtomicReference<String> lastOutput = new AtomicReference<>("");

            Thread.ofVirtual().name("bash-stdout-" + taskId).start(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        long count = lineCount.incrementAndGet();
                        double progress = BashProgressEvent.PROGRESS_OUTPUT + (0.1 * Math.min(count / 100.0, 1.0));
                        publish(BashProgressEvent.output(taskId, line, progress));
                        lastOutput.set(line);
                    }
                } catch (IOException ignored) {
                }
            });

            Thread.ofVirtual().name("bash-stderr-" + taskId).start(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        long count = lineCount.incrementAndGet();
                        publish(BashProgressEvent.output(taskId, "[stderr] " + line, BashProgressEvent.PROGRESS_OUTPUT));
                        lastOutput.set(line);
                    }
                } catch (IOException ignored) {
                }
            });

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                publish(BashProgressEvent.timeout(taskId, timeoutSeconds));
                ACTIVE_TASKS.remove(taskId);
                return new BashResult(taskId, false, "Command timed out after " + timeoutSeconds + " seconds", -1, lineCount.get());
            }

            int exitCode = process.exitValue();
            publish(BashProgressEvent.exitCode(taskId, exitCode));

            String summary = exitCode == 0 ? "Command completed successfully" : "Command failed with exit code " + exitCode;
            publish(BashProgressEvent.completed(taskId, summary));

            ACTIVE_TASKS.remove(taskId);

            return new BashResult(taskId, true, lastOutput.get(), exitCode, lineCount.get());

        } catch (Exception e) {
            publish(BashProgressEvent.error(taskId, e.getMessage()));
            ACTIVE_TASKS.remove(taskId);
            return new BashResult(taskId, false, e.getMessage(), -1, 0);
        }
    }

    public CompletableBashTask executeAsync(String command, int timeoutSeconds) {
        String taskId = "bash_async_" + System.currentTimeMillis();
        
        publish(BashProgressEvent.started(taskId));

        java.util.concurrent.CompletableFuture<BashResult> future = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            return execute(command, timeoutSeconds);
        });

        return new CompletableBashTask(taskId, future, this);
    }

    public static StreamingBashTask getTask(String taskId) {
        return ACTIVE_TASKS.get(taskId);
    }

    public static void cancelTask(String taskId) {
        StreamingBashTask task = ACTIVE_TASKS.get(taskId);
        if (task != null) {
            task.cancel();
            ACTIVE_TASKS.remove(taskId);
        }
    }

    public static int getActiveTaskCount() {
        return ACTIVE_TASKS.size();
    }

    private void publish(BashProgressEvent event) {
        if (progressSubscriber != null) {
            progressSubscriber.onNext(event);
        }
    }

    private void publishError(Throwable error) {
        if (progressSubscriber != null) {
            progressSubscriber.onError(error);
        }
    }

    public record BashResult(
        String taskId,
        boolean success,
        String lastLine,
        int exitCode,
        long lineCount
    ) {}

    public record CompletableBashTask(
        String taskId,
        java.util.concurrent.CompletableFuture<BashResult> future,
        StreamingBashExecutor executor
    ) {
        public boolean cancel() {
            return future.cancel(true);
        }
    }

    public static class StreamingBashTask {
        private final String taskId;
        private final String command;
        private final Process process;
        private final ToolExecutionContext context;
        private volatile boolean cancelled;

        StreamingBashTask(String taskId, String command, Process process, ToolExecutionContext context) {
            this.taskId = taskId;
            this.command = command;
            this.process = process;
            this.context = context;
            this.cancelled = false;
        }

        public String taskId() { return taskId; }
        public String command() { return command; }
        public Process process() { return process; }
        public boolean isCancelled() { return cancelled; }
        public boolean isAlive() { return process.isAlive(); }

        public void cancel() {
            cancelled = true;
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}