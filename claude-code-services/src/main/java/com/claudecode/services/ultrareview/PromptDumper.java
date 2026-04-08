package com.claudecode.services.ultrareview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PromptDumper {

    private static final Logger log = LoggerFactory.getLogger(PromptDumper.class);

    private final ConcurrentLinkedQueue<PromptRecord> records = new ConcurrentLinkedQueue<>();
    private final Path dumpDir;
    private final int maxRecordsInMemory;

    public PromptDumper() {
        this(Path.of(System.getProperty("user.home"), ".claude", "prompt_dumps"));
    }

    public PromptDumper(Path dumpDir) {
        this.dumpDir = dumpDir;
        this.maxRecordsInMemory = 1000;
        ensureDumpDir();
    }

    public void dump(String sessionId, String prompt, Map<String, Object> metadata) {
        PromptRecord record = new PromptRecord(
            sessionId,
            prompt,
            metadata,
            Instant.now(),
            generateDumpFilename(sessionId)
        );
        records.add(record);

        if (records.size() > maxRecordsInMemory) {
            flushOldest();
        }

        log.debug("Dumped prompt for session: {}", sessionId);
    }

    public void dumpToFile(String sessionId, String prompt, Map<String, Object> metadata) throws IOException {
        String filename = generateDumpFilename(sessionId);
        Path filePath = dumpDir.resolve(filename);

        StringBuilder content = new StringBuilder();
        content.append("# Prompt Dump\n");
        content.append("Session: ").append(sessionId).append("\n");
        content.append("Timestamp: ").append(Instant.now()).append("\n");
        content.append("---\n");
        content.append(prompt);
        content.append("\n---\n");
        content.append("# Metadata\n");
        for (Map.Entry<String, Object> e : metadata.entrySet()) {
            content.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }

        Files.writeString(filePath, content.toString());
        log.info("Dumped prompt to file: {}", filePath);
    }

    public List<PromptRecord> getRecentRecords(int limit) {
        return records.stream()
            .skip(Math.max(0, records.size() - limit))
            .toList();
    }

    public void flushOldest() {
        if (!records.isEmpty()) {
            records.poll();
        }
    }

    public void clear() {
        records.clear();
    }

    public int getRecordCount() {
        return records.size();
    }

    private void ensureDumpDir() {
        try {
            Files.createDirectories(dumpDir);
        } catch (IOException e) {
            log.error("Failed to create dump directory: {}", dumpDir, e);
        }
    }

    private String generateDumpFilename(String sessionId) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(Instant.now());
        return "prompt_" + sessionId + "_" + timestamp + ".md";
    }

    public record PromptRecord(
        String sessionId,
        String prompt,
        Map<String, Object> metadata,
        Instant timestamp,
        String filename
    ) {}
}