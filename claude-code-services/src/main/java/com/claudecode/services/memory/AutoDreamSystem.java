package com.claudecode.services.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Auto Dream system — dream consolidation.
 * Reads today's KAIROS log, generates a summary, writes to `.claude/dreams/`.
 */
public class AutoDreamSystem {

    private static final Logger LOG = LoggerFactory.getLogger(AutoDreamSystem.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private boolean dreamLockActive = false;
    private final KairosLog kairosLog;
    private final Path dreamsDir;

    public AutoDreamSystem() {
        this(new KairosLog(),
             Path.of(System.getProperty("user.home"), ".claude", "dreams"));
    }

    public AutoDreamSystem(KairosLog kairosLog, Path dreamsDir) {
        this.kairosLog = kairosLog;
        this.dreamsDir = dreamsDir;
    }

    /** Start the dream process. */
    public void startDream() {
        dreamLockActive = true;
        LOG.info("Dream lock activated");
    }

    /** Stop the dream process. */
    public void stopDream() {
        dreamLockActive = false;
        LOG.info("Dream lock deactivated");
    }

    /** Check if dream lock is active. */
    public boolean isDreamLockActive() {
        return dreamLockActive;
    }

    /** Distill session learnings into skills. */
    public String distillSkills() {
        LocalDate today = LocalDate.now();
        List<String> entries = kairosLog.getEntries(today);

        if (entries.isEmpty()) {
            return "No KAIROS entries to distill for " + today;
        }

        // Generate a summary from today's entries
        StringBuilder summary = new StringBuilder();
        summary.append("# Dream Consolidation - ").append(today).append("\n\n");
        summary.append("## Session Learnings\n\n");

        for (String entry : entries) {
            summary.append("- ").append(entry).append('\n');
        }

        summary.append("\n## Distilled Skills\n\n");
        summary.append("Consolidated ").append(entries.size())
               .append(" entries from today's session.\n");

        // Write dream file
        String dreamContent = summary.toString();
        try {
            Files.createDirectories(dreamsDir);
            Path dreamFile = dreamsDir.resolve(today.format(DATE_FMT) + ".md");
            Files.writeString(dreamFile, dreamContent, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LOG.info("Dream written to {}", dreamFile);
        } catch (IOException e) {
            LOG.error("Failed to write dream file", e);
        }

        return dreamContent;
    }
}
