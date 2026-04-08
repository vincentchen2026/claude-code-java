package com.claudecode.services.compact;

import com.claudecode.core.message.*;

import java.util.List;

/**
 * Estimates token counts for messages.
 * <p>
 * Provides both rough character-based estimation (~4 chars per token)
 * and exact counts from API {@link Usage} objects.
 * Corresponds to src/services/tokenEstimation.ts.
 */
public final class TokenEstimator {

    /** Default bytes/chars per token for rough estimation. */
    private static final double CHARS_PER_TOKEN = 4.0;

    /** Conservative padding multiplier (4/3) matching the TS implementation. */
    private static final double PADDING_MULTIPLIER = 4.0 / 3.0;

    private TokenEstimator() {
    }

    private static final TokenEstimator INSTANCE = new TokenEstimator();

    public static TokenEstimator getInstance() {
        return INSTANCE;
    }

    /**
     * Rough token count estimate based on character count (~4 chars per token),
     * padded by 4/3 to be conservative.
     */
    public long estimateTokenCount(List<Message> messages) {
        long totalChars = 0;
        for (Message msg : messages) {
            totalChars += estimateMessageChars(msg);
        }
        long rawTokens = Math.round(totalChars / CHARS_PER_TOKEN);
        return Math.round(rawTokens * PADDING_MULTIPLIER);
    }

    /**
     * Extract exact input token count from an API {@link Usage} response.
     */
    public long getExactTokenCount(Usage usage) {
        if (usage == null) {
            return 0;
        }
        return usage.inputTokens();
    }

    /**
     * Estimate the character count for a single message by summing
     * the text content of its content blocks.
     */
    public long estimateMessageChars(Message msg) {
        if (msg instanceof AssistantMessage am) {
            return estimateAssistantChars(am);
        } else if (msg instanceof UserMessage um) {
            return estimateUserChars(um);
        }
        // Other message types (system, progress, etc.) contribute minimally
        return 0;
    }

    private long estimateAssistantChars(AssistantMessage am) {
        if (am.message() == null || am.message().content() == null) {
            return 0;
        }
        long chars = 0;
        for (ContentBlock block : am.message().content()) {
            chars += estimateBlockChars(block);
        }
        return chars;
    }

    private long estimateUserChars(UserMessage um) {
        if (um.message() == null) {
            return 0;
        }
        MessageContent mc = um.message();
        if (mc.isText() && mc.text() != null) {
            return mc.text().length();
        }
        if (mc.blocks() != null) {
            long chars = 0;
            for (ContentBlock block : mc.blocks()) {
                chars += estimateBlockChars(block);
            }
            return chars;
        }
        return 0;
    }

    private long estimateBlockChars(ContentBlock block) {
        return switch (block) {
            case TextBlock tb -> tb.text() != null ? tb.text().length() : 0;
            case ToolUseBlock tu -> {
                long nameLen = tu.name() != null ? tu.name().length() : 0;
                long inputLen = tu.input() != null ? tu.input().toString().length() : 0;
                yield nameLen + inputLen;
            }
            case ToolResultBlock tr -> estimateToolResultChars(tr);
            case ThinkingBlock th -> th.thinking() != null ? th.thinking().length() : 0;
            case ImageBlock ignored -> 0; // Images are opaque; skip for char estimation
        };
    }

    private long estimateToolResultChars(ToolResultBlock tr) {
        if (tr.content() == null) {
            return 0;
        }
        long chars = 0;
        for (ContentBlock inner : tr.content()) {
            if (inner instanceof TextBlock tb && tb.text() != null) {
                chars += tb.text().length();
            }
        }
        return chars;
    }
}
