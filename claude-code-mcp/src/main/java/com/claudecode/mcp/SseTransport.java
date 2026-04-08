package com.claudecode.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP transport over HTTP Server-Sent Events (SSE).
 * Sends requests via HTTP POST and receives events via SSE stream.
 * Uses java.net.http.HttpClient with SSE parsing.
 */
public class SseTransport implements McpTransport {

    private static final Logger LOG = LoggerFactory.getLogger(SseTransport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String serverUrl;
    private final AtomicInteger requestId = new AtomicInteger(0);
    private volatile boolean connected = false;
    private HttpClient httpClient;
    private String postEndpoint;
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonNode>> pending
        = new ConcurrentHashMap<>();
    private Thread sseThread;

    public SseTransport(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Connects to the SSE endpoint. Starts listening for events.
     */
    public void connect() {
        LOG.info("SSE transport connecting to {}", serverUrl);
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Start SSE listener in background
        sseThread = Thread.ofVirtual().start(this::listenSse);
        connected = true;
    }

    private void listenSse() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .header("Accept", "text/event-stream")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.error("SSE connection failed: HTTP {}", response.statusCode());
                connected = false;
                return;
            }

            // Parse SSE events from the response body
            parseSseEvents(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("SSE listener interrupted");
        } catch (Exception e) {
            LOG.error("SSE listener error: {}", e.getMessage());
            connected = false;
        }
    }

    private void parseSseEvents(String body) {
        String eventType = null;
        StringBuilder data = new StringBuilder();

        for (String line : body.split("\n")) {
            if (line.startsWith("event:")) {
                eventType = line.substring(6).trim();
            } else if (line.startsWith("data:")) {
                data.append(line.substring(5).trim());
            } else if (line.isEmpty() && data.length() > 0) {
                handleSseEvent(eventType, data.toString());
                eventType = null;
                data.setLength(0);
            }
        }
        // Handle last event if no trailing newline
        if (data.length() > 0) {
            handleSseEvent(eventType, data.toString());
        }
    }

    private void handleSseEvent(String eventType, String data) {
        try {
            if ("endpoint".equals(eventType)) {
                // Server tells us where to POST requests
                postEndpoint = data;
                LOG.debug("SSE post endpoint: {}", postEndpoint);
            } else if ("message".equals(eventType) || eventType == null) {
                JsonNode node = MAPPER.readTree(data);
                if (node.has("id")) {
                    int id = node.get("id").asInt();
                    CompletableFuture<JsonNode> future = pending.remove(id);
                    if (future != null) {
                        if (node.has("error")) {
                            future.completeExceptionally(
                                new McpException(node.get("error").toString()));
                        } else {
                            future.complete(
                                node.has("result") ? node.get("result") : node);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse SSE event: {}", e.getMessage());
        }
    }

    @Override
    public JsonNode sendRequest(String method, JsonNode params) {
        if (!connected) {
            throw new McpException(
                "SSE transport not connected to " + serverUrl);
        }

        int id = requestId.incrementAndGet();

        ObjectNode request = MAPPER.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        if (params != null) {
            request.set("params", params);
        }

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pending.put(id, future);

        try {
            String endpoint = postEndpoint != null ? postEndpoint : serverUrl;
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                        MAPPER.writeValueAsString(request)))
                    .timeout(REQUEST_TIMEOUT)
                    .build();

            HttpResponse<String> response = httpClient.send(httpReq,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 202) {
                pending.remove(id);
                throw new McpException(
                    "HTTP POST failed: " + response.statusCode());
            }

            // If the response body contains the result directly
            String body = response.body();
            if (body != null && !body.isBlank()) {
                JsonNode respNode = MAPPER.readTree(body);
                if (respNode.has("result")) {
                    pending.remove(id);
                    return respNode.get("result");
                }
            }

            // Otherwise wait for SSE event
            return future.get(REQUEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (McpException e) {
            pending.remove(id);
            throw e;
        } catch (Exception e) {
            pending.remove(id);
            throw new McpException(
                "SSE request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() throws Exception {
        connected = false;
        pending.values().forEach(f ->
            f.completeExceptionally(
                new McpException("Transport closed")));
        pending.clear();
        if (sseThread != null) {
            sseThread.interrupt();
        }
        LOG.debug("SSE transport disconnected from {}", serverUrl);
    }
}
