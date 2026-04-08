package com.claudecode.bridge;

import java.time.Instant;

public record InboundToolResult(
    String id,
    String sessionId,
    Instant timestamp,
    String callId,
    String result,
    boolean isError
) implements InboundMessage {}