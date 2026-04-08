package com.claudecode.services.summary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ToolUseSummaryGenerator — tracks tool invocations and generates summaries.
 * Keeps a list of (toolName, durationMs, timestamp) entries.
 * generateSummary() formats them as a table.
 */
public class ToolUseSummaryGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(ToolUseSummaryGenerator.class);

    private final List<ToolInvocation> invocations = new ArrayList<>();

    /** Record for a single tool invocation. */
    public record ToolInvocation(String toolName, long durationMs, Instant timestamp) {}

    /** Record a tool invocation for summary tracking. */
    public void recordToolUse(String toolName, long durationMs) {
        invocations.add(new ToolInvocation(toolName, durationMs, Instant.now()));
        LOG.debug("Recorded tool use: {} ({}ms)", toolName, durationMs);
    }

    /** Generate a summary of tool usage in the current session. */
    public String generateSummary() {
        if (invocations.isEmpty()) {
            return "No tool invocations recorded.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s %-12s %-25s%n", "Tool", "Duration(ms)", "Timestamp"));
        sb.append("-".repeat(57)).append('\n');

        for (ToolInvocation inv : invocations) {
            sb.append(String.format("%-20s %-12d %-25s%n",
                    inv.toolName(), inv.durationMs(), inv.timestamp()));
        }

        sb.append("-".repeat(57)).append('\n');
        sb.append("Total invocations: ").append(invocations.size());

        long totalMs = invocations.stream().mapToLong(ToolInvocation::durationMs).sum();
        sb.append(" | Total duration: ").append(totalMs).append("ms");

        return sb.toString();
    }

    /** Get the number of recorded invocations. */
    public int getInvocationCount() {
        return invocations.size();
    }

    /** Get all recorded invocations. */
    public List<ToolInvocation> getInvocations() {
        return List.copyOf(invocations);
    }

    /** Clear all recorded invocations. */
    public void clear() {
        invocations.clear();
    }
}
