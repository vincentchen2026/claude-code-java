package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TerminalCaptureTool — captures terminal output.
 * Task 53.4
 *
 * Captures output from shell commands or provides terminal buffer access.
 * Uses shell-based capture when direct terminal access is unavailable.
 */
public class TerminalCaptureTool extends Tool<JsonNode, String> {

    private static final ConcurrentMap<String, List<String>> SESSION_CAPTURES = new ConcurrentHashMap<>();

    @Override
    public String name() { return "TerminalCapture"; }

    @Override
    public String description() {
        return "Capture recent terminal output. Specify the number of lines to capture from the terminal buffer.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode linesSchema = mapper().createObjectNode();
        linesSchema.put("type", "integer");
        linesSchema.put("minimum", 1);
        linesSchema.put("maximum", 1000);
        linesSchema.put("description", "Number of lines to capture from terminal output (default: 50)");
        props.set("lines", linesSchema);

        ObjectNode sessionSchema = mapper().createObjectNode();
        sessionSchema.put("type", "string");
        sessionSchema.put("description", "Session ID to capture (default: current session)");
        props.set("session_id", sessionSchema);

        ObjectNode commandSchema = mapper().createObjectNode();
        commandSchema.put("type", "string");
        commandSchema.put("description", "Shell command to run and capture output (optional)");
        props.set("command", commandSchema);

        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        int lines = input.has("lines") ? input.get("lines").asInt(50) : 50;
        lines = Math.max(1, Math.min(1000, lines));
        String sessionId = input.has("session_id") ? input.get("session_id").asText() : context.sessionId();
        String command = input.has("command") ? input.get("command").asText() : null;

        if (command != null && !command.isBlank()) {
            return captureCommandOutput(command, lines);
        }

        return captureSessionOutput(sessionId, lines, context);
    }

    private String captureCommandOutput(String command, int maxLines) {
        StringBuilder sb = new StringBuilder();
        sb.append("Command Output Capture\n");
        sb.append("======================\n\n");
        sb.append("Command: ").append(command).append("\n\n");

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            List<String> outputLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null && outputLines.size() < maxLines) {
                    outputLines.add(line);
                }
            }

            int totalLines = outputLines.size();
            sb.append("Captured ").append(totalLines).append(" lines:\n\n");

            for (int i = 0; i < outputLines.size(); i++) {
                sb.append(String.format("%6d  %s\n", i + 1, outputLines.get(i)));
            }

            if (totalLines == maxLines) {
                sb.append("\n... output truncated at ").append(maxLines).append(" lines\n");
            }

        } catch (Exception e) {
            sb.append("Error capturing output: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    private String captureSessionOutput(String sessionId, int maxLines, ToolExecutionContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Terminal Session Capture\n");
        sb.append("=========================\n\n");
        sb.append("Session: ").append(sessionId).append("\n");
        sb.append("Requested lines: ").append(maxLines).append("\n\n");

        List<String> captured = SESSION_CAPTURES.getOrDefault(sessionId, List.of());

        if (captured.isEmpty()) {
            sb.append("No captured output for this session.\n\n");
            sb.append("Note: Terminal capture requires shell integration.\n");
            sb.append("Use /terminal capture command=\"your command\" to capture specific output.\n");
            return sb.toString();
        }

        int startIdx = Math.max(0, captured.size() - maxLines);
        List<String> toShow = captured.subList(startIdx, captured.size());

        sb.append("Recent output (").append(toShow.size()).append(" lines):\n\n");

        for (int i = startIdx; i < captured.size(); i++) {
            sb.append(String.format("%6d  %s\n", i + 1, captured.get(i)));
        }

        return sb.toString();
    }

    /**
     * Add output lines to a session capture buffer.
     * Called by the shell integration when capturing output.
     */
    public static void captureLines(String sessionId, List<String> lines) {
        SESSION_CAPTURES.compute(sessionId, (k, existing) -> {
            List<String> capture = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
            capture.addAll(lines);
            int maxSize = 10000;
            if (capture.size() > maxSize) {
                capture = capture.subList(capture.size() - maxSize, capture.size());
            }
            return List.copyOf(capture);
        });
    }

    /**
     * Clear capture buffer for a session.
     */
    public static void clearCapture(String sessionId) {
        SESSION_CAPTURES.remove(sessionId);
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public boolean isConcurrencySafe() { return true; }
}
