package com.claudecode.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BridgeMainTest {

    @Test
    void bridgeStartsAndStops() throws InterruptedException {
        var transport = new BridgeTransport.WsTransportV1("ws://localhost", 8080, "/ws");
        var bridge = new BridgeMain(transport);

        assertFalse(bridge.isRunning());

        Thread runner = Thread.ofVirtual().start(bridge::run);
        Thread.sleep(100);
        assertTrue(bridge.isRunning());

        bridge.stop();
        runner.join(2000);
        assertFalse(bridge.isRunning());
    }

    @Test
    void ackDoesNotThrow() {
        var transport = new BridgeTransport.WsTransportV1("ws://localhost", 8080, "/ws");
        var bridge = new BridgeMain(transport);
        assertDoesNotThrow(() -> bridge.ack("work-item-1"));
    }
}
