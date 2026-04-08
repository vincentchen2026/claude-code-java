package com.claudecode.bridge;

/**
 * Sealed interface representing bridge transport types.
 * Supports WebSocket v1 and CCR v2 transports.
 */
public sealed interface BridgeTransport permits BridgeTransport.WsTransportV1, BridgeTransport.CcrTransportV2 {

    /** Returns the transport protocol version. */
    String version();

    /** Returns the endpoint URL. */
    String endpoint();

    /**
     * WebSocket transport v1 — classic WebSocket connection.
     */
    record WsTransportV1(String endpoint, int port, String path) implements BridgeTransport {
        @Override
        public String version() { return "v1"; }
    }

    /**
     * CCR transport v2 — Claude Code Remote transport with enhanced features.
     */
    record CcrTransportV2(String endpoint, String authToken, boolean useTls) implements BridgeTransport {
        @Override
        public String version() { return "v2"; }
    }
}
