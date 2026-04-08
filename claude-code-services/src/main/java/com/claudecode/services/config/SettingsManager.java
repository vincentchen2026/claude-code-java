package com.claudecode.services.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes settings.json files.
 * Reading supports JSONC (JSON with comments) via {@link JsoncParser}.
 */
public class SettingsManager {

    private static final Logger log = LoggerFactory.getLogger(SettingsManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Reads a settings file, stripping JSONC comments before parsing.
     *
     * @param path path to the settings file
     * @return parsed JsonNode, or null if the file doesn't exist or can't be parsed
     */
    public static JsonNode readSettings(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }
        try {
            String raw = Files.readString(path);
            String json = JsoncParser.parse(raw);
            if (json.isBlank()) {
                return null;
            }
            return MAPPER.readTree(json);
        } catch (IOException e) {
            log.warn("Failed to read settings from {}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * Writes a JsonNode to a settings file (pretty-printed JSON).
     * Creates parent directories if needed.
     *
     * @param path     target file path
     * @param settings the settings to write
     * @throws IOException if writing fails
     */
    public static void writeSettings(Path path, JsonNode settings) throws IOException {
        if (path == null || settings == null) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String json = MAPPER.writeValueAsString(settings);
        Files.writeString(path, json);
    }
}
