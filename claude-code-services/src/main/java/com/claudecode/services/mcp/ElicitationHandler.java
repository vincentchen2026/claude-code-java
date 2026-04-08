package com.claudecode.services.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class ElicitationHandler {

    private static final Logger log = LoggerFactory.getLogger(ElicitationHandler.class);

    private final Map<String, ElicitationRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, ElicitationResponse> responses = new ConcurrentHashMap<>();

    public String createElicitation(ElicitationRequest request) {
        String elicitationId = "elic_" + System.currentTimeMillis();

        ElicitationRequest stored = new ElicitationRequest(
            elicitationId,
            request.prompt(),
            request.type(),
            request.options(),
            Instant.now(),
            request.context()
        );

        pendingRequests.put(elicitationId, stored);
        log.info("Created elicitation request: {} (type: {})", elicitationId, request.type());

        return elicitationId;
    }

    public CompletableFuture<ElicitationResponse> waitForResponse(String elicitationId) {
        CompletableFuture<ElicitationResponse> future = new CompletableFuture<>();

        ElicitationResponse stored = responses.get(elicitationId);
        if (stored != null) {
            future.complete(stored);
        } else {
            future.completeExceptionally(new ElicitationTimeoutException("Elicitation timed out: " + elicitationId));
        }

        return future;
    }

    public void respond(String elicitationId, ElicitationResponse response) {
        ElicitationRequest request = pendingRequests.get(elicitationId);
        if (request == null) {
            log.warn("Elicitation request not found: {}", elicitationId);
            return;
        }

        responses.put(elicitationId, response);
        pendingRequests.remove(elicitationId);

        log.info("Responded to elicitation: {} (selected: {})", elicitationId, response.selectedOption());
    }

    public void timeout(String elicitationId) {
        ElicitationRequest request = pendingRequests.get(elicitationId);
        if (request != null) {
            pendingRequests.remove(elicitationId);
            responses.put(elicitationId, new ElicitationResponse(
                elicitationId,
                null,
                ResponseResult.TIMEOUT,
                Instant.now()
            ));
            log.info("Elicitation timed out: {}", elicitationId);
        }
    }

    public ElicitationRequest getRequest(String elicitationId) {
        return pendingRequests.get(elicitationId);
    }

    public int getPendingCount() {
        return pendingRequests.size();
    }

    public record ElicitationRequest(
        String elicitationId,
        String prompt,
        ElicitationType type,
        String[] options,
        Instant createdAt,
        Map<String, String> context
    ) {}

    public record ElicitationResponse(
        String elicitationId,
        String selectedOption,
        ResponseResult result,
        Instant respondedAt
    ) {}

    public enum ElicitationType {
        SELECT_OPTION,
        CONFIRMATION,
        TEXT_INPUT,
        RESOURCE_SELECTION
    }

    public enum ResponseResult {
        SELECTED,
        CANCELLED,
        TIMEOUT
    }

    public static class ElicitationTimeoutException extends RuntimeException {
        public ElicitationTimeoutException(String message) {
            super(message);
        }
    }
}