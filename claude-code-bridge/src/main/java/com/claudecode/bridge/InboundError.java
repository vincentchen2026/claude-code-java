package com.claudecode.bridge;

import java.time.Instant;

public record InboundError(
    String id,
    String sessionId,
    Instant timestamp,
    String errorCode,
    String errorMessage
) implements InboundMessage {}