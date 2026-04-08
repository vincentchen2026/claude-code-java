package com.claudecode.services.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/**
 * KAIROS daily append-only log.
 * Stores entries in a file per day under `.claude/kairos/YYYY-MM-DD.log`.
 * addEntry() appends, getEntries() reads.
 */
public class KairosLog {

    private static final Logger LOG = LoggerFactory.getLogger(KairosLog.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Path baseDir;

    public KairosLog() {
        this(Path.of(System.getProperty("user.home"), ".claude", "kairos"));
    }

    public KairosLog(Path baseDir) {
        this.baseDir = baseDir;
    }

    /** Add an entry to today's log. */
    public void addEntry(String entry) {
        addEntry(LocalDate.now(), entry);
    }

    /** Add an entry to a specific date's log. */
    public void addEntry(LocalDate date, String entry) {
        Path logFile = getLogFile(date);
        try {
            Files.createDirectories(logFile.getParent());
            String line = "[" + Instant.now() + "] " + entry + "\n";
            Files.writeString(logFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOG.debug("KAIROS entry added for {}: {}", date, entry);
        } catch (IOException e) {
            LOG.error("Failed to write KAIROS log entry", e);
        }
    }

    /** Get entries for a specific date. */
    public List<String> getEntries(LocalDate date) {
        Path logFile = getLogFile(date);
        if (!Files.exists(logFile)) {
            return List.of();
        }

        try (Stream<String> lines = Files.lines(logFile, StandardCharsets.UTF_8)) {
            return lines.filter(l -> !l.isBlank()).toList();
        } catch (IOException e) {
            LOG.error("Failed to read KAIROS log for {}", date, e);
            return List.of();
        }
    }

    /** Get today's log summary. */
    public String getTodaySummary() {
        List<String> entries = getEntries(LocalDate.now());
        if (entries.isEmpty()) {
            return "No KAIROS entries for today.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("KAIROS log for ").append(LocalDate.now()).append(":\n");
        for (String entry : entries) {
            sb.append("  ").append(entry).append('\n');
        }
        return sb.toString();
    }

    private Path getLogFile(LocalDate date) {
        return baseDir.resolve(date.format(DATE_FMT) + ".log");
    }
}
