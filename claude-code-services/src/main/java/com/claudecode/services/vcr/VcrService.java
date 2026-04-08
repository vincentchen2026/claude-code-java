package com.claudecode.services.vcr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * VcrService — record session messages to a file and replay them.
 * startRecording() opens a JSONL file, record() appends messages,
 * replay() reads and returns them.
 */
public class VcrService {

    private static final Logger LOG = LoggerFactory.getLogger(VcrService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private boolean recording = false;
    private Path recordingDir;
    private String currentSessionId;
    private BufferedWriter writer;

    public VcrService() {
        this(Path.of(System.getProperty("user.home"), ".claude", "vcr"));
    }

    public VcrService(Path recordingDir) {
        this.recordingDir = recordingDir;
    }

    /** Start recording the session. */
    public void startRecording() {
        startRecording("session-" + System.currentTimeMillis());
    }

    /** Start recording with a specific session ID. */
    public void startRecording(String sessionId) {
        this.currentSessionId = sessionId;
        this.recording = true;

        try {
            Files.createDirectories(recordingDir);
            Path file = recordingDir.resolve(sessionId + ".jsonl");
            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOG.info("VCR recording started: {}", sessionId);
        } catch (IOException e) {
            LOG.error("Failed to start VCR recording", e);
            recording = false;
        }
    }

    /** Record a message. */
    public void record(Object message) {
        if (!recording || writer == null) return;

        try {
            String json = MAPPER.writeValueAsString(message);
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            LOG.error("Failed to record message", e);
        }
    }

    /** Stop recording. */
    public void stopRecording() {
        recording = false;
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                LOG.error("Failed to close VCR writer", e);
            }
            writer = null;
        }
        LOG.info("VCR recording stopped: {}", currentSessionId);
    }

    /** Check if currently recording. */
    public boolean isRecording() {
        return recording;
    }

    /** Replay a recorded session. Returns the JSONL content. */
    public String replay(String sessionId) {
        Path file = recordingDir.resolve(sessionId + ".jsonl");
        if (!Files.exists(file)) {
            return "No recording found for session: " + sessionId;
        }

        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Error replaying session: " + e.getMessage();
        }
    }

    /** Replay a recorded session as a list of JSON strings. */
    public List<String> replayMessages(String sessionId) {
        Path file = recordingDir.resolve(sessionId + ".jsonl");
        if (!Files.exists(file)) {
            return List.of();
        }

        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.filter(l -> !l.isBlank()).toList();
        } catch (IOException e) {
            LOG.error("Error replaying session {}", sessionId, e);
            return List.of();
        }
    }

    /** Get the current session ID. */
    public String getCurrentSessionId() {
        return currentSessionId;
    }
}
