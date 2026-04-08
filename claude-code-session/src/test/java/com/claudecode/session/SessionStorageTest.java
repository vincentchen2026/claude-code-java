package com.claudecode.session;

import com.claudecode.core.message.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SessionStorage JSONL read/write and SessionObjectMapper configuration.
 */
class SessionStorageTest {

    @TempDir
    Path tempDir;

    private SessionStorage storage;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = SessionObjectMapper.get();
        storage = new SessionStorage(mapper);
    }

    // ---- Roundtrip tests: write → read → verify equality ----

    @Test
    void roundtripUserMessage() {
        Path file = tempDir.resolve("user.jsonl");
        UserMessage msg = new UserMessage(
                UUID.randomUUID().toString(),
                MessageContent.ofText("Hello, world!"),
                false, false, null, MessageOrigin.USER,
                null, Instant.now()
        );

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        assertInstanceOf(UserMessage.class, messages.get(0));
        UserMessage restored = (UserMessage) messages.get(0);
        assertEquals(msg.uuid(), restored.uuid());
        assertEquals("user", restored.type());
        assertTrue(restored.message().isText());
        assertEquals("Hello, world!", restored.message().text());
    }

    @Test
    void roundtripAssistantMessage() {
        Path file = tempDir.resolve("assistant.jsonl");
        AssistantMessage msg = new AssistantMessage(
                UUID.randomUUID().toString(),
                AssistantContent.of("api-msg-1", List.of(new TextBlock("I can help with that."))),
                false, null, Instant.now()
        );

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        assertInstanceOf(AssistantMessage.class, messages.get(0));
        AssistantMessage restored = (AssistantMessage) messages.get(0);
        assertEquals(msg.uuid(), restored.uuid());
        assertEquals("assistant", restored.type());
        assertEquals(1, restored.message().content().size());
        assertInstanceOf(TextBlock.class, restored.message().content().get(0));
        assertEquals("I can help with that.", ((TextBlock) restored.message().content().get(0)).text());
    }

    @Test
    void roundtripSystemMessage() {
        Path file = tempDir.resolve("system.jsonl");
        SystemMessage msg = new SystemMessage(
                UUID.randomUUID().toString(), "local_command", "info", "Command executed"
        );

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        assertInstanceOf(SystemMessage.class, messages.get(0));
        SystemMessage restored = (SystemMessage) messages.get(0);
        assertEquals(msg.uuid(), restored.uuid());
        assertEquals("local_command", restored.subtype());
        assertEquals("info", restored.level());
    }

    @Test
    void roundtripProgressMessage() {
        Path file = tempDir.resolve("progress.jsonl");
        ProgressMessage msg = new ProgressMessage(UUID.randomUUID().toString(), "Working...");

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        assertInstanceOf(ProgressMessage.class, messages.get(0));
        assertEquals("Working...", ((ProgressMessage) messages.get(0)).content());
    }

    @Test
    void roundtripAttachmentMessage() {
        Path file = tempDir.resolve("attachment.jsonl");
        AttachmentMessage msg = new AttachmentMessage(UUID.randomUUID().toString(), "file content here");

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        assertInstanceOf(AttachmentMessage.class, messages.get(0));
        assertEquals("file content here", ((AttachmentMessage) messages.get(0)).content());
    }

    @Test
    void roundtripHookResultMessage() {
        Path file = tempDir.resolve("hook.jsonl");
        HookResultMessage msg = new HookResultMessage(
                UUID.randomUUID().toString(), "pre_tool_use", "hook output"
        );

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        assertInstanceOf(HookResultMessage.class, messages.get(0));
        HookResultMessage restored = (HookResultMessage) messages.get(0);
        assertEquals("pre_tool_use", restored.hookName());
    }

    @Test
    void roundtripToolUseSummaryMessage() {
        Path file = tempDir.resolve("toolsummary.jsonl");
        ToolUseSummaryMessage msg = new ToolUseSummaryMessage(
                UUID.randomUUID().toString(), "Bash", "tu-1", "Ran ls command"
        );

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        assertInstanceOf(ToolUseSummaryMessage.class, messages.get(0));
        ToolUseSummaryMessage restored = (ToolUseSummaryMessage) messages.get(0);
        assertEquals("Bash", restored.toolName());
        assertEquals("tu-1", restored.toolUseId());
    }

    @Test
    void roundtripTombstoneMessage() {
        Path file = tempDir.resolve("tombstone.jsonl");
        TombstoneMessage msg = new TombstoneMessage(UUID.randomUUID().toString(), "replaced-uuid-1");

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        assertInstanceOf(TombstoneMessage.class, messages.get(0));
        assertEquals("replaced-uuid-1", ((TombstoneMessage) messages.get(0)).replacedUuid());
    }

    @Test
    void roundtripGroupedToolUseMessage() {
        Path file = tempDir.resolve("grouped.jsonl");
        GroupedToolUseMessage msg = new GroupedToolUseMessage(
                UUID.randomUUID().toString(),
                List.of("tu-1", "tu-2"),
                List.of("Bash", "FileRead")
        );

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        assertInstanceOf(GroupedToolUseMessage.class, messages.get(0));
        GroupedToolUseMessage restored = (GroupedToolUseMessage) messages.get(0);
        assertEquals(List.of("tu-1", "tu-2"), restored.toolUseIds());
        assertEquals(List.of("Bash", "FileRead"), restored.toolNames());
    }

    // ---- ContentBlock subtype tests ----

    @Test
    void roundtripAssistantWithToolUseBlock() {
        Path file = tempDir.resolve("tooluse.jsonl");
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.put("command", "ls -la");

        AssistantMessage msg = new AssistantMessage(
                UUID.randomUUID().toString(),
                AssistantContent.of("api-1", List.of(
                        new TextBlock("Let me check that."),
                        new ToolUseBlock("tu-123", "Bash", input)
                )),
                false, null, Instant.now()
        );

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        AssistantMessage restored = (AssistantMessage) messages.get(0);
        assertEquals(2, restored.message().content().size());
        assertInstanceOf(TextBlock.class, restored.message().content().get(0));
        assertInstanceOf(ToolUseBlock.class, restored.message().content().get(1));
        ToolUseBlock toolUse = (ToolUseBlock) restored.message().content().get(1);
        assertEquals("tu-123", toolUse.id());
        assertEquals("Bash", toolUse.name());
        assertEquals("ls -la", toolUse.input().get("command").asText());
    }

    @Test
    void roundtripAssistantWithThinkingBlock() {
        Path file = tempDir.resolve("thinking.jsonl");
        AssistantMessage msg = new AssistantMessage(
                UUID.randomUUID().toString(),
                AssistantContent.of(List.of(
                        new ThinkingBlock("Let me think about this..."),
                        new TextBlock("Here's my answer.")
                )),
                false, null, Instant.now()
        );

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        AssistantMessage restored = (AssistantMessage) messages.get(0);
        assertEquals(2, restored.message().content().size());
        assertInstanceOf(ThinkingBlock.class, restored.message().content().get(0));
        assertInstanceOf(TextBlock.class, restored.message().content().get(1));
    }

    @Test
    void roundtripToolResultBlock() {
        Path file = tempDir.resolve("toolresult.jsonl");
        UserMessage msg = new UserMessage(
                UUID.randomUUID().toString(),
                MessageContent.ofToolResult("tu-123",
                        List.of(new TextBlock("file contents here")), false),
                false, false, null, MessageOrigin.TOOL_RESULT,
                null, Instant.now()
        );

        storage.appendMessage(file, msg);
        List<Message> messages = storage.readMessages(file);

        assertEquals(1, messages.size());
        UserMessage restored = (UserMessage) messages.get(0);
        assertFalse(restored.message().isText());
        assertEquals(1, restored.message().blocks().size());
        assertInstanceOf(ToolResultBlock.class, restored.message().blocks().get(0));
        ToolResultBlock result = (ToolResultBlock) restored.message().blocks().get(0);
        assertEquals("tu-123", result.toolUseId());
        assertFalse(result.isError());
    }

    // ---- Multiple messages ----

    @Test
    void roundtripMultipleMessages() {
        Path file = tempDir.resolve("multi.jsonl");
        UserMessage user = new UserMessage(UUID.randomUUID().toString(), MessageContent.ofText("Hi"));
        AssistantMessage assistant = new AssistantMessage(
                UUID.randomUUID().toString(),
                AssistantContent.of(List.of(new TextBlock("Hello!")))
        );
        SystemMessage system = new SystemMessage(
                UUID.randomUUID().toString(), "info", "info", "Session started"
        );

        storage.appendMessage(file, user);
        storage.appendMessage(file, assistant);
        storage.appendMessage(file, system);

        List<Message> messages = storage.readMessages(file);
        assertEquals(3, messages.size());
        assertInstanceOf(UserMessage.class, messages.get(0));
        assertInstanceOf(AssistantMessage.class, messages.get(1));
        assertInstanceOf(SystemMessage.class, messages.get(2));
    }

    // ---- Malformed line handling ----

    @Test
    void malformedLinesAreSkipped() throws IOException {
        Path file = tempDir.resolve("malformed.jsonl");
        UserMessage valid = new UserMessage(UUID.randomUUID().toString(), MessageContent.ofText("Valid"));
        String validJson = mapper.writeValueAsString(valid);

        String content = validJson + "\n"
                + "this is not json\n"
                + "{\"broken\": true\n"  // missing closing brace
                + validJson + "\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);

        List<Message> messages = storage.readMessages(file);
        assertEquals(2, messages.size());
        assertInstanceOf(UserMessage.class, messages.get(0));
        assertInstanceOf(UserMessage.class, messages.get(1));
    }

    @Test
    void emptyLinesAreSkipped() throws IOException {
        Path file = tempDir.resolve("empty-lines.jsonl");
        UserMessage msg = new UserMessage(UUID.randomUUID().toString(), MessageContent.ofText("Test"));
        String json = mapper.writeValueAsString(msg);

        Files.writeString(file, "\n" + json + "\n\n\n" + json + "\n", StandardCharsets.UTF_8);

        List<Message> messages = storage.readMessages(file);
        assertEquals(2, messages.size());
    }

    @Test
    void readFromNonExistentFileReturnsEmptyList() {
        Path file = tempDir.resolve("does-not-exist.jsonl");
        List<Message> messages = storage.readMessages(file);
        assertTrue(messages.isEmpty());
    }

    // ---- JSONL format verification ----

    @Test
    void appendedMessagesAreOnePerLine() throws IOException {
        Path file = tempDir.resolve("format.jsonl");
        storage.appendMessage(file, new UserMessage(UUID.randomUUID().toString(), MessageContent.ofText("A")));
        storage.appendMessage(file, new UserMessage(UUID.randomUUID().toString(), MessageContent.ofText("B")));

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        // Each line should be valid JSON (no embedded newlines)
        for (String line : lines) {
            assertFalse(line.isEmpty());
            assertDoesNotThrow(() -> mapper.readTree(line));
        }
    }

    // ---- ObjectMapper configuration tests ----

    @Test
    void objectMapperSkipsNullFields() throws Exception {
        UserMessage msg = new UserMessage(
                "test-uuid", MessageContent.ofText("hi"),
                false, false, null, MessageOrigin.USER,
                null, null
        );
        String json = mapper.writeValueAsString(msg);
        // null fields like parentUuid and timestamp should not appear
        assertFalse(json.contains("parentUuidValue"));
        assertFalse(json.contains("timestampValue"));
    }

    @Test
    void objectMapperIgnoresUnknownProperties() throws Exception {
        // JSON with an extra unknown field should still deserialize
        String json = "{\"type\":\"system\",\"uuid\":\"u1\",\"subtype\":\"info\","
                + "\"level\":\"info\",\"content\":\"test\",\"unknownField\":\"ignored\"}";
        Message msg = mapper.readValue(json, Message.class);
        assertInstanceOf(SystemMessage.class, msg);
        assertEquals("u1", msg.uuid());
    }
}
