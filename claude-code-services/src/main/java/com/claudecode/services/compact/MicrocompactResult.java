package com.claudecode.services.compact;

import com.claudecode.core.message.Message;

import java.util.List;

/**
 * Result of a microcompact operation — the (possibly truncated) message list.
 */
public record MicrocompactResult(List<Message> messages) {
}
