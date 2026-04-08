package com.claudecode.services.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Subprocess management for local shell tasks.
 * Captures stdout/stderr and supports abort.
 */
public class LocalShellTask {

    private static final Logger log = LoggerFactory.getLogger(LocalShellTask.class);

    private final TaskState taskState;
    private final String command;
    private volatile Process process;
    private final StringBuilder output = new StringBuilder();

    public LocalShellTask(TaskState taskState, String command) {
        this.taskState = taskState;
        this.command = command;
    }

    public String getTaskId() {
        return taskState.id();
    }

    /**
     * Starts the shell command. Returns captured output.
     */
    public String execute(String workingDirectory) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        if (workingDirectory != null) {
            pb.directory(new java.io.File(workingDirectory));
        }
        pb.redirectErrorStream(true);

        process = pb.start();
        log.info("Started shell task {} with command: {}", taskState.id(), command);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }

        int exitCode = process.waitFor();
        log.info("Shell task {} completed with exit code {}", taskState.id(), exitCode);
        return output.toString();
    }

    /**
     * Aborts the running process.
     */
    public void abort() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            log.info("Aborted shell task {}", taskState.id());
        }
    }

    public String getOutput() {
        return output.toString();
    }

    public Optional<Integer> getExitCode() {
        if (process != null && !process.isAlive()) {
            return Optional.of(process.exitValue());
        }
        return Optional.empty();
    }
}
