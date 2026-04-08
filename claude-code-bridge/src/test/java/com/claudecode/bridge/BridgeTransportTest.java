package com.claudecode.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BridgeTransportTest {

    @Test
    void wsTransportV1HasCorrectVersion() {
        var transport = new BridgeTransport.WsTransportV1("ws://localhost", 8080, "/ws");
        assertEquals("v1", transport.version());
        assertEquals("ws://localhost", transport.endpoint());
        assertEquals(8080, transport.port());
        assertEquals("/ws", transport.path());
    }

    @Test
    void ccrTransportV2HasCorrectVersion() {
        var transport = new BridgeTransport.CcrTransportV2("https://remote.example.com", "token123", true);
        assertEquals("v2", transport.version());
        assertEquals("https://remote.example.com", transport.endpoint());
        assertEquals("token123", transport.authToken());
        assertTrue(transport.useTls());
    }

    @Test
    void sealedInterfacePermitsOnlyTwoTypes() {
        BridgeTransport ws = new BridgeTransport.WsTransportV1("ws://localhost", 8080, "/ws");
        BridgeTransport ccr = new BridgeTransport.CcrTransportV2("https://remote", "tok", false);
        assertInstanceOf(BridgeTransport.WsTransportV1.class, ws);
        assertInstanceOf(BridgeTransport.CcrTransportV2.class, ccr);
    }
}
