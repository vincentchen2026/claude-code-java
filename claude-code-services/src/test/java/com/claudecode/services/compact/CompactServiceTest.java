package com.claudecode.services.compact;

import com.claudecode.core.message.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CompactService} — microcompact, autocompact, and full compaction.
 */
class CompactServiceTest {

    private CompactService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new CompactService();
    }

    // --- Helper methods ---

    private AssistantMessage assistantWithToolUse(String toolUseId, String toolName) {
        ObjectNode input = mapper.createObjectNode();
        input.put("path", "/tmp/test.txt");
        ToolUseBlock toolUse = new ToolUseBlock(toolUseId, toolName, input);
        AssistantContent content = AssistantContent.of(List.of(toolUse));
        return new AssistantMessage("asst-" + toolUseId, content);
    }

    private AssistantMessage assistantWithId(String uuid, String apiId, String text) {
        TextBlock tb = new TextBlock(text);
        AssistantContent content = AssistantContent.of(apiId, List.of(tb));
        return new AssistantMessage(uuid, content);
    }

    private UserMessage userWithToolResult(String toolUseId, String text) {
        TextBlock textBlock = new TextBlock(text);
        ToolResultBlock resultBlock = new ToolResultBlock(toolUseId, List.of(textBlock), false);
        MessageContent mc = MessageContent.ofBlocks(List.of(resultBlock));
        return new UserMessage("user-" + toolUseId, mc, false, false, null,
                MessageOrigin.TOOL_RESULT, null, Instant.now());
    }

    private UserMessage simpleUserMessage(String uuid, String text) {
        return new UserMessage(uuid, MessageContent.ofText(text));
    }

    private AssistantMessage simpleAssistantMessage(String uuid, String text) {
        return new AssistantMessage(uuid, AssistantContent.of(List.of(new TextBlock(text))));
    }

    private String longString(int length) {
        return "x".repeat(length);
    }

    // ========== MicroCompact Tests ==========

    @Nested
    class MicroCompactTests {

        @Test
        void collectsCompactableToolUseIds() {
            AssistantMessage readMsg = assistantWithToolUse("tu-1", "Read");
            AssistantMessage bashMsg = assistantWithToolUse("tu-2", "Bash");
            AssistantMessage grepMsg = assistantWithToolUse("tu-3", "Grep");

            List<Message> messages = List.of(readMsg, bashMsg, grepMsg);
            Set<String> ids = service.collectCompactableToolIds(messages);

            assertEquals(Set.of("tu-1", "tu-2", "tu-3"), ids);
        }

        @Test
        void ignoresNonCompactableTools() {
            AssistantMessage agentMsg = assistantWithToolUse("tu-agent", "AgentTool");
            AssistantMessage mcpMsg = assistantWithToolUse("tu-mcp", "MCPTool");

            List<Message> messages = List.of(agentMsg, mcpMsg);
            Set<String> ids = service.collectCompactableToolIds(messages);

            assertTrue(ids.isEmpty());
        }

        @Test
        void collectsAllSixCompactableTools() {
            List<Message> messages = List.of(
                    assistantWithToolUse("tu-r", "Read"),
                    assistantWithToolUse("tu-b", "Bash"),
                    assistantWithToolUse("tu-grep", "Grep"),
                    assistantWithToolUse("tu-glob", "Glob"),
                    assistantWithToolUse("tu-e", "Edit"),
                    assistantWithToolUse("tu-w", "Write")
            );
            Set<String> ids = service.collectCompactableToolIds(messages);
            assertEquals(6, ids.size());
        }

        @Test
        void truncatesLongToolResultContent() {
            String longText = longString(15000);
            AssistantMessage asst = assistantWithToolUse("tu-1", "Read");
            UserMessage user = userWithToolResult("tu-1", longText);

            MicrocompactResult result = service.microcompactMessages(List.of(asst, user));

            assertEquals(2, result.messages().size());
            UserMessage resultUser = (UserMessage) result.messages().get(1);
            ToolResultBlock tr = (ToolResultBlock) resultUser.message().blocks().get(0);
            TextBlock tb = (TextBlock) tr.content().get(0);

            assertTrue(tb.text().length() < longText.length());
            assertTrue(tb.text().endsWith("... [truncated, 15000 chars total]"));
        }

        @Test
        void preservesShortToolResultContent() {
            String shortText = "File contents here";
            AssistantMessage asst = assistantWithToolUse("tu-1", "Read");
            UserMessage user = userWithToolResult("tu-1", shortText);

            MicrocompactResult result = service.microcompactMessages(List.of(asst, user));

            UserMessage resultUser = (UserMessage) result.messages().get(1);
            ToolResultBlock tr = (ToolResultBlock) resultUser.message().blocks().get(0);
            TextBlock tb = (TextBlock) tr.content().get(0);

            assertEquals(shortText, tb.text());
        }

        @Test
        void preservesContentExactlyAtThreshold() {
            String exactText = longString(CompactService.TRUNCATION_THRESHOLD);
            AssistantMessage asst = assistantWithToolUse("tu-1", "Read");
            UserMessage user = userWithToolResult("tu-1", exactText);

            MicrocompactResult result = service.microcompactMessages(List.of(asst, user));

            UserMessage resultUser = (UserMessage) result.messages().get(1);
            ToolResultBlock tr = (ToolResultBlock) resultUser.message().blocks().get(0);
            TextBlock tb = (TextBlock) tr.content().get(0);

            assertEquals(exactText, tb.text());
        }

        @Test
        void preservesNonCompactableToolResults() {
            String longText = longString(15000);
            AssistantMessage asst = assistantWithToolUse("tu-agent", "AgentTool");
            UserMessage user = userWithToolResult("tu-agent", longText);

            MicrocompactResult result = service.microcompactMessages(List.of(asst, user));

            UserMessage resultUser = (UserMessage) result.messages().get(1);
            ToolResultBlock tr = (ToolResultBlock) resultUser.message().blocks().get(0);
            TextBlock tb = (TextBlock) tr.content().get(0);

            assertEquals(longText, tb.text());
        }

        @Test
        void handlesMixedCompactableAndNonCompactable() {
            String longText = longString(15000);
            AssistantMessage readAsst = assistantWithToolUse("tu-read", "Read");
            AssistantMessage agentAsst = assistantWithToolUse("tu-agent", "AgentTool");
            UserMessage readUser = userWithToolResult("tu-read", longText);
            UserMessage agentUser = userWithToolResult("tu-agent", longText);

            MicrocompactResult result = service.microcompactMessages(
                    List.of(readAsst, readUser, agentAsst, agentUser));

            UserMessage readResult = (UserMessage) result.messages().get(1);
            ToolResultBlock readTr = (ToolResultBlock) readResult.message().blocks().get(0);
            TextBlock readTb = (TextBlock) readTr.content().get(0);
            assertTrue(readTb.text().contains("[truncated,"));

            UserMessage agentResult = (UserMessage) result.messages().get(3);
            ToolResultBlock agentTr = (ToolResultBlock) agentResult.message().blocks().get(0);
            TextBlock agentTb = (TextBlock) agentTr.content().get(0);
            assertEquals(longText, agentTb.text());
        }

        @Test
        void handlesEmptyMessageList() {
            MicrocompactResult result = service.microcompactMessages(List.of());
            assertTrue(result.messages().isEmpty());
        }

        @Test
        void handlesMessagesWithNoToolUse() {
            UserMessage textMsg = new UserMessage("u1",
                    MessageContent.ofText("Hello, how are you?"));
            AssistantMessage asstMsg = new AssistantMessage("a1",
                    AssistantContent.of(List.of(new TextBlock("I'm fine!"))));

            MicrocompactResult result = service.microcompactMessages(List.of(textMsg, asstMsg));
            assertEquals(2, result.messages().size());
        }

        @Test
        void truncateTextFormat() {
            String text = longString(12345);
            String truncated = CompactService.truncateText(text);
            assertEquals(CompactService.TRUNCATION_THRESHOLD, truncated.indexOf("..."));
            assertTrue(truncated.endsWith("... [truncated, 12345 chars total]"));
        }
    }

    // ========== AutoCompact Tests ==========

    @Nested
    class AutoCompactTests {

        @Test
        void shouldAutoCompactWhenTokensExceedThreshold() {
            // Create messages with enough text to exceed 93% of effective window
            // Effective window = 200000 - 20000 = 180000, threshold = 180000 * 0.93 = 167400
            // Need ~167400 tokens → ~167400 * 4 / (4/3) = ~501600 chars
            String bigText = "x".repeat(510_000);
            UserMessage msg = new UserMessage("u1", MessageContent.ofText(bigText));

            assertTrue(service.shouldAutoCompact(List.of(msg), "claude-sonnet-4-20250514", "user"));
        }

        @Test
        void shouldNotAutoCompactWhenTokensBelowThreshold() {
            UserMessage msg = simpleUserMessage("u1", "Hello world");
            assertFalse(service.shouldAutoCompact(List.of(msg), "claude-sonnet-4-20250514", "user"));
        }

        @Test
        void shouldNotAutoCompactWhenQuerySourceIsCompact() {
            String bigText = "x".repeat(510_000);
            UserMessage msg = new UserMessage("u1", MessageContent.ofText(bigText));

            assertFalse(service.shouldAutoCompact(List.of(msg), "claude-sonnet-4-20250514", "compact"));
        }

        @Test
        void shouldNotAutoCompactWhenQuerySourceIsSessionMemory() {
            String bigText = "x".repeat(510_000);
            UserMessage msg = new UserMessage("u1", MessageContent.ofText(bigText));

            assertFalse(service.shouldAutoCompact(List.of(msg), "claude-sonnet-4-20250514", "session_memory"));
        }

        @Test
        void shouldNotAutoCompactWhenDisabled() {
            service.setAutoCompactEnabled(false);
            String bigText = "x".repeat(510_000);
            UserMessage msg = new UserMessage("u1", MessageContent.ofText(bigText));

            assertFalse(service.shouldAutoCompact(List.of(msg), "claude-sonnet-4-20250514", "user"));
        }

        @Test
        void shouldAutoCompactUsesDefaultWindowForUnknownModel() {
            // Unknown model should use DEFAULT_CONTEXT_WINDOW (200K)
            String bigText = "x".repeat(510_000);
            UserMessage msg = new UserMessage("u1", MessageContent.ofText(bigText));

            assertTrue(service.shouldAutoCompact(List.of(msg), "unknown-model", "user"));
        }

        @Test
        void shouldAutoCompactHandlesNullModel() {
            String bigText = "x".repeat(510_000);
            UserMessage msg = new UserMessage("u1", MessageContent.ofText(bigText));

            assertTrue(service.shouldAutoCompact(List.of(msg), null, "user"));
        }
    }

    // ========== Full Compaction Tests ==========

    @Nested
    class FullCompactionTests {

        @Test
        void compactConversationWithNoOpSummarizer() {
            CompactSummarizer noOp = new NoOpCompactSummarizer();
            CompactService svc = new CompactService(TokenEstimator.getInstance(), noOp, true);

            UserMessage u1 = simpleUserMessage("u1", "Hello");
            AssistantMessage a1 = simpleAssistantMessage("a1", "Hi there");

            CompactionResult result = svc.compactConversation(List.of(u1, a1), false, null);

            assertNotNull(result.boundaryMarker());
            assertEquals("compact_boundary", result.boundaryMarker().subtype());
            assertTrue(result.boundaryMarker().content().contains("type=manual"));
            assertFalse(result.summaryMessages().isEmpty());
            assertTrue(result.attachments().isEmpty());
            assertTrue(result.preCompactTokenCount() > 0);
        }

        @Test
        void compactConversationAutoType() {
            CompactSummarizer noOp = new NoOpCompactSummarizer();
            CompactService svc = new CompactService(TokenEstimator.getInstance(), noOp, true);

            UserMessage u1 = simpleUserMessage("u1", "Hello");
            AssistantMessage a1 = simpleAssistantMessage("a1", "Hi there");

            CompactionResult result = svc.compactConversation(List.of(u1, a1), true, null);

            assertTrue(result.boundaryMarker().content().contains("type=auto"));
        }

        @Test
        void compactConversationWithExplicitSummarizer() {
            CompactSummarizer noOp = new NoOpCompactSummarizer();
            // Service has no summarizer, but we pass one explicitly
            CompactService svc = new CompactService(TokenEstimator.getInstance(), null, true);

            UserMessage u1 = simpleUserMessage("u1", "Hello");
            AssistantMessage a1 = simpleAssistantMessage("a1", "Hi there");

            CompactionResult result = svc.compactConversation(List.of(u1, a1), noOp, false);

            assertNotNull(result.boundaryMarker());
            assertFalse(result.summaryMessages().isEmpty());
        }

        @Test
        void compactConversationThrowsOnEmptyMessages() {
            CompactSummarizer noOp = new NoOpCompactSummarizer();
            CompactService svc = new CompactService(TokenEstimator.getInstance(), noOp, true);

            assertThrows(CompactException.class,
                    () -> svc.compactConversation(List.of(), false, null));
        }

        @Test
        void compactConversationThrowsWithNoSummarizer() {
            CompactService svc = new CompactService(TokenEstimator.getInstance(), null, true);

            UserMessage u1 = simpleUserMessage("u1", "Hello");
            assertThrows(CompactException.class,
                    () -> svc.compactConversation(List.of(u1), false, null));
        }

        @Test
        void compactConversationHandlesPTLRetry() {
            // Summarizer that returns "prompt is too long" on first call, then succeeds
            var callCount = new int[]{0};
            CompactSummarizer retrySummarizer = (msgs, prompt) -> {
                callCount[0]++;
                if (callCount[0] == 1) {
                    return "prompt is too long";
                }
                return "Summary of conversation";
            };

            CompactService svc = new CompactService(TokenEstimator.getInstance(), retrySummarizer, true);

            // Need at least 2 groups for truncation to work
            UserMessage u1 = simpleUserMessage("u1", "First message");
            AssistantMessage a1 = assistantWithId("a1", "api-1", "First response");
            UserMessage u2 = simpleUserMessage("u2", "Second message");
            AssistantMessage a2 = assistantWithId("a2", "api-2", "Second response");

            CompactionResult result = svc.compactConversation(List.of(u1, a1, u2, a2), false, null);

            assertNotNull(result);
            assertEquals(2, callCount[0]);
        }

        @Test
        void compactConversationThrowsAfterMaxPTLRetries() {
            CompactSummarizer alwaysPTL = (msgs, prompt) -> "prompt is too long";

            CompactService svc = new CompactService(TokenEstimator.getInstance(), alwaysPTL, true);

            // Need enough groups for 3 retries + the initial attempt
            List<Message> messages = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                messages.add(simpleUserMessage("u" + i, "Message " + i));
                messages.add(assistantWithId("a" + i, "api-" + i, "Response " + i));
            }

            assertThrows(CompactException.class,
                    () -> svc.compactConversation(messages, false, null));
        }

        @Test
        void buildPostCompactMessagesOrder() {
            CompactSummarizer noOp = new NoOpCompactSummarizer();
            CompactService svc = new CompactService(TokenEstimator.getInstance(), noOp, true);

            UserMessage u1 = simpleUserMessage("u1", "Hello");
            AssistantMessage a1 = simpleAssistantMessage("a1", "Hi");

            CompactionResult result = svc.compactConversation(List.of(u1, a1), false, null);
            List<Message> postCompact = result.buildPostCompactMessages();

            // First message should be the boundary marker
            assertInstanceOf(SystemMessage.class, postCompact.get(0));
            assertEquals("compact_boundary", ((SystemMessage) postCompact.get(0)).subtype());

            // Second message should be the summary
            assertInstanceOf(UserMessage.class, postCompact.get(1));
            assertTrue(((UserMessage) postCompact.get(1)).isCompactSummary());
        }
    }

    // ========== MessageGrouping Tests ==========

    @Nested
    class MessageGroupingTests {

        @Test
        void groupByApiRoundEmptyList() {
            List<List<Message>> groups = MessageGrouping.groupByApiRound(List.of());
            assertTrue(groups.isEmpty());
        }

        @Test
        void groupByApiRoundSingleAssistantGroup() {
            // A user message followed by an assistant message with a new ID
            // creates two groups: [user] and [assistant]
            UserMessage u1 = simpleUserMessage("u1", "Hello");
            AssistantMessage a1 = assistantWithId("a1", "api-1", "Hi");

            List<List<Message>> groups = MessageGrouping.groupByApiRound(List.of(u1, a1));
            // u1 goes into first group, a1 starts a new group (new assistant ID)
            assertEquals(2, groups.size());
            assertEquals(1, groups.get(0).size()); // [u1]
            assertEquals(1, groups.get(1).size()); // [a1]
        }

        @Test
        void groupByApiRoundMultipleGroups() {
            UserMessage u1 = simpleUserMessage("u1", "Hello");
            AssistantMessage a1 = assistantWithId("a1", "api-1", "Hi");
            UserMessage u2 = simpleUserMessage("u2", "How are you?");
            AssistantMessage a2 = assistantWithId("a2", "api-2", "Fine");

            List<List<Message>> groups = MessageGrouping.groupByApiRound(List.of(u1, a1, u2, a2));
            // [u1], [a1, u2], [a2]
            assertEquals(3, groups.size());
        }

        @Test
        void groupByApiRoundSameAssistantIdStaysTogether() {
            UserMessage u1 = simpleUserMessage("u1", "Hello");
            // Two assistant messages with same API ID (same API response)
            AssistantMessage a1a = assistantWithId("a1a", "api-1", "Part 1");
            AssistantMessage a1b = assistantWithId("a1b", "api-1", "Part 2");

            List<List<Message>> groups = MessageGrouping.groupByApiRound(List.of(u1, a1a, a1b));
            // [u1], [a1a, a1b] — same API ID stays together
            assertEquals(2, groups.size());
            assertEquals(1, groups.get(0).size()); // [u1]
            assertEquals(2, groups.get(1).size()); // [a1a, a1b]
        }

        @Test
        void groupByApiRoundPreservesAllMessages() {
            List<Message> messages = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                messages.add(simpleUserMessage("u" + i, "Msg " + i));
                messages.add(assistantWithId("a" + i, "api-" + i, "Resp " + i));
            }

            List<List<Message>> groups = MessageGrouping.groupByApiRound(messages);

            int totalMessages = groups.stream().mapToInt(List::size).sum();
            assertEquals(messages.size(), totalMessages);
        }
    }

    // ========== Compact Boundary Tests ==========

    @Nested
    class CompactBoundaryTests {

        @Test
        void createCompactBoundaryMarkerAuto() {
            SystemMessage marker = CompactService.createCompactBoundaryMarker("auto", 50000);

            assertEquals("compact_boundary", marker.subtype());
            assertEquals("info", marker.level());
            assertTrue(marker.content().contains("type=auto"));
            assertTrue(marker.content().contains("pre_compact_tokens=50000"));
            assertNotNull(marker.uuid());
        }

        @Test
        void createCompactBoundaryMarkerManual() {
            SystemMessage marker = CompactService.createCompactBoundaryMarker("manual", 75000);

            assertTrue(marker.content().contains("type=manual"));
            assertTrue(marker.content().contains("pre_compact_tokens=75000"));
        }
    }

    // ========== TruncateHeadForPTLRetry Tests ==========

    @Nested
    class TruncateHeadTests {

        @Test
        void truncateHeadRemovesOldestGroup() {
            CompactSummarizer noOp = new NoOpCompactSummarizer();
            CompactService svc = new CompactService(TokenEstimator.getInstance(), noOp, true);

            UserMessage u1 = simpleUserMessage("u1", "First");
            AssistantMessage a1 = assistantWithId("a1", "api-1", "Response 1");
            UserMessage u2 = simpleUserMessage("u2", "Second");
            AssistantMessage a2 = assistantWithId("a2", "api-2", "Response 2");

            // Groups: [u1], [a1, u2], [a2] — removing first group [u1] leaves [a1, u2, a2]
            List<Message> truncated = svc.truncateHeadForPTLRetry(List.of(u1, a1, u2, a2));

            assertEquals(3, truncated.size());
            assertEquals("a1", truncated.get(0).uuid());
        }

        @Test
        void truncateHeadThrowsWithSingleGroup() {
            CompactSummarizer noOp = new NoOpCompactSummarizer();
            CompactService svc = new CompactService(TokenEstimator.getInstance(), noOp, true);

            UserMessage u1 = simpleUserMessage("u1", "Only message");

            assertThrows(CompactException.class,
                    () -> svc.truncateHeadForPTLRetry(List.of(u1)));
        }
    }

    // ========== NoOpCompactSummarizer Tests ==========

    @Nested
    class NoOpCompactSummarizerTests {

        @Test
        void summarizeConcatenatesText() {
            NoOpCompactSummarizer noOp = new NoOpCompactSummarizer();

            UserMessage u1 = simpleUserMessage("u1", "Hello");
            AssistantMessage a1 = simpleAssistantMessage("a1", "World");

            String summary = noOp.summarize(List.of(u1, a1), "prompt");
            assertTrue(summary.contains("Hello"));
            assertTrue(summary.contains("World"));
        }

        @Test
        void summarizeHandlesEmptyList() {
            NoOpCompactSummarizer noOp = new NoOpCompactSummarizer();
            String summary = noOp.summarize(List.of(), "prompt");
            assertEquals("", summary);
        }
    }

    // ========== createPostCompactAttachments Tests ==========

    @Nested
    class PostCompactAttachmentsTests {

        @Test
        void createPostCompactAttachmentsReturnsEmptyList() {
            List<Message> attachments = service.createPostCompactAttachments();
            assertNotNull(attachments);
            assertTrue(attachments.isEmpty());
        }
    }
}
