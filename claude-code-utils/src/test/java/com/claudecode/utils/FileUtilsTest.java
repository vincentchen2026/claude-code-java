package com.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void readFileIfExists_missingFile() {
        Optional<String> result = FileUtils.readFileIfExists(tempDir.resolve("missing.txt"));
        assertTrue(result.isEmpty());
    }

    @Test
    void writeAndReadFile() {
        Path file = tempDir.resolve("test.txt");
        FileUtils.writeFile(file, "hello world");
        assertEquals("hello world", FileUtils.readFile(file));
    }

    @Test
    void writeFile_createsParentDirs() {
        Path file = tempDir.resolve("sub/dir/test.txt");
        FileUtils.writeFile(file, "content");
        assertTrue(Files.exists(file));
        assertEquals("content", FileUtils.readFile(file));
    }

    @Test
    void appendLine_createsAndAppends() {
        Path file = tempDir.resolve("log.jsonl");
        FileUtils.appendLine(file, "{\"a\":1}");
        FileUtils.appendLine(file, "{\"b\":2}");
        List<String> lines = FileUtils.readLines(file);
        assertEquals(2, lines.size());
        assertEquals("{\"a\":1}", lines.get(0));
        assertEquals("{\"b\":2}", lines.get(1));
    }

    @Test
    void readLines_missingFile() {
        List<String> lines = FileUtils.readLines(tempDir.resolve("nope.txt"));
        assertTrue(lines.isEmpty());
    }

    @Test
    void isBinaryFile_textFile() throws Exception {
        Path file = tempDir.resolve("text.txt");
        Files.writeString(file, "just text");
        assertFalse(FileUtils.isBinaryFile(file));
    }

    @Test
    void isBinaryFile_binaryFile() throws Exception {
        Path file = tempDir.resolve("binary.bin");
        Files.write(file, new byte[]{0x00, 0x01, 0x02});
        assertTrue(FileUtils.isBinaryFile(file));
    }
}
