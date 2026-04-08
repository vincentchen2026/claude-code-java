package com.claudecode.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BridgePermissionCallbacks {

    private final Map<String, PermissionCallback> callbacks;
    private final BiConsumer<String, PermissionRequest> defaultCallback;

    public BridgePermissionCallbacks() {
        this(new java.util.function.BiConsumer<>() {
            @Override
            public void accept(String sessionId, PermissionRequest request) {
                throw new UnsupportedOperationException("No default callback configured");
            }
        });
    }

    public BridgePermissionCallbacks(BiConsumer<String, PermissionRequest> defaultCallback) {
        this.callbacks = new ConcurrentHashMap<>();
        this.defaultCallback = defaultCallback;
    }

    public void registerCallback(String sessionId, PermissionCallback callback) {
        callbacks.put(sessionId, callback);
    }

    public void unregisterCallback(String sessionId) {
        callbacks.remove(sessionId);
    }

    public void requestPermission(String sessionId, PermissionRequest request) {
        PermissionCallback callback = callbacks.get(sessionId);
        if (callback != null) {
            callback.onPermissionRequest(request);
        } else {
            defaultCallback.accept(sessionId, request);
        }
    }

    public void grantPermission(String sessionId, String requestId) {
        PermissionCallback callback = callbacks.get(sessionId);
        if (callback != null) {
            callback.onPermissionGranted(requestId);
        }
    }

    public void denyPermission(String sessionId, String requestId) {
        PermissionCallback callback = callbacks.get(sessionId);
        if (callback != null) {
            callback.onPermissionDenied(requestId);
        }
    }

    public void timeoutPermission(String sessionId, String requestId) {
        PermissionCallback callback = callbacks.get(sessionId);
        if (callback != null) {
            callback.onPermissionTimeout(requestId);
        }
    }

    public boolean hasCallback(String sessionId) {
        return callbacks.containsKey(sessionId);
    }

    public int getCallbackCount() {
        return callbacks.size();
    }

    public interface PermissionCallback {
        void onPermissionRequest(PermissionRequest request);
        void onPermissionGranted(String requestId);
        void onPermissionDenied(String requestId);
        void onPermissionTimeout(String requestId);
    }

    public record PermissionRequest(
        String requestId,
        String sessionId,
        PermissionType type,
        String tool,
        String details,
        long timestamp
    ) {}

    public enum PermissionType {
        BASH,
        FILE_READ,
        FILE_WRITE,
        FILE_EDIT,
        WEB_FETCH,
        WEB_SEARCH,
        TOOL_USE,
        AGENT_SPAWN,
        MCP_SERVER
    }
}