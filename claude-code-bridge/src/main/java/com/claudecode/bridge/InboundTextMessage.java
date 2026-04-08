package com.claudecode.bridge;

import java.time.Instant;

public record InboundTextMessage(
    String id,
    String sessionId,
    Instant timestamp,
    String text
) implements InboundMessage {}