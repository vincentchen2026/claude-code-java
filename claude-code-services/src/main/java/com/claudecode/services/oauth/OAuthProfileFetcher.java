package com.claudecode.services.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class OAuthProfileFetcher {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String userInfoEndpoint;

    public OAuthProfileFetcher(String userInfoEndpoint) {
        this(userInfoEndpoint, HttpClient.newHttpClient());
    }

    public OAuthProfileFetcher(String userInfoEndpoint, HttpClient httpClient) {
        this.userInfoEndpoint = userInfoEndpoint;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<OAuthProfile> fetchProfile(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(userInfoEndpoint))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    return parseProfile(response.body());
                } else {
                    throw new OAuthProfileException("Failed to fetch profile: " + response.statusCode());
                }
            });
    }

    private OAuthProfile parseProfile(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return new OAuthProfile(
                getTextOrNull(node, "sub"),
                getTextOrNull(node, "name"),
                getTextOrNull(node, "email"),
                getTextOrNull(node, "picture"),
                getTextOrNull(node, "locale")
            );
        } catch (Exception e) {
            throw new OAuthProfileException("Failed to parse profile JSON", e);
        }
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    public record OAuthProfile(
        String subject,
        String name,
        String email,
        String picture,
        String locale
    ) {}

    public static class OAuthProfileException extends RuntimeException {
        public OAuthProfileException(String message) {
            super(message);
        }

        public OAuthProfileException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}