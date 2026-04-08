package com.claudecode.core.message;

import com.claudecode.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CP-4 Serialization compatibility property-based tests.
 * <p>
 * Validates: Requirements CP-4 (消息序列化兼容性)
 * <p>
 * Properties verified:
 * - Serialization roundtrip preserves all fields for all Message subtypes
 * - Serialization roundtrip preserves all fields for all ContentBlock subtypes
 * - All Message subtypes can be serialized/deserialized via the polymorphic interface
 */
class MessageSerializationProperties {

    private static final ObjectMapper MAPPER = JsonUtils.getMapper();

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> uuids() {
        return Arbitraries.create(() -> UUID.randomUUID().toString());
    }

    @Provide
    Arbitrary<String> nonEmptyStrings() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
    }

    @Provide
    Arbitrary<Instant> instants() {
        return Arbitraries.longs()
            .between(0L, 4_000_000_000L)
            .map(Instant::ofEpochSecond);
    }

    @Provide
    Arbitrary<TextBlock> textBlocks() {
        return nonEmptyStrings().map(TextBlock::new);
    }

    @Provide
    Arbitrary<ToolUseBlock> toolUseBlocks() {
        return Combinators.combine(
            nonEmptyStrings(),
            nonEmptyStrings(),
            Arbitraries.just(MAPPER.createObjectNode().put("key", "value"))
        ).as((id, name, input) -> new ToolUseBlock(id, name, (JsonNode) input));
    }

    @Provide
    Arbitrary<ToolResultBlock> toolResultBlocks() {
        return Combinators.combine(
            nonEmptyStrings(),
            textBlocks().list().ofMinSize(0).ofMaxSize(2).map(l -> (List<ContentBlock>) (List<? extends ContentBlock>) l),
            Arbitraries.of(true, false)
        ).as(ToolResultBlock::new);
    }

    @Provide
    Arbitrary<ThinkingBlock> thinkingBlocks() {
        return nonEmptyStrings().map(ThinkingBlock::new);
    }

    @Provide
    Arbitrary<ImageBlock> imageBlocks() {
        return Arbitraries.just(new ImageBlock(MAPPER.createObjectNode().put("type", "base64").put("data", "abc")));
    }

    @Provide
    Arbitrary<ContentBlock> contentBlocks() {
        return Arbitraries.oneOf(
            textBlocks().map(b -> (ContentBlock) b),
            toolUseBlocks().map(b -> (ContentBlock) b),
            toolResultBlocks().map(b -> (ContentBlock) b),
            thinkingBlocks().map(b -> (ContentBlock) b),
            imageBlocks().map(b -> (ContentBlock) b)
        );
    }

    @Provide
    Arbitrary<MessageContent> messageContents() {
        return nonEmptyStrings().map(MessageContent::ofText);
    }

    @Provide
    Arbitrary<AssistantContent> assistantContents() {
        return Combinators.combine(
            nonEmptyStrings(),
            textBlocks().list().ofMinSize(1).ofMaxSize(3).map(l -> (List<ContentBlock>) (List<? extends ContentBlock>) l)
        ).as(AssistantContent::of);
    }

    @Provide
    Arbitrary<UserMessage> userMessages() {
        return Combinators.combine(
            uuids(), messageContents(), Arbitraries.of(true, false),
            Arbitraries.of(true, false), Arbitraries.of(MessageOrigin.values()),
            instants()
        ).as((uuid, msg, isMeta, isCompact, origin, ts) ->
            new UserMessage(uuid, msg, isMeta, isCompact, null, origin, null, ts));
    }

    @Provide
    Arbitrary<AssistantMessage> assistantMessages() {
        return Combinators.combine(
            uuids(), assistantContents(), Arbitraries.of(true, false), instants()
        ).as((uuid, content, isErr, ts) ->
            new AssistantMessage(uuid, content, isErr, null, ts));
    }

    @Provide
    Arbitrary<SystemMessage> systemMessages() {
        return Combinators.combine(
            uuids(),
            Arbitraries.of("local_command", "compact_boundary", "api_error", "info"),
            Arbitraries.of("info", "warning", "error"),
            nonEmptyStrings(),
            instants()
        ).as((uuid, subtype, level, content, ts) ->
            new SystemMessage(uuid, subtype, level, content, null, ts));
    }

    @Provide
    Arbitrary<ProgressMessage> progressMessages() {
        return Combinators.combine(uuids(), nonEmptyStrings(), instants())
            .as((uuid, content, ts) -> new ProgressMessage(uuid, content, null, ts));
    }

    @Provide
    Arbitrary<AttachmentMessage> attachmentMessages() {
        return Combinators.combine(uuids(), nonEmptyStrings(), instants())
            .as((uuid, content, ts) -> new AttachmentMessage(uuid, content, null, ts));
    }

    @Provide
    Arbitrary<HookResultMessage> hookResultMessages() {
        return Combinators.combine(uuids(), nonEmptyStrings(), nonEmptyStrings(), instants())
            .as((uuid, hookName, content, ts) -> new HookResultMessage(uuid, hookName, content, null, ts));
    }

    @Provide
    Arbitrary<ToolUseSummaryMessage> toolUseSummaryMessages() {
        return Combinators.combine(uuids(), nonEmptyStrings(), nonEmptyStrings(), nonEmptyStrings(), instants())
            .as((uuid, toolName, toolUseId, summary, ts) ->
                new ToolUseSummaryMessage(uuid, toolName, toolUseId, summary, null, ts));
    }

    @Provide
    Arbitrary<TombstoneMessage> tombstoneMessages() {
        return Combinators.combine(uuids(), uuids(), instants())
            .as((uuid, replacedUuid, ts) -> new TombstoneMessage(uuid, replacedUuid, null, ts));
    }

    @Provide
    Arbitrary<GroupedToolUseMessage> groupedToolUseMessages() {
        return Combinators.combine(
            uuids(),
            nonEmptyStrings().list().ofMinSize(1).ofMaxSize(3),
            nonEmptyStrings().list().ofMinSize(1).ofMaxSize(3),
            instants()
        ).as((uuid, ids, names, ts) -> new GroupedToolUseMessage(uuid, ids, names, null, ts));
    }

    @Provide
    Arbitrary<Message> allMessages() {
        return Arbitraries.oneOf(
            userMessages().map(m -> (Message) m),
            assistantMessages().map(m -> (Message) m),
            systemMessages().map(m -> (Message) m),
            progressMessages().map(m -> (Message) m),
            attachmentMessages().map(m -> (Message) m),
            hookResultMessages().map(m -> (Message) m),
            toolUseSummaryMessages().map(m -> (Message) m),
            tombstoneMessages().map(m -> (Message) m),
            groupedToolUseMessages().map(m -> (Message) m)
        );
    }

    // ========== Properties ==========

    /**
     * CP-4: Serialization roundtrip preserves all fields for all ContentBlock subtypes.
     * <p>
     * Validates: Requirements CP-4
     */
    @Property(tries = 200)
    void contentBlockRoundtripPreservesAllFields(@ForAll("contentBlocks") ContentBlock original) throws Exception {
        String json = MAPPER.writeValueAsString(original);
        ContentBlock deserialized = MAPPER.readValue(json, ContentBlock.class);

        assertNotNull(deserialized);
        assertEquals(original.getClass(), deserialized.getClass());

        switch (original) {
            case TextBlock tb -> {
                TextBlock restored = (TextBlock) deserialized;
                assertEquals(tb.text(), restored.text());
            }
            case ToolUseBlock tub -> {
                ToolUseBlock restored = (ToolUseBlock) deserialized;
                assertEquals(tub.id(), restored.id());
                assertEquals(tub.name(), restored.name());
                assertEquals(tub.input(), restored.input());
            }
            case ToolResultBlock trb -> {
                ToolResultBlock restored = (ToolResultBlock) deserialized;
                assertEquals(trb.toolUseId(), restored.toolUseId());
                assertEquals(trb.isError(), restored.isError());
                assertEquals(trb.content().size(), restored.content().size());
            }
            case ThinkingBlock thb -> {
                ThinkingBlock restored = (ThinkingBlock) deserialized;
                assertEquals(thb.thinking(), restored.thinking());
            }
            case ImageBlock ib -> {
                ImageBlock restored = (ImageBlock) deserialized;
                assertEquals(ib.source(), restored.source());
            }
        }
    }

    /**
     * CP-4: Serialization roundtrip preserves all fields for all Message subtypes.
     * <p>
     * Validates: Requirements CP-4
     */
    @Property(tries = 200)
    void messageRoundtripPreservesAllFields(@ForAll("allMessages") Message original) throws Exception {
        String json = MAPPER.writeValueAsString(original);
        Message deserialized = MAPPER.readValue(json, Message.class);

        assertNotNull(deserialized);
        assertEquals(original.getClass(), deserialized.getClass());
        assertEquals(original.uuid(), deserialized.uuid());
        assertEquals(original.type(), deserialized.type());
    }

    /**
     * CP-4: All Message subtypes produce valid JSON with a "type" discriminator field.
     * <p>
     * Validates: Requirements CP-4
     */
    @Property(tries = 200)
    void allMessageSubtypesHaveTypeDiscriminator(@ForAll("allMessages") Message original) throws Exception {
        String json = MAPPER.writeValueAsString(original);
        JsonNode tree = MAPPER.readTree(json);

        assertTrue(tree.has("type"), "Serialized message must have 'type' field");
        assertEquals(original.type(), tree.get("type").asText());
    }

    /**
     * CP-4: All ContentBlock subtypes produce valid JSON with a "type" discriminator field.
     * <p>
     * Validates: Requirements CP-4
     */
    @Property(tries = 200)
    void allContentBlockSubtypesHaveTypeDiscriminator(@ForAll("contentBlocks") ContentBlock original) throws Exception {
        String json = MAPPER.writeValueAsString(original);
        JsonNode tree = MAPPER.readTree(json);

        assertTrue(tree.has("type"), "Serialized content block must have 'type' field");
        String expectedType = switch (original) {
            case TextBlock ignored -> "text";
            case ToolUseBlock ignored -> "tool_use";
            case ToolResultBlock ignored -> "tool_result";
            case ThinkingBlock ignored -> "thinking";
            case ImageBlock ignored -> "image";
        };
        assertEquals(expectedType, tree.get("type").asText());
    }
}
