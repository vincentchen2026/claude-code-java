package com.claudecode.bridge;

import java.time.Instant;

public record InboundPing(
    String id,
    String sessionId,
    Instant timestamp
) implements InboundMessage {}