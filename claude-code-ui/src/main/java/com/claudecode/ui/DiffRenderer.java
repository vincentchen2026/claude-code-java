package com.claudecode.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders unified diff output with ANSI colors.
 * Added lines in green with +, removed lines in red with -, context lines normally.
 */
public final class DiffRenderer {

    private DiffRenderer() {}

    /**
     * Render a unified diff string with ANSI colors.
     * Expects standard unified diff format lines.
     */
    public static String renderUnifiedDiff(String diff) {
        if (diff == null || diff.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String line : diff.split("\n", -1)) {
            sb.append(renderDiffLine(line));
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Render a single diff line with appropriate color.
     */
    public static String renderDiffLine(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        if (line.startsWith("+++") || line.startsWith("---")) {
            return Ansi.styled(line, AnsiStyle.BOLD);
        }
        if (line.startsWith("@@")) {
            return Ansi.colored(line, AnsiColor.CYAN);
        }
        if (line.startsWith("+")) {
            return Ansi.colored(line, AnsiColor.GREEN);
        }
        if (line.startsWith("-")) {
            return Ansi.colored(line, AnsiColor.RED);
        }
        // Context line
        return line;
    }

    /**
     * Generate a simple unified diff between two texts.
     * Uses a basic line-by-line comparison with context lines.
     *
     * @param oldText  the original text
     * @param newText  the modified text
     * @param fileName the file name for the diff header
     * @param contextLines number of context lines around changes
     * @return unified diff string
     */
    public static String generateDiff(String oldText, String newText, String fileName, int contextLines) {
        String[] oldLines = oldText.split("\n", -1);
        String[] newLines = newText.split("\n", -1);

        // Simple LCS-based diff
        List<DiffLine> diffLines = computeDiff(oldLines, newLines);

        // Format as unified diff
        StringBuilder sb = new StringBuilder();
        sb.append("--- a/").append(fileName).append("\n");
        sb.append("+++ b/").append(fileName).append("\n");

        // Group changes into hunks with context
        List<Hunk> hunks = groupIntoHunks(diffLines, contextLines);
        for (Hunk hunk : hunks) {
            sb.append(String.format("@@ -%d,%d +%d,%d @@\n",
                    hunk.oldStart, hunk.oldCount, hunk.newStart, hunk.newCount));
            for (DiffLine dl : hunk.lines) {
                switch (dl.type) {
                    case CONTEXT -> sb.append(" ").append(dl.content).append("\n");
                    case ADDED -> sb.append("+").append(dl.content).append("\n");
                    case REMOVED -> sb.append("-").append(dl.content).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Generate a diff and render it with ANSI colors.
     */
    public static String generateAndRenderDiff(String oldText, String newText, String fileName) {
        String diff = generateDiff(oldText, newText, fileName, 3);
        return renderUnifiedDiff(diff);
    }

    // --- Internal diff computation ---

    enum DiffType { CONTEXT, ADDED, REMOVED }

    record DiffLine(DiffType type, String content, int oldLineNum, int newLineNum) {}

    record Hunk(int oldStart, int oldCount, int newStart, int newCount, List<DiffLine> lines) {}

    static List<DiffLine> computeDiff(String[] oldLines, String[] newLines) {
        // Simple O(n*m) LCS for correctness
        int n = oldLines.length;
        int m = newLines.length;
        int[][] lcs = new int[n + 1][m + 1];

        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (oldLines[i].equals(newLines[j])) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }

        List<DiffLine> result = new ArrayList<>();
        int i = 0, j = 0;
        int oldNum = 1, newNum = 1;
        while (i < n || j < m) {
            if (i < n && j < m && oldLines[i].equals(newLines[j])) {
                result.add(new DiffLine(DiffType.CONTEXT, oldLines[i], oldNum++, newNum++));
                i++;
                j++;
            } else if (j < m && (i >= n || lcs[i][j + 1] >= lcs[i + 1][j])) {
                result.add(new DiffLine(DiffType.ADDED, newLines[j], -1, newNum++));
                j++;
            } else if (i < n) {
                result.add(new DiffLine(DiffType.REMOVED, oldLines[i], oldNum++, -1));
                i++;
            }
        }
        return result;
    }

    static List<Hunk> groupIntoHunks(List<DiffLine> diffLines, int contextLines) {
        List<Hunk> hunks = new ArrayList<>();
        if (diffLines.isEmpty()) return hunks;

        // Find change indices
        List<Integer> changeIndices = new ArrayList<>();
        for (int i = 0; i < diffLines.size(); i++) {
            if (diffLines.get(i).type != DiffType.CONTEXT) {
                changeIndices.add(i);
            }
        }
        if (changeIndices.isEmpty()) return hunks;

        // Group changes that are close together
        int hunkStart = Math.max(0, changeIndices.get(0) - contextLines);
        int hunkEnd = Math.min(diffLines.size() - 1, changeIndices.get(0) + contextLines);

        List<int[]> ranges = new ArrayList<>();
        int rangeStart = hunkStart;
        int rangeEnd = hunkEnd;

        for (int ci = 1; ci < changeIndices.size(); ci++) {
            int newStart = Math.max(0, changeIndices.get(ci) - contextLines);
            int newEnd = Math.min(diffLines.size() - 1, changeIndices.get(ci) + contextLines);
            if (newStart <= rangeEnd + 1) {
                rangeEnd = newEnd;
            } else {
                ranges.add(new int[]{rangeStart, rangeEnd});
                rangeStart = newStart;
                rangeEnd = newEnd;
            }
        }
        ranges.add(new int[]{rangeStart, rangeEnd});

        for (int[] range : ranges) {
            List<DiffLine> hunkLines = diffLines.subList(range[0], Math.min(range[1] + 1, diffLines.size()));
            int oldStart = 1, newStart = 1, oldCount = 0, newCount = 0;
            boolean foundFirst = false;
            for (DiffLine dl : hunkLines) {
                if (!foundFirst) {
                    if (dl.oldLineNum > 0) oldStart = dl.oldLineNum;
                    if (dl.newLineNum > 0) newStart = dl.newLineNum;
                    foundFirst = true;
                }
                switch (dl.type) {
                    case CONTEXT -> { oldCount++; newCount++; }
                    case ADDED -> newCount++;
                    case REMOVED -> oldCount++;
                }
            }
            hunks.add(new Hunk(oldStart, oldCount, newStart, newCount, new ArrayList<>(hunkLines)));
        }
        return hunks;
    }
}
