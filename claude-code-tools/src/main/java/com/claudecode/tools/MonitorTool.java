package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MonitorTool — monitors a command's output stream.
 * Task 53.1
 */
public class MonitorTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "Monitor"; }

    @Override
    public String description() {
        return "Monitor the output of a running command. Attach to a process and stream its output.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode pidSchema = mapper().createObjectNode();
        pidSchema.put("type", "integer");
        pidSchema.put("minimum", 1);
        pidSchema.put("description", "Process ID to monitor");
        props.set("pid", pidSchema);

        ObjectNode linesSchema = mapper().createObjectNode();
        linesSchema.put("type", "integer");
        linesSchema.put("minimum", 1);
        linesSchema.put("maximum", 1000);
        linesSchema.put("description", "Number of recent lines to capture (default: 100)");
        props.set("lines", linesSchema);

        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        int pid = input.has("pid") ? input.get("pid").asInt(-1) : -1;
        int lines = input.has("lines") ? input.get("lines").asInt(100) : 100;

        if (pid <= 0) {
            return "Error: valid pid is required.";
        }

        try {
            // Check if process is still running
            ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
            if (handle == null) {
                return String.format("Process %d not found.", pid);
            }

            boolean alive = handle.isAlive();
            String info = handle.info().toString();

            return String.format(
                "Monitor for PID %d:\n" +
                "  Alive: %s\n" +
                "  Info: %s\n" +
                "  [Live output streaming would be implemented via process handle in full version]",
                pid, alive, info);
        } catch (Exception e) {
            return String.format("Error monitoring process: %s", e.getMessage());
        }
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public boolean isConcurrencySafe() { return true; }
}
