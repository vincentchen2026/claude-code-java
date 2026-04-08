package com.claudecode.bridge;

import java.time.Instant;

public record InboundPong(
    String id,
    String sessionId,
    Instant timestamp
) implements InboundMessage {}