package com.claudecode.core.engine;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class PartialMessageForwarder {

    private final List<PartialMessage> incompleteMessages;
    private final MessageListener listener;
    private final AtomicBoolean enabled;

    public PartialMessageForwarder(MessageListener listener) {
        this.incompleteMessages = new CopyOnWriteArrayList<>();
        this.listener = listener;
        this.enabled = new AtomicBoolean(true);
    }

    public void startMessage(String messageId, String type) {
        if (!enabled.get()) return;

        PartialMessage partial = new PartialMessage(
            messageId,
            type,
            new StringBuilder(),
            Instant.now(),
            false
        );
        incompleteMessages.add(partial);
    }

    public void appendContent(String messageId, String content) {
        if (!enabled.get()) return;

        for (PartialMessage msg : incompleteMessages) {
            if (msg.messageId().equals(messageId)) {
                msg.content().append(content);
                listener.onPartialContent(messageId, content);
            }
        }
    }

    public void completeMessage(String messageId) {
        if (!enabled.get()) return;

        for (int i = 0; i < incompleteMessages.size(); i++) {
            PartialMessage msg = incompleteMessages.get(i);
            if (msg.messageId().equals(messageId)) {
                PartialMessage completed = new PartialMessage(
                    msg.messageId(),
                    msg.type(),
                    msg.content(),
                    msg.startedAt(),
                    true
                );
                incompleteMessages.remove(i);
                listener.onMessageComplete(completed);
                break;
            }
        }
    }

    public void cancelMessage(String messageId) {
        incompleteMessages.removeIf(msg -> msg.messageId().equals(messageId));
        listener.onMessageCancelled(messageId);
    }

    public List<PartialMessage> getIncompleteMessages() {
        return List.copyOf(incompleteMessages);
    }

    public boolean hasIncompleteMessages() {
        return !incompleteMessages.isEmpty();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void clear() {
        incompleteMessages.clear();
    }

    public record PartialMessage(
        String messageId,
        String type,
        StringBuilder content,
        Instant startedAt,
        boolean complete
    ) {
        public String contentAsString() {
            return content.toString();
        }
    }

    public interface MessageListener {
        void onPartialContent(String messageId, String content);
        default void onMessageComplete(PartialMessage message) {}
        default void onMessageCancelled(String messageId) {}
    }
}