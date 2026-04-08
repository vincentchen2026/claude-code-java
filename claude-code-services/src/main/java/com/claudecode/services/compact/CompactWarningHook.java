package com.claudecode.services.compact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class CompactWarningHook {

    private static final Logger log = LoggerFactory.getLogger(CompactWarningHook.class);

    private final List<WarningListener> listeners;
    private volatile boolean enabled;

    public CompactWarningHook() {
        this.listeners = new CopyOnWriteArrayList<>();
        this.enabled = true;
    }

    public void addListener(WarningListener listener) {
        listeners.add(listener);
    }

    public void removeListener(WarningListener listener) {
        listeners.remove(listener);
    }

    public void emitWarning(CompactWarning warning) {
        if (!enabled) return;

        log.debug("Emitting compact warning: {} - {}", warning.category(), warning.message());

        for (var listener : listeners) {
            try {
                listener.onWarning(warning);
            } catch (Exception e) {
                log.warn("Warning listener threw exception", e);
            }
        }
    }

    public void emitPreCompactWarning(CompactContext context) {
        if (context.messageCount() > 100) {
            emitWarning(new CompactWarning(
                "HIGH_MESSAGE_COUNT",
                "High message count before compaction",
                WarningCategory.PRE_COMPACT,
                context.messageCount()
            ));
        }

        if (context.tokenCount() > 80000) {
            emitWarning(new CompactWarning(
                "HIGH_TOKEN_COUNT",
                "High token count before compaction",
                WarningCategory.PRE_COMPACT,
                context.tokenCount()
            ));
        }
    }

    public void emitPostCompactWarning(CompactContext context, CompactionResult result) {
        int postMessageCount = result.summaryMessages().size() + result.attachments().size();
        if (postMessageCount > context.messageCount() * 0.9) {
            emitWarning(new CompactWarning(
                "MINIMAL_REDUCTION",
                "Compaction achieved minimal message reduction",
                WarningCategory.POST_COMPACT,
                postMessageCount
            ));
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public enum WarningCategory {
        PRE_COMPACT,
        POST_COMPACT,
        INTERNAL
    }

    public record CompactWarning(
        String code,
        String message,
        WarningCategory category,
        Object detail
    ) {}

    public record CompactContext(
        int messageCount,
        int tokenCount,
        String sessionId
    ) {}

    public interface WarningListener {
        void onWarning(CompactWarning warning);
    }
}