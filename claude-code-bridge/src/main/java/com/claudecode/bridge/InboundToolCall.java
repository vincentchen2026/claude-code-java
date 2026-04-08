package com.claudecode.bridge;

import java.time.Instant;

public record InboundToolCall(
    String id,
    String sessionId,
    Instant timestamp,
    String toolName,
    String arguments
) implements InboundMessage {}