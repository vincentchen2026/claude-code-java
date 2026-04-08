package com.claudecode.mcp;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles OAuth authentication flow for MCP servers that require auth.
 * Opens browser with auth URL, starts local HTTP server to receive callback,
 * exchanges code for token.
 */
public class McpAuthTool extends Tool<JsonNode, String> {

    private static final Logger LOG = LoggerFactory.getLogger(McpAuthTool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int CALLBACK_PORT = 19876;
    private static final Duration TOKEN_TIMEOUT = Duration.ofMinutes(2);

    @Override
    public String name() {
        return "mcp__auth";
    }

    @Override
    public String description() {
        return "Handles OAuth authentication for MCP servers";
    }

    @Override
    public JsonNode inputSchema() {
        return buildSchema();
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String serverId = input.has("serverId")
            ? input.get("serverId").asText() : "";
        String authUrl = input.has("authUrl")
            ? input.get("authUrl").asText() : "";
        String tokenUrl = input.has("tokenUrl")
            ? input.get("tokenUrl").asText() : "";
        String clientId = input.has("clientId")
            ? input.get("clientId").asText() : "";

        if (serverId.isBlank()) {
            return "Error: serverId is required";
        }
        if (authUrl.isBlank()) {
            return "Error: authUrl is required for OAuth flow";
        }

        String redirectUri = "http://localhost:" + CALLBACK_PORT + "/callback";

        // Build the authorization URL
        String fullAuthUrl = authUrl
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=mcp";

        LOG.info("Starting OAuth flow for MCP server '{}'", serverId);

        try {
            // Open browser
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(fullAuthUrl));
            } else {
                // Fallback: try xdg-open on Linux
                new ProcessBuilder("xdg-open", fullAuthUrl).start();
            }

            LOG.info("Opened browser for OAuth. Waiting for callback...");

            // Start local callback server
            CompletableFuture<String> codeFuture = new CompletableFuture<>();
            var server = com.sun.net.httpserver.HttpServer.create(
                    new InetSocketAddress(CALLBACK_PORT), 0);

            server.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String code = null;
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("code=")) {
                            code = param.substring(5);
                        }
                    }
                }

                String responseBody = code != null
                        ? "Authorization successful! You can close this tab."
                        : "Authorization failed. No code received.";
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }

                if (code != null) {
                    codeFuture.complete(code);
                } else {
                    codeFuture.completeExceptionally(
                        new McpException("No authorization code received"));
                }
            });

            server.start();

            try {
                String code = codeFuture.get(
                    TOKEN_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

                // Exchange code for token if tokenUrl is provided
                if (!tokenUrl.isBlank()) {
                    return exchangeCodeForToken(
                        tokenUrl, code, clientId, redirectUri, serverId);
                }

                return "OAuth code received for server '"
                    + serverId + "': " + code;
            } finally {
                server.stop(0);
            }
        } catch (Exception e) {
            LOG.error("OAuth flow failed for server '{}'", serverId, e);
            return "Error: OAuth flow failed: " + e.getMessage();
        }
    }

    private String exchangeCodeForToken(
            String tokenUrl, String code, String clientId,
            String redirectUri, String serverId) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String body = "grant_type=authorization_code"
                    + "&code=" + code
                    + "&client_id=" + clientId
                    + "&redirect_uri=" + redirectUri;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type",
                        "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return "OAuth token obtained for server '"
                    + serverId + "': " + response.body();
            } else {
                return "Error: token exchange failed (HTTP "
                    + response.statusCode() + "): " + response.body();
            }
        } catch (Exception e) {
            return "Error: token exchange failed: " + e.getMessage();
        }
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");

        ObjectNode serverIdProp = MAPPER.createObjectNode();
        serverIdProp.put("type", "string");
        serverIdProp.put("description",
            "The MCP server ID to authenticate with");
        props.set("serverId", serverIdProp);

        ObjectNode authUrlProp = MAPPER.createObjectNode();
        authUrlProp.put("type", "string");
        authUrlProp.put("description", "The OAuth authorization URL");
        props.set("authUrl", authUrlProp);

        ObjectNode tokenUrlProp = MAPPER.createObjectNode();
        tokenUrlProp.put("type", "string");
        tokenUrlProp.put("description", "The OAuth token exchange URL");
        props.set("tokenUrl", tokenUrlProp);

        ObjectNode clientIdProp = MAPPER.createObjectNode();
        clientIdProp.put("type", "string");
        clientIdProp.put("description", "The OAuth client ID");
        props.set("clientId", clientIdProp);

        ArrayNode required = schema.putArray("required");
        required.add("serverId");
        return schema;
    }
}
