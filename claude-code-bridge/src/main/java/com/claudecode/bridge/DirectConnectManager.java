package com.claudecode.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DirectConnectManager {

    private static final Logger log = LoggerFactory.getLogger(DirectConnectManager.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Map<String, DirectSession> sessions;
    private final AtomicInteger sessionCounter;
    private volatile Consumer<DirectConnectEvent> eventHandler;
    private volatile Consumer<DirectMessage> messageHandler;

    public DirectConnectManager() {
        this.httpClient = HttpClient.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.sessions = new ConcurrentHashMap<>();
        this.sessionCounter = new AtomicInteger(0);
    }

    public String createDirectSession(DirectConnectConfig config) {
        String sessionId = generateSessionId();
        DirectSession session = new DirectSession(sessionId, config.endpoint(), config.authToken());
        sessions.put(sessionId, session);

        notifyEvent(new DirectConnectEvent(sessionId, DirectEventType.CREATED, null));

        executor.submit(() -> establishConnection(session, config));

        return sessionId;
    }

    private void establishConnection(DirectSession session, DirectConnectConfig config) {
        try {
            notifyEvent(new DirectConnectEvent(session.id(), DirectEventType.CONNECTING, null));

            httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer " + config.authToken())
                .buildAsync(URI.create(session.endpoint()), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        session.setConnected(true);
                        session.setWebSocket(webSocket);
                        notifyEvent(new DirectConnectEvent(session.id(), DirectEventType.CONNECTED, null));
                        webSocket.request(1);
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        if (data != null && messageHandler != null) {
                            messageHandler.accept(new DirectMessage(session.id(), data.toString(), System.currentTimeMillis()));
                        }
                        webSocket.request(1);
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        session.setConnected(false);
                        notifyEvent(new DirectConnectEvent(session.id(), DirectEventType.ERROR, error.getMessage()));
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        session.setConnected(false);
                        notifyEvent(new DirectConnectEvent(session.id(), DirectEventType.CLOSED, reason));
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }
                });

        } catch (Exception e) {
            log.error("Failed to create direct connection: {}", e.getMessage());
            notifyEvent(new DirectConnectEvent(session.id(), DirectEventType.ERROR, e.getMessage()));
        }
    }

    public boolean sendMessage(String sessionId, String message) {
        DirectSession session = sessions.get(sessionId);
        if (session != null && session.isConnected() && session.getWebSocket() != null) {
            session.getWebSocket().sendText(message, true);
            return true;
        }
        return false;
    }

    public boolean sendBinary(String sessionId, ByteBuffer data) {
        DirectSession session = sessions.get(sessionId);
        if (session != null && session.isConnected() && session.getWebSocket() != null) {
            session.getWebSocket().sendBinary(data, true);
            return true;
        }
        return false;
    }

    public void closeSession(String sessionId) {
        DirectSession session = sessions.remove(sessionId);
        if (session != null) {
            session.setConnected(false);
            WebSocket ws = session.getWebSocket();
            if (ws != null) {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "Session closed");
            }
            notifyEvent(new DirectConnectEvent(sessionId, DirectEventType.CLOSED, null));
            log.info("Direct session {} closed", sessionId);
        }
    }

    public Optional<DirectSessionInfo> getSessionInfo(String sessionId) {
        DirectSession session = sessions.get(sessionId);
        if (session != null) {
            return Optional.of(new DirectSessionInfo(sessionId, session.endpoint(), session.isConnected()));
        }
        return Optional.empty();
    }

    public int getActiveSessionCount() {
        return (int) sessions.values().stream().filter(DirectSession::isConnected).count();
    }

    public void registerConnection(String connectionId, String endpoint) {
        sessions.put(connectionId, new DirectSession(connectionId, endpoint, null));
        log.info("Registered direct connection: {}", connectionId);
    }

    public boolean removeConnection(String connectionId) {
        DirectSession removed = sessions.remove(connectionId);
        if (removed != null) {
            log.info("Removed direct connection: {}", connectionId);
            return true;
        }
        return false;
    }

    public Optional<DirectSessionInfo> getConnection(String connectionId) {
        DirectSession session = sessions.get(connectionId);
        if (session != null) {
            return Optional.of(new DirectSessionInfo(connectionId, session.endpoint(), session.isConnected()));
        }
        return Optional.empty();
    }

    public int connectionCount() {
        return sessions.size();
    }

    public void onEvent(Consumer<DirectConnectEvent> handler) {
        this.eventHandler = handler;
    }

    public void onMessage(Consumer<DirectMessage> handler) {
        this.messageHandler = handler;
    }

    private void notifyEvent(DirectConnectEvent event) {
        if (eventHandler != null) {
            eventHandler.accept(event);
        }
    }

    public void shutdown() {
        log.info("Shutting down DirectConnectManager");
        sessions.keySet().forEach(this::closeSession);
        executor.shutdown();
    }

    private String generateSessionId() {
        return "direct-" + sessionCounter.incrementAndGet() + "-" + System.currentTimeMillis();
    }

    public enum DirectEventType {
        CREATED, CONNECTING, CONNECTED, ERROR, CLOSED
    }

    public enum DirectStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR, CLOSED
    }

    public record DirectConnectConfig(String endpoint, String authToken) {}

    public record DirectConnectEvent(String sessionId, DirectEventType type, String message) {}

    public record DirectMessage(String sessionId, String payload, long timestamp) {}

    public record DirectSessionInfo(String id, String endpoint, boolean active) {}

    public static class DirectSession {
        private final String id;
        private final String endpoint;
        private final String authToken;
        private final AtomicBoolean connected = new AtomicBoolean(false);
        private volatile WebSocket webSocket;

        public DirectSession(String id, String endpoint, String authToken) {
            this.id = id;
            this.endpoint = endpoint;
            this.authToken = authToken;
        }

        public String id() { return id; }
        public String endpoint() { return endpoint; }
        public String authToken() { return authToken; }
        public boolean isConnected() { return connected.get(); }
        public void setConnected(boolean val) { connected.set(val); }
        public WebSocket getWebSocket() { return webSocket; }
        public void setWebSocket(WebSocket ws) { this.webSocket = ws; }
    }
}
