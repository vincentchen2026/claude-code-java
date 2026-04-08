package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

/**
 * ToolSearchTool — search/discover available tools by name or description.
 * Task 52.4
 */
public class ToolSearchTool extends Tool<JsonNode, String> {

    private final ToolRegistry toolRegistry;

    public ToolSearchTool(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public String name() { return "ToolSearch"; }

    @Override
    public String description() {
        return "Search for available tools by name or description. " +
               "Use this to discover what tools are available and their capabilities.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode querySchema = mapper().createObjectNode();
        querySchema.put("type", "string");
        querySchema.put("description", "Search query to match against tool names and descriptions");
        props.set("query", querySchema);

        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String query = input.has("query") ? input.get("query").asText("").toLowerCase() : "";

        List<Tool<?, ?>> allTools = new ArrayList<>(toolRegistry.getAll());
        List<Tool<?, ?>> matching = allTools.stream()
            .filter(t -> t.name().toLowerCase().contains(query)
                      || t.description().toLowerCase().contains(query))
            .collect(Collectors.toList());

        if (matching.isEmpty()) {
            return String.format("No tools found matching query: '%s'", query);
        }

        String results = matching.stream()
            .map(t -> String.format("  - %s: %s", t.name(), t.description()))
            .collect(Collectors.joining("\n"));

        return String.format("Found %d tool(s) matching '%s':\n%s",
            matching.size(), query.isEmpty() ? "(all)" : query, results);
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public boolean isConcurrencySafe() { return true; }
}
