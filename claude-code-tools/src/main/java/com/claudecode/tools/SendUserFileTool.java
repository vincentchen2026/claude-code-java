package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SendUserFileTool — sends a file to the user.
 * Task 54.1
 */
public class SendUserFileTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "SendUserFile"; }

    @Override
    public String description() {
        return "Send a file to the user. The file will be made available for the user to view or download.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode pathSchema = mapper().createObjectNode();
        pathSchema.put("type", "string");
        pathSchema.put("description", "Absolute path to the file to send to the user");
        props.set("file_path", pathSchema);

        schema.set("required", mapper().createArrayNode().add("file_path"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String filePath = input.has("file_path") ? input.get("file_path").asText() : null;
        if (filePath == null || filePath.isBlank()) {
            return "Error: file_path is required.";
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return String.format("Error: file not found: %s", filePath);
        }
        if (!Files.isRegularFile(path)) {
            return String.format("Error: not a regular file: %s", filePath);
        }

        try {
            long size = Files.size(path);
            return String.format(
                "File sent to user: %s\n" +
                "  Size: %d bytes\n" +
                "  The file is now available to the user.",
                filePath, size);
        } catch (Exception e) {
            return String.format("Error sending file: %s", e.getMessage());
        }
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public boolean isConcurrencySafe() { return true; }
}
