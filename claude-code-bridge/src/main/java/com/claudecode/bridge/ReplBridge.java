package com.claudecode.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ReplBridge implements Flow.Subscriber<String> {

    private static final Logger log = LoggerFactory.getLogger(ReplBridge.class);
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final Duration BASE_RECONNECT_DELAY = Duration.ofSeconds(1);
    private static final Duration MAX_RECONNECT_DELAY = Duration.ofSeconds(30);
    private static final Duration PING_INTERVAL = Duration.ofSeconds(30);

    private final String endpoint;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final ConcurrentLinkedQueue<String> pendingMessages;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private volatile Consumer<String> messageHandler;
    private volatile Consumer<Throwable> errorHandler;
    private volatile Runnable disconnectHandler;
    private volatile WebSocket webSocket;
    private Flow.Subscription subscription;

    public ReplBridge(String endpoint) {
        this.endpoint = endpoint;
        this.httpClient = HttpClient.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.pendingMessages = new ConcurrentLinkedQueue<>();
    }

    public void onMessage(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public void onError(Consumer<Throwable> handler) {
        this.errorHandler = handler;
    }

    public void onDisconnect(Runnable handler) {
        this.disconnectHandler = handler;
    }

    public boolean connect() {
        if (!connected.compareAndSet(false, true)) {
            log.warn("Already connected");
            return true;
        }

        log.info("Connecting to WebSocket endpoint: {}", endpoint);
        try {
            httpClient.newWebSocketBuilder()
                .subprotocols("bridge.v1")
                .buildAsync(URI.create(endpoint), new WebSocketListener())
                .whenComplete((ws, ex) -> {
                    if (ex != null) {
                        log.error("WebSocket connection failed: {}", ex.getMessage());
                        handleDisconnection();
                    } else {
                        webSocket = ws;
                    }
                });
            return true;
        } catch (Exception e) {
            log.error("Failed to initiate WebSocket connection", e);
            connected.set(false);
            return false;
        }
    }

    public void disconnect() {
        if (!connected.compareAndSet(true, false)) {
            return;
        }

        log.info("Disconnecting from WebSocket endpoint");
        reconnecting.set(false);
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnect");
        }
        executor.shutdown();
    }

    public boolean send(String message) {
        if (!connected.get()) {
            log.warn("Cannot send — not connected");
            return false;
        }
        if (webSocket != null) {
            webSocket.sendText(message, true);
            log.debug("Sent message ({} chars)", message.length());
            return true;
        }
        pendingMessages.offer(message);
        return true;
    }

    public boolean isConnected() {
        return connected.get() && webSocket != null;
    }

    public boolean reconnect() {
        if (reconnecting.compareAndSet(false, true)) {
            try {
                int attempt = reconnectAttempts.incrementAndGet();
                if (attempt > MAX_RECONNECT_ATTEMPTS) {
                    log.error("Max reconnect attempts ({}) exceeded", MAX_RECONNECT_ATTEMPTS);
                    reconnecting.set(false);
                    return false;
                }

                Duration delay = calculateBackoff(attempt);
                log.info("Reconnect attempt {}/{} after {}ms", attempt, MAX_RECONNECT_ATTEMPTS, delay.toMillis());

                Thread.sleep(delay);
                boolean success = connect();
                reconnecting.set(false);
                if (success && isConnected()) {
                    reconnectAttempts.set(0);
                }
                return success;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                reconnecting.set(false);
                return false;
            }
        }
        return false;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }

    public Duration calculateBackoff(int attempt) {
        long delayMs = BASE_RECONNECT_DELAY.toMillis() * (1L << Math.min(attempt - 1, 10));
        return Duration.ofMillis(Math.min(delayMs, MAX_RECONNECT_DELAY.toMillis()));
    }

    private void handleDisconnection() {
        boolean wasConnected = connected.getAndSet(false);
        if (wasConnected && disconnectHandler != null) {
            disconnectHandler.run();
        }
        if (reconnecting.get()) {
            reconnect();
        }
    }

    private void flushPendingMessages() {
        String msg;
        while ((msg = pendingMessages.poll()) != null) {
            send(msg);
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(String item) {
        send(item);
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("Flow error: {}", throwable.getMessage());
        if (errorHandler != null) {
            errorHandler.accept(throwable);
        }
        handleDisconnection();
    }

    @Override
    public void onComplete() {
        log.info("Message stream completed");
        handleDisconnection();
    }

    private class WebSocketListener implements WebSocket.Listener {
        private ByteBuffer buffer = ByteBuffer.allocate(8192);

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("WebSocket connected: {}", endpoint);
            connected.set(true);
            flushPendingMessages();
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (messageHandler != null && data != null) {
                messageHandler.accept(data.toString());
            }
            webSocket.request(1);
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            log.debug("Received binary data ({} bytes)", data.remaining());
            webSocket.request(1);
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            log.debug("Received ping");
            return webSocket.sendPong(ByteBuffer.wrap(new byte[0]));
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            log.debug("Received pong");
            return WebSocket.Listener.super.onPong(webSocket, message);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("WebSocket error: {}", error.getMessage());
            if (errorHandler != null) {
                errorHandler.accept(error);
            }
            handleDisconnection();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.info("WebSocket closed: {} - {}", statusCode, reason);
            handleDisconnection();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
    }
}
