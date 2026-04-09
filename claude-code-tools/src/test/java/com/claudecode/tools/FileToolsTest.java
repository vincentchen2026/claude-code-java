package com.claudecode.tools;

import com.claudecode.core.engine.AbortController;
import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.core.engine.ToolExecutionContext.ProgressSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileToolsTest {

    private final ObjectMapper mapper = new ObjectMapper();
    @TempDir
    Path tempDir;

    private ToolExecutionContext ctx() {
        return new ToolExecutionContext(new AbortController(), "test-session", tempDir.toString(), ProgressSink.NOOP);
    }

    // --- FileReadTool tests ---

    @Test
    void readExistingFile() throws IOException {
        Files.writeString(tempDir.resolve("test.txt"), "hello world", StandardCharsets.UTF_8);

        FileReadTool tool = new FileReadTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "test.txt");

        String result = tool.call(input, ctx());
        assertEquals("hello world", result);
    }

    @Test
    void readNonexistentFile() {
        FileReadTool tool = new FileReadTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "nonexistent.txt");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("not found"));
    }

    @Test
    void readFileWithLineRange() throws IOException {
        Files.writeString(tempDir.resolve("lines.txt"), "line1\nline2\nline3\nline4\nline5");

        FileReadTool tool = new FileReadTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "lines.txt");
        input.put("start_line", 2);
        input.put("end_line", 4);

        String result = tool.call(input, ctx());
        assertEquals("line2\nline3\nline4", result);
    }

    @Test
    void readBinaryFileReturnsError() throws IOException {
        byte[] binary = new byte[]{0x00, 0x01, 0x02, 0x03};
        Files.write(tempDir.resolve("binary.bin"), binary);

        FileReadTool tool = new FileReadTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "binary.bin");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("binary"));
    }

    @Test
    void readToolIsReadOnly() {
        assertTrue(new FileReadTool().isReadOnly());
    }

    @Test
    void readToolIsConcurrencySafe() {
        assertTrue(new FileReadTool().isConcurrencySafe());
    }

    // --- FileWriteTool tests ---

    @Test
    void writeNewFile() {
        FileWriteTool tool = new FileWriteTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "new-file.txt");
        input.put("content", "hello world");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Created"));
        assertTrue(Files.exists(tempDir.resolve("new-file.txt")));
    }

    @Test
    void writeCreatesParentDirectories() {
        FileWriteTool tool = new FileWriteTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "sub/dir/file.txt");
        input.put("content", "nested content");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Created"));
        assertTrue(Files.exists(tempDir.resolve("sub/dir/file.txt")));
    }

    @Test
    void writeOverwritesExistingFile() throws IOException {
        Files.writeString(tempDir.resolve("existing.txt"), "old content");

        FileWriteTool tool = new FileWriteTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "existing.txt");
        input.put("content", "new content");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Updated"));
        assertEquals("new content", Files.readString(tempDir.resolve("existing.txt")));
    }

    // --- FileEditTool tests ---

    @Test
    void editReplacesExactMatch() throws IOException {
        Files.writeString(tempDir.resolve("edit.txt"), "hello world\nfoo bar\nbaz");

        FileEditTool tool = new FileEditTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "edit.txt");
        input.put("old_str", "foo bar");
        input.put("new_str", "replaced");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Edited"));

        String content = Files.readString(tempDir.resolve("edit.txt"));
        assertTrue(content.contains("replaced"));
        assertFalse(content.contains("foo bar"));
    }

    @Test
    void editRejectsNonUniqueMatch() throws IOException {
        Files.writeString(tempDir.resolve("dup.txt"), "hello hello hello");

        FileEditTool tool = new FileEditTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "dup.txt");
        input.put("old_str", "hello");
        input.put("new_str", "world");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("matches"));
    }

    @Test
    void editRejectsNotFound() throws IOException {
        Files.writeString(tempDir.resolve("nf.txt"), "hello world");

        FileEditTool tool = new FileEditTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "nf.txt");
        input.put("old_str", "nonexistent");
        input.put("new_str", "replacement");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("not found"));
    }

    @Test
    void editRejectsEmptyOldStr() throws IOException {
        Files.writeString(tempDir.resolve("empty.txt"), "content");

        FileEditTool tool = new FileEditTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "empty.txt");
        input.put("old_str", "");
        input.put("new_str", "replacement");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Error"));
    }

    @Test
    void editNonexistentFile() {
        FileEditTool tool = new FileEditTool();
        ObjectNode input = mapper.createObjectNode();
        input.put("file_path", "nonexistent.txt");
        input.put("old_str", "old");
        input.put("new_str", "new");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Error"));
        assertTrue(result.contains("not found"));
    }
}
