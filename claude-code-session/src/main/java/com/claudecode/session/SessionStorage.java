package com.claudecode.session;

import com.claudecode.core.message.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * JSONL-based session storage for messages.
 * <p>
 * Each message is serialized as a single JSON line (newline-delimited JSONL format).
 * Malformed lines are skipped with a warning log during reads.
 */
public class SessionStorage {

    private static final Logger log = LoggerFactory.getLogger(SessionStorage.class);
    private final ObjectMapper mapper;

    public SessionStorage() {
        this(SessionObjectMapper.get());
    }

    public SessionStorage(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Serializes a message to JSON and appends it as one line to the session file.
     * Creates parent directories and the file if they don't exist.
     */
    public void appendMessage(Path sessionFile, Message message) {
        try {
            String json = mapper.writeValueAsString(message);
            Path parent = sessionFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(sessionFile, json + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to append message to " + sessionFile, e);
        }
    }

    /**
     * Reads all lines from the session file, deserializing each as a Message.
     * Malformed lines are skipped and logged as warnings.
     *
     * @return list of successfully deserialized messages; empty list if file doesn't exist
     */
    public List<Message> readMessages(Path sessionFile) {
        if (!Files.exists(sessionFile)) {
            return List.of();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(sessionFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read session file: " + sessionFile, e);
        }

        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                Message msg = mapper.readValue(line, Message.class);
                messages.add(msg);
            } catch (JsonProcessingException e) {
                log.warn("Skipping malformed line {} in {}: {}", i + 1, sessionFile, e.getMessage());
            }
        }
        return messages;
    }
}
