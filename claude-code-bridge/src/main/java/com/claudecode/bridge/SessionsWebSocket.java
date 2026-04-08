package com.claudecode.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SessionsWebSocket {

    private static final Logger log = LoggerFactory.getLogger(SessionsWebSocket.class);
    private static final Duration PING_INTERVAL = Duration.ofSeconds(25);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final Map<String, SessionWebSocket> activeSessions;
    private final AtomicInteger sessionCounter;
    private volatile Consumer<SessionMessage> messageHandler;
    private volatile Consumer<SessionState> stateHandler;

    public SessionsWebSocket() {
        this.httpClient = HttpClient.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.activeSessions = new ConcurrentHashMap<>();
        this.sessionCounter = new AtomicInteger(0);
    }

    public SessionsWebSocket(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.activeSessions = new ConcurrentHashMap<>();
        this.sessionCounter = new AtomicInteger(0);
    }

    public String createSession(String remoteUrl, String sessionToken) {
        String sessionId = generateSessionId();
        SessionWebSocket ws = new SessionWebSocket(sessionId, remoteUrl, sessionToken);
        activeSessions.put(sessionId, ws);

        ws.connect().whenComplete((success, error) -> {
            if (success) {
                log.info("Session {} connected to {}", sessionId, remoteUrl);
                notifyState(new SessionState(sessionId, SessionStatus.CONNECTED, null));
            } else {
                log.error("Session {} failed to connect: {}", sessionId, error != null ? error.getMessage() : "unknown");
                notifyState(new SessionState(sessionId, SessionStatus.ERROR, error != null ? error.getMessage() : "unknown"));
            }
        });

        return sessionId;
    }

    public boolean sendMessage(String sessionId, String message) {
        SessionWebSocket ws = activeSessions.get(sessionId);
        if (ws != null && ws.isConnected()) {
            ws.sendText(message, true);
            return true;
        }
        return false;
    }

    public boolean sendBinary(String sessionId, ByteBuffer data) {
        SessionWebSocket ws = activeSessions.get(sessionId);
        if (ws != null && ws.isConnected()) {
            ws.sendBinary(data, true);
            return true;
        }
        return false;
    }

    public void closeSession(String sessionId) {
        SessionWebSocket ws = activeSessions.remove(sessionId);
        if (ws != null) {
            ws.disconnect();
            notifyState(new SessionState(sessionId, SessionStatus.CLOSED, null));
            log.info("Session {} closed", sessionId);
        }
    }

    public SessionStatus getSessionStatus(String sessionId) {
        SessionWebSocket ws = activeSessions.get(sessionId);
        return ws != null ? ws.getStatus() : SessionStatus.NOT_FOUND;
    }

    public int getActiveSessionCount() {
        return (int) activeSessions.values().stream()
            .filter(ws -> ws.getStatus() == SessionStatus.CONNECTED)
            .count();
    }

    public void onMessage(Consumer<SessionMessage> handler) {
        this.messageHandler = handler;
    }

    public void onStateChange(Consumer<SessionState> handler) {
        this.stateHandler = handler;
    }

    private void notifyMessage(String sessionId, String message) {
        if (messageHandler != null) {
            messageHandler.accept(new SessionMessage(sessionId, message, System.currentTimeMillis()));
        }
    }

    private void notifyState(SessionState state) {
        if (stateHandler != null) {
            stateHandler.accept(state);
        }
    }

    public void shutdown() {
        log.info("Shutting down SessionsWebSocket");
        activeSessions.keySet().forEach(this::closeSession);
        scheduler.shutdown();
        executor.shutdown();
    }

    private String generateSessionId() {
        return "session-" + sessionCounter.incrementAndGet() + "-" + System.currentTimeMillis();
    }

    private class SessionWebSocket {
        private final String sessionId;
        private final String remoteUrl;
        private final String sessionToken;
        private final AtomicBoolean connected = new AtomicBoolean(false);
        private volatile WebSocket webSocket;
        private volatile SessionStatus status = SessionStatus.DISCONNECTED;

        SessionWebSocket(String sessionId, String remoteUrl, String sessionToken) {
            this.sessionId = sessionId;
            this.remoteUrl = remoteUrl;
            this.sessionToken = sessionToken;
        }

        java.util.concurrent.CompletableFuture<Boolean> connect() {
            return httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer " + sessionToken)
                .buildAsync(URI.create(remoteUrl), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        connected.set(true);
                        status = SessionStatus.CONNECTED;
                        SessionWebSocket.this.webSocket = webSocket;
                        webSocket.request(1);
                        startPingTask();
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        if (data != null) {
                            notifyMessage(sessionId, data.toString());
                        }
                        webSocket.request(1);
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        status = SessionStatus.ERROR;
                        connected.set(false);
                        notifyState(new SessionState(sessionId, SessionStatus.ERROR, error.getMessage()));
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        status = SessionStatus.CLOSED;
                        connected.set(false);
                        notifyState(new SessionState(sessionId, SessionStatus.CLOSED, reason));
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }
                })
                .thenApply(ws -> {
                    this.webSocket = ws;
                    return true;
                })
                .exceptionally(ex -> {
                    status = SessionStatus.ERROR;
                    connected.set(false);
                    return false;
                });
        }

        void disconnect() {
            connected.set(false);
            status = SessionStatus.DISCONNECTED;
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Session disconnect");
            }
        }

        void sendText(String message, boolean last) {
            if (webSocket != null) {
                webSocket.sendText(message, last);
            }
        }

        void sendBinary(ByteBuffer data, boolean last) {
            if (webSocket != null) {
                webSocket.sendBinary(data, last);
            }
        }

        boolean isConnected() {
            return connected.get() && webSocket != null;
        }

        SessionStatus getStatus() {
            return status;
        }

        private void startPingTask() {
            scheduler.scheduleAtFixedRate(() -> {
                if (isConnected() && webSocket != null) {
                    webSocket.sendPing(ByteBuffer.wrap(new byte[0]));
                }
            }, PING_INTERVAL.toMillis(), PING_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    public enum SessionStatus {
        CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED, CLOSED, ERROR, NOT_FOUND
    }

    public record SessionMessage(String sessionId, String message, long timestamp) {}

    public record SessionState(String sessionId, SessionStatus status, String errorMessage) {}
}
