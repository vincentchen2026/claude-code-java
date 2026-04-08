package com.claudecode.services.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class AuthCodeListener {

    private static final Logger log = LoggerFactory.getLogger(AuthCodeListener.class);
    private static final ObjectMapper OM = new ObjectMapper();

    private final HttpServer server;
    private final int port;
    private final Map<String, AuthCallback> pendingCallbacks = new ConcurrentHashMap<>();
    private final Map<String, String> receivedCodes = new ConcurrentHashMap<>();

    public AuthCodeListener(int port) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/callback", this::handleCallback);
        this.server.createContext("/health", this::handleHealth);
    }

    public void start() {
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        log.info("OAuth auth code listener started on port {}", port);
    }

    public void stop() {
        server.stop(0);
        log.info("OAuth auth code listener stopped");
    }

    public CompletableFuture<String> waitForCode(String state) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingCallbacks.put(state, new AuthCallback(state, future));
        
        future.orTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
            .whenComplete((result, ex) -> pendingCallbacks.remove(state));

        return future;
    }

    private void handleCallback(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        
        log.debug("OAuth callback received: {}?{}", path, query);

        Map<String, String> params = parseQueryParams(query);
        String code = params.get("code");
        String state = params.get("state");
        String error = params.get("error");

        String response;
        int statusCode;

        if (error != null) {
            response = buildErrorResponse(error, params.get("error_description"));
            statusCode = 400;
            log.warn("OAuth error: {} - {}", error, params.get("error_description"));
        } else if (code != null && state != null) {
            receivedCodes.put(state, code);
            
            AuthCallback callback = pendingCallbacks.get(state);
            if (callback != null) {
                callback.future().complete(code);
            }
            
            response = buildSuccessResponse();
            statusCode = 200;
            log.info("OAuth code received for state: {}", state);
        } else {
            response = buildErrorResponse("invalid_request", "Missing code or state parameter");
            statusCode = 400;
        }

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleHealth(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String response = "{\"status\":\"ok\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new ConcurrentHashMap<>();
        if (query != null && !query.isEmpty()) {
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    params.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                }
            }
        }
        return params;
    }

    private String buildSuccessResponse() {
        return """
            <!DOCTYPE html>
            <html>
            <head><title>Authentication Successful</title></head>
            <body style="font-family: system-ui; text-align: center; padding: 50px; background: #1a1a2e; color: #eee;">
                <h1 style="color: #4ade80;">✓ Authentication Successful</h1>
                <p>You may now close this window and return to the application.</p>
            </body>
            </html>
            """;
    }

    private String buildErrorResponse(String error, String description) {
        String desc = description != null ? description : "";
        return """
            <!DOCTYPE html>
            <html>
            <head><title>Authentication Failed</title></head>
            <body style="font-family: system-ui; text-align: center; padding: 50px; background: #1a1a2e; color: #eee;">
                <h1 style="color: #ef4444;">✕ Authentication Failed</h1>
                <p>Error: %s</p>
                <p>%s</p>
            </body>
            </html>
            """.formatted(error, desc);
    }

    public int getPort() {
        return port;
    }

    public Map<String, String> getReceivedCodes() {
        return Map.copyOf(receivedCodes);
    }

    public record AuthCallback(
        String state,
        CompletableFuture<String> future
    ) {}
}