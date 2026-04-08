package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * REPLTool — REPL interaction tool.
 * Task 54.3
 */
public class REPLTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "REPL"; }

    @Override
    public String description() {
        return "Execute code in a REPL (Read-Eval-Print Loop). " +
               "Supports Python, Node.js, and other interpreters.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode langSchema = mapper().createObjectNode();
        langSchema.put("type", "string");
        langSchema.put("enum", mapper().createArrayNode().add("python").add("node").add("bash"));
        langSchema.put("description", "Language/interpreter to use");
        props.set("language", langSchema);

        ObjectNode codeSchema = mapper().createObjectNode();
        codeSchema.put("type", "string");
        codeSchema.put("description", "Code to execute");
        props.set("code", codeSchema);

        schema.set("required", mapper().createArrayNode().add("language").add("code"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String language = input.has("language") ? input.get("language").asText() : "";
        String code = input.has("code") ? input.get("code").asText() : "";

        if (language.isBlank()) {
            return "Error: language is required (python, node, or bash).";
        }
        if (code.isBlank()) {
            return "Error: code is required.";
        }

        String interpreter = switch (language.toLowerCase()) {
            case "python" -> "python3";
            case "node" -> "node";
            case "bash" -> "bash";
            default -> null;
        };

        if (interpreter == null) {
            return String.format("Error: unsupported language '%s'. Supported: python, node, bash.", language);
        }

        try {
            // Write code to temp file and execute
            Path tempFile = Files.createTempFile("repl_", "." + language);
            Files.writeString(tempFile, code);

            try {
                ProcessBuilder pb = new ProcessBuilder(interpreter, tempFile.toString());
                pb.redirectErrorStream(true);
                Process process = pb.start();

                String output;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    output = reader.lines().limit(500).collect(java.util.stream.Collectors.joining("\n"));
                }

                boolean completed = process.waitFor(30, TimeUnit.SECONDS);
                int exitCode = completed ? process.exitValue() : -1;

                if (!completed) {
                    process.destroyForcibly();
                    return "REPL execution timed out after 30 seconds.";
                }

                return String.format("REPL (%s) exit code: %d\n%s", language, exitCode, output);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            return String.format("REPL execution failed: %s", e.getMessage());
        }
    }

    @Override
    public boolean isReadOnly() { return false; }
}
