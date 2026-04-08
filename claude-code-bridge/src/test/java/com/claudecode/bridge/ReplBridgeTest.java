package com.claudecode.bridge;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ReplBridgeTest {

    @Test
    void backoffCalculation() {
        var bridge = new ReplBridge("ws://localhost:8080/ws");
        assertEquals(Duration.ofSeconds(1), bridge.calculateBackoff(1));
        assertEquals(Duration.ofSeconds(2), bridge.calculateBackoff(2));
        assertEquals(Duration.ofSeconds(4), bridge.calculateBackoff(3));
        assertTrue(bridge.calculateBackoff(20).toSeconds() <= 30);
    }

    @Test
    void sendFailsWhenNotConnected() {
        var bridge = new ReplBridge("ws://localhost:8080/ws");
        assertFalse(bridge.isConnected());
        assertFalse(bridge.send("hello"));
    }

    @Test
    void connectInitiatesConnection() {
        var bridge = new ReplBridge("ws://localhost:8080/ws");
        assertFalse(bridge.isConnected());
        assertTrue(bridge.connect());
    }

    @Test
    void disconnectClearsConnection() {
        var bridge = new ReplBridge("ws://localhost:8080/ws");
        bridge.connect();
        bridge.disconnect();
        assertFalse(bridge.isConnected());
    }

    @Test
    void reconnectAttemptsIncrementsEvenWithoutConnection() {
        var bridge = new ReplBridge("ws://localhost:8080/ws");
        assertEquals(0, bridge.getReconnectAttempts());
        bridge.reconnect();
        assertEquals(1, bridge.getReconnectAttempts());
    }
}
