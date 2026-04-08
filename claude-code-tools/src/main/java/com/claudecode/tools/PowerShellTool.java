package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * PowerShellTool — execute PowerShell commands (Windows).
 * Input: {command}. Uses ProcessBuilder with "powershell" or "pwsh".
 */
public class PowerShellTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    @Override public String name() { return "PowerShell"; }

    @Override public String description() { return "Execute PowerShell commands (Windows)"; }

    @Override public JsonNode inputSchema() { return SCHEMA; }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String command = input.has("command") ? input.get("command").asText("") : "";
        int timeoutSeconds = input.has("timeout") ? input.get("timeout").asInt(DEFAULT_TIMEOUT_SECONDS) : DEFAULT_TIMEOUT_SECONDS;

        if (command.isBlank()) {
            return "Error: command is required";
        }

        try {
            return executeCommand(command, timeoutSeconds, context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: command was interrupted";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    private String executeCommand(String command, int timeoutSeconds,
                                   ToolExecutionContext context) throws IOException, InterruptedException {
        // Try pwsh (PowerShell Core) first, fall back to powershell (Windows PowerShell)
        String shell = findPowerShell();
        ProcessBuilder pb = new ProcessBuilder(shell, "-NoProfile", "-Command", command);
        pb.directory(Path.of(context.workingDirectory()).toFile());
        pb.redirectErrorStream(false);

        Process process = pb.start();

        context.abortController().onAbort(() -> {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        });

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread stdoutThread = Thread.ofVirtual().start(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append('\n');
                }
            } catch (IOException ignored) { }
        });

        Thread stderrThread = Thread.ofVirtual().start(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append('\n');
                }
            } catch (IOException ignored) { }
        });

        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            stdoutThread.join(1000);
            stderrThread.join(1000);
            return "Error: command timed out after " + timeoutSeconds + " seconds\n" + stdout + stderr;
        }

        stdoutThread.join(5000);
        stderrThread.join(5000);

        int exitCode = process.exitValue();
        StringBuilder result = new StringBuilder();
        if (!stdout.isEmpty()) result.append(stdout);
        if (!stderr.isEmpty()) {
            if (!result.isEmpty()) result.append('\n');
            result.append(stderr);
        }
        if (exitCode != 0) {
            result.append("\nExit code: ").append(exitCode);
        }
        return result.toString();
    }

    private static String findPowerShell() {
        // Prefer pwsh (cross-platform PowerShell Core)
        try {
            Process p = new ProcessBuilder("pwsh", "-Version").start();
            p.waitFor(2, TimeUnit.SECONDS);
            if (p.exitValue() == 0) return "pwsh";
        } catch (Exception ignored) { }
        return "powershell";
    }

    @Override
    public boolean isEnabled() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");

        ObjectNode commandProp = properties.putObject("command");
        commandProp.put("type", "string");
        commandProp.put("description", "The PowerShell command to execute");

        ObjectNode timeoutProp = properties.putObject("timeout");
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "Timeout in seconds (default 120)");
        timeoutProp.put("default", 120);

        ArrayNode required = schema.putArray("required");
        required.add("command");
        return schema;
    }
}
