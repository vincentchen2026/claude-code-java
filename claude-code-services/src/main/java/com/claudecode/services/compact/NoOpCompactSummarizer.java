package com.claudecode.services.compact;

import com.claudecode.core.message.*;

import java.util.List;

/**
 * A no-op summarizer that concatenates message text content for testing purposes.
 * Does not call any LLM — simply joins text from all messages.
 */
public class NoOpCompactSummarizer implements CompactSummarizer {

    @Override
    public String summarize(List<Message> messages, String compactPrompt) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            String text = extractText(msg);
            if (!text.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append(text);
            }
        }
        return sb.toString();
    }

    private String extractText(Message msg) {
        if (msg instanceof UserMessage um && um.message() != null) {
            if (um.message().isText() && um.message().text() != null) {
                return um.message().text();
            }
            if (um.message().blocks() != null) {
                StringBuilder sb = new StringBuilder();
                for (ContentBlock block : um.message().blocks()) {
                    if (block instanceof TextBlock tb && tb.text() != null) {
                        sb.append(tb.text());
                    }
                }
                return sb.toString();
            }
        } else if (msg instanceof AssistantMessage am
                && am.message() != null && am.message().content() != null) {
            StringBuilder sb = new StringBuilder();
            for (ContentBlock block : am.message().content()) {
                if (block instanceof TextBlock tb && tb.text() != null) {
                    sb.append(tb.text());
                }
            }
            return sb.toString();
        } else if (msg instanceof SystemMessage sm) {
            return sm.content() != null ? sm.content() : "";
        }
        return "";
    }
}
