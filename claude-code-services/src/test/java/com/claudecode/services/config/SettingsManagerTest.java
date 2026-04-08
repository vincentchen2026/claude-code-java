package com.claudecode.services.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SettingsManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void readSettingsFromJsoncFile() throws IOException {
        Path file = tempDir.resolve("settings.json");
        Files.writeString(file, """
            {
              // comment
              "model": "sonnet",
              "maxTokens": 4096,
            }""");

        JsonNode node = SettingsManager.readSettings(file);
        assertNotNull(node);
        assertEquals("sonnet", node.get("model").asText());
        assertEquals(4096, node.get("maxTokens").asInt());
    }

    @Test
    void readSettingsReturnsNullForMissingFile() {
        JsonNode node = SettingsManager.readSettings(tempDir.resolve("nonexistent.json"));
        assertNull(node);
    }

    @Test
    void readSettingsReturnsNullForNullPath() {
        assertNull(SettingsManager.readSettings(null));
    }

    @Test
    void writeSettingsCreatesFile() throws IOException {
        Path file = tempDir.resolve("sub/dir/settings.json");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("model", "opus");
        node.put("maxTokens", 8192);

        SettingsManager.writeSettings(file, node);

        assertTrue(Files.exists(file));
        String content = Files.readString(file);
        assertTrue(content.contains("opus"));
        assertTrue(content.contains("8192"));
    }

    @Test
    void writeSettingsOverwritesExisting() throws IOException {
        Path file = tempDir.resolve("settings.json");
        Files.writeString(file, "old content");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("model", "haiku");

        SettingsManager.writeSettings(file, node);

        String content = Files.readString(file);
        assertTrue(content.contains("haiku"));
        assertFalse(content.contains("old content"));
    }

    @Test
    void roundTripReadWrite() throws IOException {
        Path file = tempDir.resolve("settings.json");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode original = mapper.createObjectNode();
        original.put("model", "sonnet");
        original.put("maxTokens", 4096);

        SettingsManager.writeSettings(file, original);
        JsonNode read = SettingsManager.readSettings(file);

        assertNotNull(read);
        assertEquals("sonnet", read.get("model").asText());
        assertEquals(4096, read.get("maxTokens").asInt());
    }
}
