package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * RemoteTriggerTool — trigger remote agent execution via BridgeTransport.
 * Sends a trigger message to a remote Claude Code instance through WebSocket.
 *
 * Task 51.4:
 * - BridgeTransport-based remote triggering
 * - Remote agent ID targeting
 * - Response timeout handling
 * - Error reporting
 */
public class RemoteTriggerTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;

    private final BridgeClient bridgeClient;

    public RemoteTriggerTool() {
        this(null);
    }

    public RemoteTriggerTool(BridgeClient bridgeClient) {
        this.bridgeClient = bridgeClient != null ? bridgeClient : new NoOpBridgeClient();
    }

    @Override
    public String name() {
        return "RemoteTrigger";
    }

    @Override
    public String description() {
        return "Trigger a remote agent via Bridge connection";
    }

    @Override
    public JsonNode inputSchema() {
        return SCHEMA;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String agentId = input.has("agent_id") ? input.get("agent_id").asText("") : "";
        if (agentId.isBlank()) {
            return "Error: agent_id is required";
        }

        String prompt = input.has("prompt") ? input.get("prompt").asText("") : "";
        if (prompt.isBlank()) {
            prompt = input.has("task") ? input.get("task").asText("") : "";
        }
        if (prompt.isBlank()) {
            return "Error: prompt or task is required";
        }

        int timeoutSeconds = input.has("timeout") ? input.get("timeout").asInt(DEFAULT_TIMEOUT_SECONDS) : DEFAULT_TIMEOUT_SECONDS;
        Optional<String> sessionId = extractSessionId(input);
        Optional<String> transportEndpoint = extractTransportEndpoint(input);

        TriggerRequest request = new TriggerRequest(
            agentId,
            prompt,
            sessionId,
            transportEndpoint,
            timeoutSeconds
        );

        try {
            TriggerResponse response = bridgeClient.sendTrigger(request);
            return formatResponse(response);
        } catch (TimeoutException e) {
            return "Error: remote trigger timed out after " + timeoutSeconds + " seconds";
        } catch (Exception e) {
            return "Error: remote trigger failed: " + e.getMessage();
        }
    }

    private Optional<String> extractSessionId(JsonNode input) {
        if (input.has("session_id") && !input.get("session_id").isNull()) {
            return Optional.of(input.get("session_id").asText());
        }
        return Optional.empty();
    }

    private Optional<String> extractTransportEndpoint(JsonNode input) {
        if (input.has("transport_endpoint") && !input.get("transport_endpoint").isNull()) {
            return Optional.of(input.get("transport_endpoint").asText());
        }
        return Optional.empty();
    }

    private String formatResponse(TriggerResponse response) {
        if (response.error().isPresent()) {
            return "Error: " + response.error().get();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Remote agent triggered successfully.\n");
        sb.append("Agent ID: ").append(response.agentId()).append("\n");
        if (response.sessionId().isPresent()) {
            sb.append("Session ID: ").append(response.sessionId().get()).append("\n");
        }
        sb.append("Response: ").append(response.output());
        return sb.toString();
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        return PermissionDecision.ASK;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode agentIdProp = properties.putObject("agent_id");
        agentIdProp.put("type", "string");
        agentIdProp.put("description", "The remote agent ID to trigger");

        ObjectNode promptProp = properties.putObject("prompt");
        promptProp.put("type", "string");
        promptProp.put("description", "The prompt/task for the remote agent");

        ObjectNode taskProp = properties.putObject("task");
        taskProp.put("type", "string");
        taskProp.put("description", "Alias for prompt - the task description");

        ObjectNode sessionIdProp = properties.putObject("session_id");
        sessionIdProp.put("type", "string");
        sessionIdProp.put("description", "Optional session ID to resume");

        ObjectNode timeoutProp = properties.putObject("timeout");
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "Timeout in seconds (default: 300)");
        timeoutProp.put("default", 300);

        ObjectNode transportEndpointProp = properties.putObject("transport_endpoint");
        transportEndpointProp.put("type", "string");
        transportEndpointProp.put("description", "Bridge transport endpoint URL");

        ArrayNode required = schema.putArray("required");
        required.add("agent_id");
        required.add("prompt");

        return schema;
    }

    public record TriggerRequest(
        String agentId,
        String prompt,
        Optional<String> sessionId,
        Optional<String> transportEndpoint,
        int timeoutSeconds
    ) {}

    public record TriggerResponse(
        String agentId,
        String output,
        Optional<String> sessionId,
        Optional<String> error
    ) {
        public static TriggerResponse success(String agentId, String output, String sessionId) {
            return new TriggerResponse(agentId, output, Optional.of(sessionId), Optional.empty());
        }

        public static TriggerResponse error(String agentId, String error) {
            return new TriggerResponse(agentId, "", Optional.empty(), Optional.of(error));
        }
    }

    public interface BridgeClient {
        TriggerResponse sendTrigger(TriggerRequest request) throws Exception;
    }

    public static class NoOpBridgeClient implements BridgeClient {
        @Override
        public TriggerResponse sendTrigger(TriggerRequest request) {
            return TriggerResponse.error(
                request.agentId(),
                "Bridge client not configured. Set up BridgeTransport for remote triggering."
            );
        }
    }

    public static class WebSocketBridgeClient implements BridgeClient {
        private final String endpoint;
        private final String authToken;
        private final int connectionTimeoutMs;

        public WebSocketBridgeClient(String endpoint, String authToken, int connectionTimeoutMs) {
            this.endpoint = endpoint;
            this.authToken = authToken;
            this.connectionTimeoutMs = connectionTimeoutMs;
        }

        @Override
        public TriggerResponse sendTrigger(TriggerRequest request) throws Exception {
            return TriggerResponse.error(
                request.agentId(),
                "WebSocket bridge client not fully implemented. Use BridgeMain for full functionality."
            );
        }
    }
}