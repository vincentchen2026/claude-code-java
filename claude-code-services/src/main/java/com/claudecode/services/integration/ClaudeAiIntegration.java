package com.claudecode.services.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClaudeAiIntegration {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiIntegration.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final Map<String, String> sessionCookies = new ConcurrentHashMap<>();

    public ClaudeAiIntegration(String apiKey) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.baseUrl = "https://claude.ai";
        this.apiKey = apiKey;
    }

    public ClaudeAiIntegration(String baseUrl, String apiKey, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public IntegrationResult sendMessage(String conversationId, String message) {
        try {
            String jsonBody = String.format("{\"message\":\"%s\"}", message.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/conversations/" + conversationId + "/messages"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(60))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new IntegrationResult(true, response.body(), null);
            } else {
                return new IntegrationResult(false, null, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send message to Claude.ai", e);
            return new IntegrationResult(false, null, e.getMessage());
        }
    }

    public IntegrationResult getConversation(String conversationId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/conversations/" + conversationId))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new IntegrationResult(true, response.body(), null);
            } else {
                return new IntegrationResult(false, null, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to get conversation from Claude.ai", e);
            return new IntegrationResult(false, null, e.getMessage());
        }
    }

    public IntegrationResult listConversations() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/conversations"))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return new IntegrationResult(true, response.body(), null);
            } else {
                return new IntegrationResult(false, null, "HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to list conversations from Claude.ai", e);
            return new IntegrationResult(false, null, e.getMessage());
        }
    }

    public void setSessionCookie(String name, String value) {
        sessionCookies.put(name, value);
    }

    public String getSessionCookie(String name) {
        return sessionCookies.get(name);
    }

    public void clearSessionCookies() {
        sessionCookies.clear();
    }

    public record IntegrationResult(
        boolean success,
        String responseBody,
        String errorMessage
    ) {}
}