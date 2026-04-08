package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SnipTool — history snipping/cropping.
 * Marks messages before a given point as snipped.
 * Task 49.4
 */
public class SnipTool extends Tool<JsonNode, String> {

    private static final Map<String, SnipRecord> SNIP_HISTORY = new ConcurrentHashMap<>();
    private static int SNIP_COUNTER = 0;

    @Override
    public String name() { return "Snip"; }

    @Override
    public String description() {
        return "Snip (crop) conversation history before a given message. " +
               "Use this to reduce context window usage by removing old messages.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode beforeSchema = mapper().createObjectNode();
        beforeSchema.put("type", "string");
        beforeSchema.put("description", "UUID of the message to snip before. " +
            "All messages before this one will be removed from context.");
        props.set("before_message_uuid", beforeSchema);

        ObjectNode reasonSchema = mapper().createObjectNode();
        reasonSchema.put("type", "string");
        reasonSchema.put("description", "Reason for snipping (optional)");
        props.set("reason", reasonSchema);

        schema.set("required", mapper().createArrayNode().add("before_message_uuid"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String beforeUuid = input.has("before_message_uuid")
            ? input.get("before_message_uuid").asText() : null;
        String reason = input.has("reason") ? input.get("reason").asText() : "Manual snip";

        if (beforeUuid == null || beforeUuid.isBlank()) {
            return "Error: before_message_uuid is required.";
        }

        String snipId = "snip_" + (++SNIP_COUNTER);
        long timestamp = System.currentTimeMillis();

        SNIP_HISTORY.put(snipId, new SnipRecord(snipId, beforeUuid, reason, timestamp, context.sessionId()));

        StringBuilder sb = new StringBuilder();
        sb.append("History Snip Applied\n");
        sb.append("====================\n\n");
        sb.append("Snip ID: ").append(snipId).append("\n");
        sb.append("Before message UUID: ").append(beforeUuid).append("\n");
        sb.append("Reason: ").append(reason).append("\n");
        sb.append("Timestamp: ").append(new Date(timestamp)).append("\n");
        sb.append("Session: ").append(context.sessionId()).append("\n\n");
        sb.append("All messages before the specified UUID have been marked as snipped.\n");
        sb.append("These messages will not be included in future context windows.\n\n");
        sb.append("Use /snip list to see snip history.\n");

        return sb.toString();
    }

    /**
     * List all snip records.
     */
    public static List<SnipRecord> listSnips() {
        return List.copyOf(SNIP_HISTORY.values());
    }

    /**
     * Get a snip by ID.
     */
    public static Optional<SnipRecord> getSnip(String snipId) {
        return Optional.ofNullable(SNIP_HISTORY.get(snipId));
    }

    /**
     * Clear snip history.
     */
    public static void clearHistory() {
        SNIP_HISTORY.clear();
    }

    public record SnipRecord(
        String snipId,
        String beforeMessageUuid,
        String reason,
        long timestamp,
        String sessionId
    ) {}
}
