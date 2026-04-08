package com.claudecode.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BridgeDebugUtils {

    private static final Logger log = LoggerFactory.getLogger(BridgeDebugUtils.class);

    private final Map<String, ConnectionState> connections;
    private final AtomicInteger totalMessagesSent;
    private final AtomicInteger totalMessagesReceived;
    private final AtomicLong totalBytesSent;
    private final AtomicLong totalBytesReceived;

    public BridgeDebugUtils() {
        this.connections = new ConcurrentHashMap<>();
        this.totalMessagesSent = new AtomicInteger(0);
        this.totalMessagesReceived = new AtomicInteger(0);
        this.totalBytesSent = new AtomicLong(0);
        this.totalBytesReceived = new AtomicLong(0);
    }

    public void recordConnectionOpen(String sessionId) {
        connections.put(sessionId, new ConnectionState(
            sessionId,
            Instant.now(),
            ConnectionStatus.CONNECTED,
            0,
            0
        ));
        log.debug("Connection opened: {}", sessionId);
    }

    public void recordConnectionClose(String sessionId) {
        ConnectionState state = connections.get(sessionId);
        if (state != null) {
            connections.put(sessionId, new ConnectionState(
                state.sessionId(),
                state.connectedAt(),
                ConnectionStatus.DISCONNECTED,
                state.messagesSent(),
                state.messagesReceived()
            ));
            log.debug("Connection closed: {}", sessionId);
        }
    }

    public void recordMessageSent(String sessionId, int byteCount) {
        totalMessagesSent.incrementAndGet();
        totalBytesSent.addAndGet(byteCount);
        
        ConnectionState state = connections.get(sessionId);
        if (state != null) {
            connections.put(sessionId, new ConnectionState(
                state.sessionId(),
                state.connectedAt(),
                state.status(),
                state.messagesSent() + 1,
                state.messagesReceived()
            ));
        }
    }

    public void recordMessageReceived(String sessionId, int byteCount) {
        totalMessagesReceived.incrementAndGet();
        totalBytesReceived.addAndGet(byteCount);
        
        ConnectionState state = connections.get(sessionId);
        if (state != null) {
            connections.put(sessionId, new ConnectionState(
                state.sessionId(),
                state.connectedAt(),
                state.status(),
                state.messagesSent(),
                state.messagesReceived() + 1
            ));
        }
    }

    public ConnectionState getConnectionState(String sessionId) {
        return connections.get(sessionId);
    }

    public Map<String, ConnectionState> getAllConnectionStates() {
        return Map.copyOf(connections);
    }

    public DebugSummary getSummary() {
        return new DebugSummary(
            connections.size(),
            totalMessagesSent.get(),
            totalMessagesReceived.get(),
            totalBytesSent.get(),
            totalBytesReceived.get()
        );
    }

    public String formatDiagnostics() {
        var summary = getSummary();
        StringBuilder sb = new StringBuilder();
        sb.append("=== Bridge Diagnostics ===\n");
        sb.append("Active connections: ").append(summary.activeConnections()).append("\n");
        sb.append("Messages sent: ").append(summary.totalMessagesSent()).append("\n");
        sb.append("Messages received: ").append(summary.totalMessagesReceived()).append("\n");
        sb.append("Bytes sent: ").append(summary.totalBytesSent()).append("\n");
        sb.append("Bytes received: ").append(summary.totalBytesReceived()).append("\n");
        sb.append("\nConnection details:\n");
        
        for (var entry : connections.entrySet()) {
            var conn = entry.getValue();
            Duration duration = Duration.between(conn.connectedAt(), Instant.now());
            sb.append("  ").append(entry.getKey())
              .append(" - ").append(conn.status())
              .append(" - ").append(duration.toSeconds()).append("s")
              .append(" - sent:").append(conn.messagesSent())
              .append(" recv:").append(conn.messagesReceived())
              .append("\n");
        }
        
        return sb.toString();
    }

    public enum ConnectionStatus {
        CONNECTED, DISCONNECTED, RECONNECTING, ERROR
    }

    public record ConnectionState(
        String sessionId,
        Instant connectedAt,
        ConnectionStatus status,
        int messagesSent,
        int messagesReceived
    ) {}

    public record DebugSummary(
        int activeConnections,
        int totalMessagesSent,
        int totalMessagesReceived,
        long totalBytesSent,
        long totalBytesReceived
    ) {}
}