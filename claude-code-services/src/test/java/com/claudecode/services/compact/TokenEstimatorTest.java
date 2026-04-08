package com.claudecode.services.compact;

import com.claudecode.core.message.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TokenEstimator}.
 */
class TokenEstimatorTest {

    private final TokenEstimator estimator = TokenEstimator.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    // --- Character-based estimation ---

    @Test
    void estimateTokenCountForTextMessages() {
        // 400 chars → 400/4 = 100 raw tokens → 100 * 4/3 ≈ 133
        String text = "a".repeat(400);
        UserMessage msg = new UserMessage("u1", MessageContent.ofText(text));

        long tokens = estimator.estimateTokenCount(List.of(msg));

        assertEquals(133, tokens);
    }

    @Test
    void estimateTokenCountForEmptyList() {
        assertEquals(0, estimator.estimateTokenCount(List.of()));
    }

    @Test
    void estimateTokenCountForAssistantMessage() {
        // 800 chars in text block → 800/4 = 200 raw → 200 * 4/3 ≈ 267
        TextBlock tb = new TextBlock("b".repeat(800));
        AssistantMessage msg = new AssistantMessage("a1", AssistantContent.of(List.of(tb)));

        long tokens = estimator.estimateTokenCount(List.of(msg));

        assertEquals(267, tokens);
    }

    @Test
    void estimateTokenCountForToolUseBlock() {
        ObjectNode input = mapper.createObjectNode();
        input.put("path", "/tmp/test.txt");
        ToolUseBlock tu = new ToolUseBlock("tu-1", "Read", input);
        AssistantMessage msg = new AssistantMessage("a1", AssistantContent.of(List.of(tu)));

        long tokens = estimator.estimateTokenCount(List.of(msg));

        // name "Read" (4 chars) + input JSON string length
        assertTrue(tokens > 0);
    }

    @Test
    void estimateTokenCountForToolResultBlock() {
        TextBlock inner = new TextBlock("c".repeat(1200));
        ToolResultBlock tr = new ToolResultBlock("tu-1", List.of(inner), false);
        UserMessage msg = new UserMessage("u1", MessageContent.ofBlocks(List.of(tr)),
                false, false, null, MessageOrigin.TOOL_RESULT, null, Instant.now());

        long tokens = estimator.estimateTokenCount(List.of(msg));

        // 1200 chars → 1200/4 = 300 raw → 300 * 4/3 = 400
        assertEquals(400, tokens);
    }

    @Test
    void estimateTokenCountMultipleMessages() {
        // 400 chars user + 400 chars assistant = 800 total chars
        // 800/4 = 200 raw → 200 * 4/3 ≈ 267
        UserMessage user = new UserMessage("u1", MessageContent.ofText("a".repeat(400)));
        AssistantMessage asst = new AssistantMessage("a1",
                AssistantContent.of(List.of(new TextBlock("b".repeat(400)))));

        long tokens = estimator.estimateTokenCount(List.of(user, asst));

        assertEquals(267, tokens);
    }

    // --- Exact count from Usage ---

    @Test
    void getExactTokenCountFromUsage() {
        Usage usage = new Usage(1500, 300, 0, 0);
        assertEquals(1500, estimator.getExactTokenCount(usage));
    }

    @Test
    void getExactTokenCountFromNullUsage() {
        assertEquals(0, estimator.getExactTokenCount(null));
    }

    @Test
    void getExactTokenCountFromEmptyUsage() {
        assertEquals(0, estimator.getExactTokenCount(Usage.EMPTY));
    }

    // --- estimateMessageChars ---

    @Test
    void estimateMessageCharsForTextUser() {
        UserMessage msg = new UserMessage("u1", MessageContent.ofText("Hello world"));
        assertEquals(11, estimator.estimateMessageChars(msg));
    }

    @Test
    void estimateMessageCharsForAssistantWithThinking() {
        ThinkingBlock thinking = new ThinkingBlock("Let me think about this...");
        TextBlock text = new TextBlock("Here's my answer.");
        AssistantMessage msg = new AssistantMessage("a1",
                AssistantContent.of(List.of(thinking, text)));

        long chars = estimator.estimateMessageChars(msg);

        // "Let me think about this..." = 26 chars, "Here's my answer." = 17 chars
        assertEquals(26 + 17, chars);
    }

    @Test
    void estimateMessageCharsForNullContent() {
        AssistantMessage msg = new AssistantMessage("a1", null);
        assertEquals(0, estimator.estimateMessageChars(msg));
    }

    @Test
    void estimateMessageCharsIgnoresNonUserAssistant() {
        // SystemMessage and other types return 0
        SystemMessage sys = new SystemMessage("s1", "info", "info", "System message");
        assertEquals(0, estimator.estimateMessageChars(sys));
    }
}
