package com.claudecode.bridge;

import java.time.Instant;

public sealed interface InboundMessage permits
    InboundTextMessage, InboundToolCall, InboundToolResult,
    InboundError, InboundPing, InboundPong {

    String id();
    String sessionId();
    Instant timestamp();
}