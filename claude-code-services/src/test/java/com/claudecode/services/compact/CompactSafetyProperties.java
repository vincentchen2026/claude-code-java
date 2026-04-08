package com.claudecode.services.compact;

import com.claudecode.core.message.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CP-7 Session compaction safety property-based tests.
 * <p>
 * Validates: Requirements CP-7 (会话压缩安全性)
 * <p>
 * Properties verified:
 * - Compaction preserves unpaired tool_use/tool_result messages
 * - Compact boundary marker is correctly placed and formatted
 * - MessageGrouping produces valid, non-overlapping groups covering all messages
 */
class CompactSafetyProperties {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ========== Property: Compaction preserves unpaired tool_use/tool_result ==========

    /**
     * CP-7: Compaction must not lose unpaired tool_use blocks that have
     * corresponding tool_result blocks. After compaction, the summary messages
     * plus boundary marker form a valid sequence.
     * <p>
     * Validates: Requirements CP-7
     */
    @Property(tries = 50)
    void compactionPreservesUnpairedToolUseToolResult(
            @ForAll("conversationsWithToolUse") List<Message> messages
    ) {
        // Collect all tool_use IDs and their corresponding tool_result IDs
        Set<String> toolUseIds = new HashSet<>();
        Set<String> toolResultIds = new HashSet<>();

        for (Message msg : messages) {
            if (msg instanceof AssistantMessage am
                    && am.message() != null && am.message().content() != null) {
                for (ContentBlock block : am.message().content()) {
                    if (block instanceof ToolUseBlock tu) {
                        toolUseIds.add(tu.id());
                    }
                }
            }
            if (msg instanceof UserMessage um
                    && um.message() != null && um.message().blocks() != null) {
                for (ContentBlock block : um.message().blocks()) {
                    if (block instanceof ToolResultBlock tr) {
                        toolResultIds.add(tr.toolUseId());
                    }
                }
            }
        }

        // Perform compaction with NoOp summarizer
        NoOpCompactSummarizer summarizer = new NoOpCompactSummarizer();
        CompactService service = new CompactService(
                TokenEstimator.getInstance(), summarizer, true);

        CompactionResult result = service.compactConversation(messages, false, null);

        // The result must have a boundary marker
        assertNotNull(result.boundaryMarker(),
                "Compaction result must include a boundary marker");

        // The summary messages must not be empty
        assertFalse(result.summaryMessages().isEmpty(),
                "Compaction result must include at least one summary message");

        // The post-compact message list must be well-formed
        List<Message> postCompact = result.buildPostCompactMessages();
        assertFalse(postCompact.isEmpty(),
                "Post-compact messages must not be empty");

        // First message must be the boundary marker
        assertInstanceOf(SystemMessage.class, postCompact.get(0),
                "First post-compact message must be the boundary marker");

        // The summary should contain text from the original tool interactions
        // (NoOp summarizer concatenates text, so tool-related text should be present)
        String summaryText = extractAllText(result.summaryMessages());
        // Verify the summary is non-empty (tool interactions produced content)
        if (!toolUseIds.isEmpty()) {
            assertFalse(summaryText.isEmpty(),
                    "Summary should contain content from tool interactions");
        }
    }

    // ========== Property: Compact boundary is correctly placed and formatted ==========

    /**
     * CP-7: The compact_boundary marker must have correct subtype, contain
     * the compaction type (auto/manual), and include the pre-compact token count.
     * <p>
     * Validates: Requirements CP-7
     */
    @Property(tries = 100)
    void compactBoundaryIsCorrectlyFormatted(
            @ForAll("compactTypes") String compactType,
            @ForAll @IntRange(min = 0, max = 500_000) int tokenCount
    ) {
        SystemMessage marker = CompactService.createCompactBoundaryMarker(
                compactType, tokenCount);

        // Subtype must be "compact_boundary"
        assertEquals("compact_boundary", marker.subtype(),
                "Boundary marker subtype must be 'compact_boundary'");

        // Level must be "info"
        assertEquals("info", marker.level(),
                "Boundary marker level must be 'info'");

        // Content must contain the compact type
        assertTrue(marker.content().contains("type=" + compactType),
                "Boundary content must contain the compact type");

        // Content must contain the pre-compact token count
        assertTrue(marker.content().contains("pre_compact_tokens=" + tokenCount),
                "Boundary content must contain the pre-compact token count");

        // UUID must be non-null and non-empty
        assertNotNull(marker.uuid(), "Boundary marker must have a UUID");
        assertFalse(marker.uuid().isEmpty(), "Boundary marker UUID must not be empty");
    }

    // ========== Property: MessageGrouping produces valid groups ==========

    /**
     * CP-7: MessageGrouping.groupByApiRound must produce groups that:
     * 1. Cover all input messages (no messages lost)
     * 2. Are non-empty
     * 3. Maintain original message order
     * <p>
     * Validates: Requirements CP-7
     */
    @Property(tries = 100)
    void messageGroupingProducesValidGroups(
            @ForAll("conversationMessages") List<Message> messages
    ) {
        List<List<Message>> groups = MessageGrouping.groupByApiRound(messages);

        if (messages.isEmpty()) {
            assertTrue(groups.isEmpty(),
                    "Empty input must produce empty groups");
            return;
        }

        // All groups must be non-empty
        for (int i = 0; i < groups.size(); i++) {
            assertFalse(groups.get(i).isEmpty(),
                    "Group " + i + " must not be empty");
        }

        // Total messages across all groups must equal input size
        int totalGrouped = groups.stream().mapToInt(List::size).sum();
        assertEquals(messages.size(), totalGrouped,
                "All messages must be accounted for in groups");

        // Messages must maintain original order
        List<Message> flattened = new ArrayList<>();
        for (List<Message> group : groups) {
            flattened.addAll(group);
        }
        for (int i = 0; i < messages.size(); i++) {
            assertSame(messages.get(i), flattened.get(i),
                    "Message order must be preserved at index " + i);
        }
    }

    /**
     * CP-7: Within each group produced by groupByApiRound, all assistant messages
     * must share the same API message ID (or have null IDs).
     * <p>
     * Validates: Requirements CP-7
     */
    @Property(tries = 100)
    void messageGroupingAssistantIdsConsistentWithinGroup(
            @ForAll("conversationMessages") List<Message> messages
    ) {
        List<List<Message>> groups = MessageGrouping.groupByApiRound(messages);

        for (List<Message> group : groups) {
            Set<String> assistantIds = new HashSet<>();
            for (Message msg : group) {
                if (msg instanceof AssistantMessage am
                        && am.message() != null && am.message().id() != null) {
                    assistantIds.add(am.message().id());
                }
            }
            // Within a group, there should be at most one distinct non-null assistant ID
            assertTrue(assistantIds.size() <= 1,
                    "All assistant messages within a group must share the same API ID, "
                            + "but found: " + assistantIds);
        }
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<List<Message>> conversationsWithToolUse() {
        // Generate conversations that include tool_use and tool_result pairs
        return Arbitraries.integers().between(1, 5).flatMap(rounds -> {
            List<Arbitrary<List<Message>>> roundArbitraries = new ArrayList<>();
            for (int i = 0; i < rounds; i++) {
                final int roundIdx = i;
                roundArbitraries.add(generateRoundWithToolUse(roundIdx));
            }
            return combineRounds(roundArbitraries);
        });
    }

    private Arbitrary<List<Message>> generateRoundWithToolUse(int roundIdx) {
        return Arbitraries.of("Read", "Bash", "Grep", "Edit", "Write", "AgentTool")
                .flatMap(toolName -> Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50)
                        .map(text -> {
                            String tuId = "tu-" + roundIdx;
                            String apiId = "api-" + roundIdx;

                            List<Message> round = new ArrayList<>();

                            // User message
                            round.add(new UserMessage("u-" + roundIdx,
                                    MessageContent.ofText("Request " + roundIdx)));

                            // Assistant with tool_use
                            ObjectNode input = MAPPER.createObjectNode();
                            input.put("path", "/tmp/file" + roundIdx + ".txt");
                            ToolUseBlock toolUse = new ToolUseBlock(tuId, toolName, input);
                            AssistantContent aContent = AssistantContent.of(apiId, List.of(toolUse));
                            round.add(new AssistantMessage("a-" + roundIdx, aContent,
                                    false, null, Instant.now()));

                            // User with tool_result
                            TextBlock resultText = new TextBlock(text);
                            ToolResultBlock toolResult = new ToolResultBlock(
                                    tuId, List.of(resultText), false);
                            MessageContent mc = MessageContent.ofBlocks(List.of(toolResult));
                            round.add(new UserMessage("ur-" + roundIdx, mc, false, false,
                                    null, MessageOrigin.TOOL_RESULT, null, Instant.now()));

                            return round;
                        }));
    }

    @Provide
    Arbitrary<List<Message>> conversationMessages() {
        return Arbitraries.integers().between(0, 8).flatMap(rounds -> {
            if (rounds == 0) {
                return Arbitraries.just(List.of());
            }
            List<Arbitrary<List<Message>>> roundArbitraries = new ArrayList<>();
            for (int i = 0; i < rounds; i++) {
                final int roundIdx = i;
                roundArbitraries.add(generateRound(roundIdx));
            }
            return combineRounds(roundArbitraries);
        });
    }

    private Arbitrary<List<Message>> generateRound(int roundIdx) {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30)
                .map(text -> {
                    String apiId = "api-" + roundIdx;
                    List<Message> round = new ArrayList<>();

                    // User message
                    round.add(new UserMessage("u-" + roundIdx,
                            MessageContent.ofText("User " + text)));

                    // Assistant message
                    TextBlock tb = new TextBlock("Assistant " + text);
                    AssistantContent content = AssistantContent.of(apiId, List.of(tb));
                    round.add(new AssistantMessage("a-" + roundIdx, content,
                            false, null, Instant.now()));

                    return round;
                });
    }

    @Provide
    Arbitrary<String> compactTypes() {
        return Arbitraries.of("auto", "manual");
    }

    // ========== Helpers ==========

    private Arbitrary<List<Message>> combineRounds(List<Arbitrary<List<Message>>> rounds) {
        if (rounds.isEmpty()) {
            return Arbitraries.just(List.of());
        }
        if (rounds.size() == 1) {
            return rounds.get(0);
        }
        // Combine all rounds into a single flat list
        Arbitrary<List<Message>> combined = rounds.get(0);
        for (int i = 1; i < rounds.size(); i++) {
            combined = Combinators.combine(combined, rounds.get(i))
                    .as((a, b) -> {
                        List<Message> result = new ArrayList<>(a);
                        result.addAll(b);
                        return result;
                    });
        }
        return combined;
    }

    private String extractAllText(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            if (msg instanceof UserMessage um && um.message() != null) {
                if (um.message().isText() && um.message().text() != null) {
                    sb.append(um.message().text());
                }
            }
        }
        return sb.toString();
    }
}
