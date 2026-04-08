package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ConfigTool — runtime configuration management.
 * Task 49.2
 */
public class ConfigTool extends Tool<JsonNode, String> {

    private static final Map<String, String> CONFIG_STORE = new ConcurrentHashMap<>();

    @Override
    public String name() { return "Config"; }

    @Override
    public String description() {
        return "Manage runtime configuration. Supports get, set, list, and delete operations for configuration keys.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode actionSchema = mapper().createObjectNode();
        actionSchema.put("type", "string");
        actionSchema.set("enum", mapper().createArrayNode()
            .add("get").add("set").add("list").add("delete"));
        actionSchema.put("description", "The action to perform");
        props.set("action", actionSchema);

        ObjectNode keySchema = mapper().createObjectNode();
        keySchema.put("type", "string");
        keySchema.put("description", "Configuration key (required for get/set/delete)");
        props.set("key", keySchema);

        ObjectNode valueSchema = mapper().createObjectNode();
        valueSchema.put("type", "string");
        valueSchema.put("description", "Configuration value (required for set)");
        props.set("value", valueSchema);

        schema.set("required", mapper().createArrayNode().add("action"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String action = input.has("action") ? input.get("action").asText() : "";
        String key = input.has("key") ? input.get("key").asText() : null;
        String value = input.has("value") ? input.get("value").asText() : null;

        return switch (action) {
            case "get" -> {
                if (key == null) yield "Error: key is required for get action.";
                String v = CONFIG_STORE.get(key);
                yield v != null ? String.format("%s = %s", key, v) : String.format("Key '%s' not found.", key);
            }
            case "set" -> {
                if (key == null) yield "Error: key is required for set action.";
                if (value == null) yield "Error: value is required for set action.";
                CONFIG_STORE.put(key, value);
                yield String.format("Config set: %s = %s", key, value);
            }
            case "list" -> {
                if (CONFIG_STORE.isEmpty()) yield "No configuration entries.";
                String entries = CONFIG_STORE.entrySet().stream()
                    .map(e -> String.format("  %s = %s", e.getKey(), e.getValue()))
                    .collect(Collectors.joining("\n"));
                yield "Configuration:\n" + entries;
            }
            case "delete" -> {
                if (key == null) yield "Error: key is required for delete action.";
                String removed = CONFIG_STORE.remove(key);
                yield removed != null ? String.format("Deleted config key: %s", key)
                    : String.format("Key '%s' not found.", key);
            }
            default -> "Error: action must be one of: get, set, list, delete.";
        };
    }

    @Override
    public boolean isReadOnly() { return false; }
}
