package com.claudecode.bridge;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

public class RemotePermissionBridge {

    private final Map<String, PendingPermission> pendingPermissions;
    private final Map<String, PermissionResponse> completedPermissions;
    private final BiConsumer<String, PermissionResponse> responseCallback;
    private final ScheduledExecutorService timeoutExecutor;

    public RemotePermissionBridge(BiConsumer<String, PermissionResponse> responseCallback) {
        this.pendingPermissions = new ConcurrentHashMap<>();
        this.completedPermissions = new ConcurrentHashMap<>();
        this.responseCallback = responseCallback;
        this.timeoutExecutor = Executors.newScheduledThreadPool(1);
    }

    public String requestPermission(String sessionId, PermissionRequest request) {
        String requestId = generateRequestId();
        
        PendingPermission pending = new PendingPermission(
            requestId,
            sessionId,
            request,
            Instant.now(),
            CompletableFuture.completedFuture(null)
        );
        
        pendingPermissions.put(requestId, pending);
        
        timeoutExecutor.schedule(() -> {
            if (pendingPermissions.containsKey(requestId)) {
                timeoutPermission(requestId);
            }
        }, 60, TimeUnit.SECONDS);
        
        return requestId;
    }

    public void respondToPermission(String requestId, boolean granted, String reason) {
        PendingPermission pending = pendingPermissions.remove(requestId);
        if (pending == null) {
            return;
        }
        
        PermissionResponse response = new PermissionResponse(
            requestId,
            pending.sessionId(),
            granted ? ResponseStatus.GRANTED : ResponseStatus.DENIED,
            reason,
            Instant.now()
        );
        
        completedPermissions.put(requestId, response);
        
        if (responseCallback != null) {
            responseCallback.accept(requestId, response);
        }
    }

    public PermissionResponse getResponse(String requestId) {
        return completedPermissions.get(requestId);
    }

    public PendingPermission getPendingPermission(String requestId) {
        return pendingPermissions.get(requestId);
    }

    public boolean hasPendingPermission(String requestId) {
        return pendingPermissions.containsKey(requestId);
    }

    public int getPendingCount() {
        return pendingPermissions.size();
    }

    public void cancelPendingPermission(String requestId) {
        PendingPermission pending = pendingPermissions.remove(requestId);
        if (pending != null) {
            PermissionResponse response = new PermissionResponse(
                requestId,
                pending.sessionId(),
                ResponseStatus.CANCELLED,
                "Cancelled by requester",
                Instant.now()
            );
            completedPermissions.put(requestId, response);
        }
    }

    private void timeoutPermission(String requestId) {
        PendingPermission pending = pendingPermissions.remove(requestId);
        if (pending != null) {
            PermissionResponse response = new PermissionResponse(
                requestId,
                pending.sessionId(),
                ResponseStatus.TIMEOUT,
                "Permission request timed out",
                Instant.now()
            );
            completedPermissions.put(requestId, response);
            
            if (responseCallback != null) {
                responseCallback.accept(requestId, response);
            }
        }
    }

    public void shutdown() {
        timeoutExecutor.shutdown();
    }

    private String generateRequestId() {
        return "perm-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(10000);
    }

    public enum ResponseStatus {
        GRANTED, DENIED, TIMEOUT, CANCELLED
    }

    public record PendingPermission(
        String requestId,
        String sessionId,
        PermissionRequest request,
        Instant requestedAt,
        CompletableFuture<Void> future
    ) {}

    public record PermissionResponse(
        String requestId,
        String sessionId,
        ResponseStatus status,
        String reason,
        Instant respondedAt
    ) {}

    public record PermissionRequest(
        String tool,
        String action,
        String details
    ) {}
}