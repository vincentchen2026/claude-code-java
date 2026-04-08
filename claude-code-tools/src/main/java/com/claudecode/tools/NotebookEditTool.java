package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * NotebookEditTool — edit Jupyter notebook cells.
 * Input: {notebook_path, cell_index, new_source}. Reads the .ipynb JSON,
 * updates the specified cell's source, and writes back.
 */
public class NotebookEditTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();

    @Override public String name() { return "NotebookEdit"; }

    @Override public String description() { return "Edit Jupyter notebook cells"; }

    @Override public JsonNode inputSchema() { return SCHEMA; }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String notebookPath = input.has("notebook_path") ? input.get("notebook_path").asText("") : "";
        int cellIndex = input.has("cell_index") ? input.get("cell_index").asInt(-1) : -1;
        String newSource = input.has("new_source") ? input.get("new_source").asText("") : "";

        if (notebookPath.isBlank()) {
            return "Error: notebook_path is required";
        }
        if (cellIndex < 0) {
            return "Error: cell_index must be a non-negative integer";
        }

        Path path = Path.of(context.workingDirectory()).resolve(notebookPath);

        if (!Files.exists(path)) {
            return "Error: notebook not found: " + notebookPath;
        }

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            ObjectNode notebook = (ObjectNode) mapper().readTree(content);

            if (!notebook.has("cells") || !notebook.get("cells").isArray()) {
                return "Error: invalid notebook format - missing 'cells' array";
            }

            ArrayNode cells = (ArrayNode) notebook.get("cells");
            if (cellIndex >= cells.size()) {
                return "Error: cell_index " + cellIndex + " out of range (notebook has " + cells.size() + " cells)";
            }

            ObjectNode cell = (ObjectNode) cells.get(cellIndex);

            // Convert new_source to array of lines (Jupyter format)
            ArrayNode sourceArray = mapper().createArrayNode();
            String[] lines = newSource.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (i < lines.length - 1) {
                    sourceArray.add(lines[i] + "\n");
                } else {
                    sourceArray.add(lines[i]);
                }
            }
            cell.set("source", sourceArray);

            String updatedContent = mapper().writerWithDefaultPrettyPrinter().writeValueAsString(notebook);
            Files.writeString(path, updatedContent, StandardCharsets.UTF_8);

            return "Updated cell " + cellIndex + " in " + notebookPath;
        } catch (IOException e) {
            return "Error: failed to edit notebook: " + e.getMessage();
        }
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");

        ObjectNode pathProp = properties.putObject("notebook_path");
        pathProp.put("type", "string");
        pathProp.put("description", "Path to the Jupyter notebook (.ipynb) file");

        ObjectNode cellProp = properties.putObject("cell_index");
        cellProp.put("type", "integer");
        cellProp.put("description", "Index of the cell to edit (0-based)");

        ObjectNode sourceProp = properties.putObject("new_source");
        sourceProp.put("type", "string");
        sourceProp.put("description", "New source content for the cell");

        ArrayNode required = schema.putArray("required");
        required.add("notebook_path");
        required.add("cell_index");
        required.add("new_source");
        return schema;
    }
}
