package com.claudecode.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SSHSessionManager {

    private static final Logger log = LoggerFactory.getLogger(SSHSessionManager.class);
    private static final int DEFAULT_PORT = 22;
    private static final Duration SESSION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final Map<String, SSHSession> sessions;
    private final ExecutorService executor;
    private final AtomicInteger sessionCounter;
    private volatile Consumer<SSHChannelEvent> channelHandler;
    private volatile Consumer<SSHSessionEvent> sessionHandler;

    public SSHSessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.sessionCounter = new AtomicInteger(0);
    }

    public String createSession(SSHConnectionConfig config) {
        String sessionId = generateSessionId();
        SSHSession session = new SSHSession(
            sessionId,
            config.host(),
            config.port() > 0 ? config.port() : DEFAULT_PORT,
            config.username(),
            config.authMethod(),
            config.privateKey(),
            config.passphrase(),
            Instant.now()
        );

        sessions.put(sessionId, session);
        notifySessionEvent(new SSHSessionEvent(sessionId, SSHEventType.CREATED, null));

        executor.submit(() -> connectSession(session, config));

        return sessionId;
    }

    private void connectSession(SSHSession session, SSHConnectionConfig config) {
        try {
            updateSessionStatus(session.id(), SSHConnectionStatus.CONNECTING);
            notifySessionEvent(new SSHSessionEvent(session.id(), SSHEventType.CONNECTING, null));

            try (Socket socket = new Socket()) {
                socket.connect(
                    new InetSocketAddress(session.host(), session.port()),
                    (int) CONNECT_TIMEOUT.toMillis()
                );

                session.setConnected(true);
                updateSessionStatus(session.id(), SSHConnectionStatus.CONNECTED);
                notifySessionEvent(new SSHSessionEvent(session.id(), SSHEventType.CONNECTED, null));
                log.info("SSH session {} connected to {}:{}", session.id(), session.host(), session.port());

                session.setInputStream(socket.getInputStream());
                session.setOutputStream(socket.getOutputStream());

                startReading(session);
            }
        } catch (IOException e) {
            log.error("SSH session {} connection failed: {}", session.id(), e.getMessage());
            updateSessionStatus(session.id(), SSHConnectionStatus.ERROR);
            notifySessionEvent(new SSHSessionEvent(session.id(), SSHEventType.ERROR, e.getMessage()));
        }
    }

    private void startReading(SSHSession session) {
        executor.submit(() -> {
            byte[] buffer = new byte[8192];
            try {
                InputStream in = session.getInputStream();
                if (in == null) return;

                int bytesRead;
                while (session.isConnected() && (bytesRead = in.read(buffer)) != -1) {
                    String data = new String(buffer, 0, bytesRead);
                    notifyChannelEvent(new SSHChannelEvent(session.id(), SSHChannelEventType.DATA_RECEIVED, data));
                }
                session.setConnected(false);
                updateSessionStatus(session.id(), SSHConnectionStatus.DISCONNECTED);
                notifySessionEvent(new SSHSessionEvent(session.id(), SSHEventType.DISCONNECTED, "Connection closed"));
            } catch (IOException e) {
                if (session.isConnected()) {
                    log.error("SSH session {} read error: {}", session.id(), e.getMessage());
                    session.setConnected(false);
                    updateSessionStatus(session.id(), SSHConnectionStatus.ERROR);
                    notifySessionEvent(new SSHSessionEvent(session.id(), SSHEventType.ERROR, e.getMessage()));
                }
            }
        });
    }

    public boolean sendData(String sessionId, String data) {
        SSHSession session = sessions.get(sessionId);
        if (session != null && session.isConnected()) {
            try {
                OutputStream out = session.getOutputStream();
                if (out != null) {
                    out.write(data.getBytes());
                    out.flush();
                    notifyChannelEvent(new SSHChannelEvent(sessionId, SSHChannelEventType.DATA_SENT, data));
                    return true;
                }
            } catch (IOException e) {
                log.error("SSH session {} send error: {}", sessionId, e.getMessage());
            }
        }
        return false;
    }

    public boolean sendData(String sessionId, byte[] data) {
        SSHSession session = sessions.get(sessionId);
        if (session != null && session.isConnected()) {
            try {
                OutputStream out = session.getOutputStream();
                if (out != null) {
                    out.write(data);
                    out.flush();
                    return true;
                }
            } catch (IOException e) {
                log.error("SSH session {} send error: {}", sessionId, e.getMessage());
            }
        }
        return false;
    }

    public void closeSession(String sessionId) {
        SSHSession session = sessions.remove(sessionId);
        if (session != null) {
            session.setConnected(false);
            updateSessionStatus(sessionId, SSHConnectionStatus.CLOSED);
            notifySessionEvent(new SSHSessionEvent(sessionId, SSHEventType.CLOSED, null));
            log.info("SSH session {} closed", sessionId);
        }
    }

    public SSHConnectionStatus getSessionStatus(String sessionId) {
        SSHSession session = sessions.get(sessionId);
        return session != null ? session.getStatus() : SSHConnectionStatus.NOT_FOUND;
    }

    public SSHSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
            .filter(s -> s.getStatus() == SSHConnectionStatus.CONNECTED)
            .count();
    }

    public void onChannelEvent(Consumer<SSHChannelEvent> handler) {
        this.channelHandler = handler;
    }

    public void onSessionEvent(Consumer<SSHSessionEvent> handler) {
        this.sessionHandler = handler;
    }

    private void updateSessionStatus(String sessionId, SSHConnectionStatus status) {
        SSHSession session = sessions.get(sessionId);
        if (session != null) {
            session.setStatus(status);
        }
    }

    private void notifyChannelEvent(SSHChannelEvent event) {
        if (channelHandler != null) {
            channelHandler.accept(event);
        }
    }

    private void notifySessionEvent(SSHSessionEvent event) {
        if (sessionHandler != null) {
            sessionHandler.accept(event);
        }
    }

    public void shutdown() {
        log.info("Shutting down SSHSessionManager");
        sessions.keySet().forEach(this::closeSession);
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateSessionId() {
        return "ssh-" + sessionCounter.incrementAndGet() + "-" + System.currentTimeMillis();
    }

    public enum SSHConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR, CLOSED, NOT_FOUND
    }

    public enum SSHEventType {
        CREATED, CONNECTING, CONNECTED, DISCONNECTED, ERROR, CLOSED
    }

    public enum SSHChannelEventType {
        DATA_SENT, DATA_RECEIVED, EOF, ERROR
    }

    public record SSHConnectionConfig(
        String host,
        int port,
        String username,
        String authMethod,
        String privateKey,
        String passphrase
    ) {}

    public static class SSHSession {
        private final String id;
        private final String host;
        private final int port;
        private final String username;
        private final String authMethod;
        private final String privateKey;
        private final String passphrase;
        private final Instant createdAt;
        private final AtomicBoolean connected = new AtomicBoolean(false);
        private volatile InputStream inputStream;
        private volatile OutputStream outputStream;
        private volatile SSHConnectionStatus status = SSHConnectionStatus.DISCONNECTED;

        public SSHSession(String id, String host, int port, String username, String authMethod,
                         String privateKey, String passphrase, Instant createdAt) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.username = username;
            this.authMethod = authMethod;
            this.privateKey = privateKey;
            this.passphrase = passphrase;
            this.createdAt = createdAt;
        }

        public String id() { return id; }
        public String host() { return host; }
        public int port() { return port; }
        public String username() { return username; }
        public String authMethod() { return authMethod; }
        public String privateKey() { return privateKey; }
        public String passphrase() { return passphrase; }
        public Instant createdAt() { return createdAt; }
        public boolean isConnected() { return connected.get(); }
        public void setConnected(boolean val) { connected.set(val); }
        public SSHConnectionStatus getStatus() { return status; }
        public void setStatus(SSHConnectionStatus s) { status = s; }
        public InputStream getInputStream() { return inputStream; }
        public void setInputStream(InputStream s) { inputStream = s; }
        public OutputStream getOutputStream() { return outputStream; }
        public void setOutputStream(OutputStream s) { outputStream = s; }
    }

    public record SSHSessionEvent(String sessionId, SSHEventType type, String message) {}

    public record SSHChannelEvent(String sessionId, SSHChannelEventType type, String data) {}
}
