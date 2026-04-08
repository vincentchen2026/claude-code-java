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
 * WorkflowTool — executes workflow scripts.
 * Task 53.2
 */
public class WorkflowTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "Workflow"; }

    @Override
    public String description() {
        return "Execute a workflow script file. The script will be run as a subprocess.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode pathSchema = mapper().createObjectNode();
        pathSchema.put("type", "string");
        pathSchema.put("description", "Absolute path to the workflow script to execute");
        props.set("script_path", pathSchema);

        ObjectNode argsSchema = mapper().createObjectNode();
        argsSchema.put("type", "string");
        argsSchema.put("description", "Optional arguments to pass to the script");
        props.set("args", argsSchema);

        schema.set("required", mapper().createArrayNode().add("script_path"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String scriptPath = input.has("script_path") ? input.get("script_path").asText() : null;
        String args = input.has("args") ? input.get("args").asText("") : "";

        if (scriptPath == null || scriptPath.isBlank()) {
            return "Error: script_path is required.";
        }

        Path script = Path.of(scriptPath);
        if (!Files.exists(script)) {
            return String.format("Error: script not found: %s", scriptPath);
        }
        if (!Files.isExecutable(script)) {
            return String.format("Error: script is not executable: %s", scriptPath);
        }

        try {
            ProcessBuilder pb;
            if (args.isBlank()) {
                pb = new ProcessBuilder(scriptPath);
            } else {
                pb = new ProcessBuilder(scriptPath, args);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            int exitCode = completed ? process.exitValue() : -1;

            if (!completed) {
                process.destroyForcibly();
                return String.format("Workflow script timed out after 60 seconds.\nPartial output:\n%s", output);
            }

            return String.format("Workflow script exited with code %d:\n%s", exitCode, output);
        } catch (Exception e) {
            return String.format("Error executing workflow: %s", e.getMessage());
        }
    }

    @Override
    public boolean isReadOnly() { return false; }
}
