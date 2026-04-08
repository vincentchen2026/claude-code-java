package com.claudecode.services.compact;

import com.claudecode.core.message.AssistantMessage;
import com.claudecode.core.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Groups messages by API round (assistant message ID changes mark new groups).
 * Corresponds to src/services/compact/grouping.ts.
 */
public final class MessageGrouping {

    private MessageGrouping() {
    }

    /**
     * Group messages by API round. A new group starts when an assistant message
     * with a different {@code message.id()} is encountered.
     * <p>
     * Messages within the same API response share the same assistant content ID
     * and belong to the same group.
     *
     * @param messages the conversation messages
     * @return list of message groups (each group is a list of messages)
     */
    public static List<List<Message>> groupByApiRound(List<Message> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }

        List<List<Message>> groups = new ArrayList<>();
        List<Message> current = new ArrayList<>();
        String lastAssistantId = null;

        for (Message msg : messages) {
            if (msg instanceof AssistantMessage am) {
                String currentId = am.message() != null ? am.message().id() : null;
                if (!Objects.equals(currentId, lastAssistantId) && !current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                current.add(msg);
                lastAssistantId = currentId;
            } else {
                current.add(msg);
            }
        }

        if (!current.isEmpty()) {
            groups.add(current);
        }

        return groups;
    }
}
