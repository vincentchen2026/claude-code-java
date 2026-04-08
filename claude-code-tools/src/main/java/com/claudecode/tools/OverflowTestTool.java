package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * OverflowTestTool — testing tool placeholder.
 * Task 55.3
 */
public class OverflowTestTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "OverflowTest"; }

    @Override
    public String description() {
        return "Testing tool for overflow and edge case scenarios. Used for debugging and validation.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode testSchema = mapper().createObjectNode();
        testSchema.put("type", "string");
        testSchema.put("description", "Name of the test to run");
        props.set("test_name", testSchema);

        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String testName = input.has("test_name") ? input.get("test_name").asText("default") : "default";
        return String.format("OverflowTest '%s' executed. [Test placeholder — no actual test logic implemented]", testName);
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public boolean isConcurrencySafe() { return true; }
}
