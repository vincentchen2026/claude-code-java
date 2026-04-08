package com.claudecode.ui;

import com.claudecode.core.message.*;

import java.util.List;

/**
 * Dispatches SDKMessage rendering to the appropriate renderer.
 * Each message type is rendered differently:
 * - Assistant → MarkdownRenderer
 * - StreamEvent (content_block_delta) → incremental text print
 * - System → gray/dim text
 * - Error → red text
 * - Result → token usage summary
 * - Tool use blocks → tool name + brief input summary
 */
public class MessageRenderDispatcher {

    private final TerminalRenderer terminal;
    private final MarkdownRenderer markdownRenderer;

    public MessageRenderDispatcher(TerminalRenderer terminal, MarkdownRenderer markdownRenderer) {
        this.terminal = terminal;
        this.markdownRenderer = markdownRenderer;
    }

    /**
     * Render an SDKMessage to the terminal.
     */
    public void render(SDKMessage message) {
        switch (message) {
            case SDKMessage.Assistant assistant -> renderAssistant(assistant);
            case SDKMessage.StreamEvent streamEvent -> renderStreamEvent(streamEvent);
            case SDKMessage.System system -> renderSystem(system);
            case SDKMessage.Error error -> renderError(error);
            case SDKMessage.Result result -> renderResult(result);
            case SDKMessage.Progress progress -> renderProgress(progress);
            case SDKMessage.User ignored -> { /* User messages are not rendered back */ }
            case SDKMessage.Sentinel ignored -> { /* Terminal sentinel, no rendering */ }
            case SDKMessage.Attachment attachment -> renderAttachment(attachment);
            case SDKMessage.Tombstone tombstone -> renderTombstone(tombstone);
            case SDKMessage.CompactBoundary boundary -> renderCompactBoundary(boundary);
            case SDKMessage.ToolUseSummary summary -> renderToolUseSummary(summary);
            case SDKMessage.ApiRetry retry -> renderApiRetry(retry);
            case SDKMessage.StreamRequestStart start -> renderStreamRequestStart(start);
        }
    }

    void renderAssistant(SDKMessage.Assistant assistant) {
        if (assistant.message() == null || assistant.message().message() == null) {
            return;
        }
        AssistantContent content = assistant.message().message();
        if (content.content() == null) {
            return;
        }
        for (ContentBlock block : content.content()) {
            switch (block) {
                case TextBlock text -> {
                    String rendered = markdownRenderer.render(text.text());
                    terminal.print(rendered);
                }
                case ToolUseBlock toolUse -> renderToolUse(toolUse);
                case ThinkingBlock thinking -> {
                    terminal.println(Ansi.colored("  💭 " + truncate(thinking.thinking(), 100), AnsiColor.GRAY));
                }
                default -> { /* Other block types not rendered */ }
            }
        }
        terminal.println("");
    }

    void renderStreamEvent(SDKMessage.StreamEvent event) {
        if ("content_block_delta".equals(event.eventType()) && event.data() instanceof String text) {
            // Print text delta incrementally without newline
            terminal.print(text);
        }
    }

    void renderSystem(SDKMessage.System system) {
        if (system.message() == null) {
            return;
        }
        String content = system.message().content();
        if (content == null || content.isEmpty()) {
            return;
        }
        String level = system.message().level();
        if ("warning".equals(level)) {
            terminal.println(Ansi.colored("⚠ " + content, AnsiColor.YELLOW));
        } else if ("error".equals(level)) {
            terminal.println(Ansi.colored("✗ " + content, AnsiColor.RED));
        } else {
            terminal.println(Ansi.styled(content, AnsiStyle.DIM));
        }
    }

    void renderError(SDKMessage.Error error) {
        String message = error.exception() != null
                ? error.exception().getMessage()
                : "Unknown error";
        terminal.println(Ansi.colored("✗ Error: " + message, AnsiColor.RED));
    }

    void renderResult(SDKMessage.Result result) {
        if (result.totalUsage() == null) {
            return;
        }
        Usage usage = result.totalUsage();
        String summary = String.format(
                "Token usage: %d input, %d output (%d total)",
                usage.inputTokens(), usage.outputTokens(), usage.totalTokens());

        if (usage.cacheReadInputTokens() > 0) {
            summary += String.format(" | Cache read: %d", usage.cacheReadInputTokens());
        }

        terminal.println(Ansi.styled(summary, AnsiStyle.DIM));
    }

    void renderProgress(SDKMessage.Progress progress) {
        if (progress.message() != null && progress.message().content() != null) {
            terminal.println(Ansi.colored("⟳ " + progress.message().content(), AnsiColor.CYAN));
        }
    }

    void renderAttachment(SDKMessage.Attachment attachment) {
        terminal.println(Ansi.styled("[attachment:" + attachment.attachmentType() + "]", AnsiStyle.DIM));
    }

    void renderTombstone(SDKMessage.Tombstone tombstone) {
        terminal.println(Ansi.styled("[tombstone: replaced " + tombstone.replacedUuid() + "]", AnsiStyle.DIM));
    }

    void renderCompactBoundary(SDKMessage.CompactBoundary boundary) {
        terminal.println(Ansi.styled(
            "--- compact boundary (" + boundary.compactedMessageUuids().size() + " messages) ---",
            AnsiStyle.DIM));
    }

    void renderToolUseSummary(SDKMessage.ToolUseSummary summary) {
        terminal.println(Ansi.styled(
            "[tool summary: " + summary.toolName() + "] " + summary.summary(),
            AnsiStyle.DIM));
    }

    void renderApiRetry(SDKMessage.ApiRetry retry) {
        terminal.println(Ansi.colored(
            "⟳ API retry: " + retry.reason() + " (attempt " + retry.retryCount() + ")",
            AnsiColor.YELLOW));
    }

    void renderStreamRequestStart(SDKMessage.StreamRequestStart start) {
        terminal.println(Ansi.styled(
            "→ streaming request: model=" + start.model() + ", messages=" + start.messageCount(),
            AnsiStyle.DIM));
    }

    void renderToolUse(ToolUseBlock toolUse) {
        String inputSummary = "";
        if (toolUse.input() != null) {
            String inputStr = toolUse.input().toString();
            inputSummary = truncate(inputStr, 80);
        }
        terminal.println(Ansi.colored("  🔧 " + toolUse.name() + " " + inputSummary, AnsiColor.CYAN));
    }

    static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
