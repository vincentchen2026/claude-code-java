package com.claudecode.services.hooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Optional;

/**
 * Input data passed to hook commands as context.
 */
public record HookInput(
    HookEvent event,
    Optional<String> toolName,
    Optional<JsonNode> toolInput,
    Optional<JsonNode> toolOutput,
    Optional<String> toolUseId,
    Map<String, Object> extra
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static HookInput forPreToolUse(String toolName, JsonNode input, String toolUseId) {
        return new HookInput(HookEvent.PRE_TOOL_USE, Optional.of(toolName),
            Optional.ofNullable(input), Optional.empty(), Optional.of(toolUseId), Map.of());
    }

    public static HookInput forPostToolUse(String toolName, JsonNode input, JsonNode output, String toolUseId) {
        return new HookInput(HookEvent.POST_TOOL_USE, Optional.of(toolName),
            Optional.ofNullable(input), Optional.ofNullable(output), Optional.of(toolUseId), Map.of());
    }

    public static HookInput forStop(String stopReason) {
        return new HookInput(HookEvent.STOP, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(),
            Map.of("stopReason", stopReason));
    }

    public static HookInput forSessionStart(String trigger) {
        return new HookInput(HookEvent.SESSION_START, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(),
            Map.of("trigger", trigger));
    }

    public static HookInput forPermissionRequest(String toolName, JsonNode input, String toolUseId) {
        return new HookInput(HookEvent.PERMISSION_REQUEST, Optional.of(toolName),
            Optional.ofNullable(input), Optional.empty(), Optional.of(toolUseId), Map.of());
    }

    public static HookInput forEvent(HookEvent event) {
        return new HookInput(event, Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Map.of());
    }

    /**
     * Serializes this input to a JSON string for passing to hook commands.
     */
    public String toJson() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("event", event.configKey());
        toolName.ifPresent(n -> node.put("toolName", n));
        toolInput.ifPresent(i -> node.set("toolInput", i));
        toolOutput.ifPresent(o -> node.set("toolOutput", o));
        toolUseId.ifPresent(id -> node.put("toolUseId", id));
        if (!extra.isEmpty()) {
            ObjectNode extraNode = node.putObject("extra");
            extra.forEach((k, v) -> extraNode.put(k, String.valueOf(v)));
        }
        return node.toString();
    }
}
