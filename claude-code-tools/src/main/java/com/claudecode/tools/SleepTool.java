package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * SleepTool — delay/sleep for a specified number of seconds.
 * Task 53.5
 */
public class SleepTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "Sleep"; }

    @Override
    public String description() {
        return "Pause execution for a specified number of seconds. " +
               "Useful for waiting between operations or rate limiting.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode secondsSchema = mapper().createObjectNode();
        secondsSchema.put("type", "integer");
        secondsSchema.put("minimum", 0);
        secondsSchema.put("maximum", 300);
        secondsSchema.put("description", "Number of seconds to sleep (0-300, default: 1)");
        props.set("seconds", secondsSchema);

        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        int seconds = input.has("seconds") ? input.get("seconds").asInt(1) : 1;
        seconds = Math.max(0, Math.min(300, seconds));

        try {
            Thread.sleep(seconds * 1000L);
            return String.format("Slept for %d second(s).", seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return String.format("Sleep interrupted after partial duration.");
        }
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public boolean isConcurrencySafe() { return true; }
}
