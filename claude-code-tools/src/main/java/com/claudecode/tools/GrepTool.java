package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Tool for searching file contents using ripgrep (rg) or Java regex fallback.
 * Task 58.1 enhancements:
 * - Multi output mode (CONTENT / FILES_WITH_MATCHES / COUNT)
 * - Context lines (-A/-B/-C)
 * - Multi-line mode (--multiline-dotall)
 * - VCS exclusion (.git/.svn/.hg)
 * - Ignore patterns
 * - Result sorting by mtime
 * - Path relativization
 */
public class GrepTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();
    private static final int MAX_RESULTS = 200;

    // Task 58.1: VCS directories to exclude
    private static final Set<String> VCS_DIRS = Set.of(".git", ".svn", ".hg", ".bzr");

    @Override
    public String name() {
        return "Grep";
    }

    @Override
    public String description() {
        return "Search file contents using regex patterns. Supports ripgrep if available, with Java regex fallback.";
    }

    @Override
    public JsonNode inputSchema() {
        return SCHEMA;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        return PermissionDecision.ALLOW;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String pattern = input.has("pattern") ? input.get("pattern").asText("") : "";
        String include = input.has("include") ? input.get("include").asText("") : "";
        String exclude = input.has("exclude") ? input.get("exclude").asText("") : "";
        boolean caseSensitive = input.has("case_sensitive") && input.get("case_sensitive").asBoolean(false);
        // Task 58.1: New parameters
        String outputMode = input.has("output_mode") ? input.get("output_mode").asText("content") : "content";
        int contextBefore = input.has("before_context") ? input.get("before_context").asInt(0) : 0;
        int contextAfter = input.has("after_context") ? input.get("after_context").asInt(0) : 0;
        boolean multiLine = input.has("multiline") && input.get("multiline").asBoolean(false);
        boolean sortByMtime = input.has("sort_by_mtime") && input.get("sort_by_mtime").asBoolean(false);

        if (pattern.isBlank()) {
            return "Error: pattern is required";
        }

        Path cwd = Path.of(context.workingDirectory());

        // Try ripgrep first, fall back to Java regex
        if (isRipgrepAvailable()) {
            return executeRipgrep(pattern, include, exclude, caseSensitive, outputMode,
                contextBefore, contextAfter, multiLine, cwd);
        } else {
            return executeJavaGrep(pattern, include, exclude, caseSensitive, outputMode,
                contextBefore, contextAfter, multiLine, sortByMtime, cwd);
        }
    }

    private String executeRipgrep(String pattern, String include, String exclude,
                                   boolean caseSensitive, String outputMode,
                                   int contextBefore, int contextAfter,
                                   boolean multiLine, Path cwd) {
        List<String> args = new ArrayList<>();
        args.add("rg");
        args.add("--line-number");
        args.add("--no-heading");
        args.add("--color=never");
        args.add("--max-count=" + MAX_RESULTS);

        if (!caseSensitive) {
            args.add("--ignore-case");
        }
        if (!include.isBlank()) {
            args.add("--glob=" + include);
        }
        if (!exclude.isBlank()) {
            args.add("--glob=!" + exclude);
        }
        // Task 58.1: Context lines
        if (contextBefore > 0) args.add("-B" + contextBefore);
        if (contextAfter > 0) args.add("-A" + contextAfter);
        if (contextBefore > 0 && contextAfter > 0) args.add("-C" + Math.max(contextBefore, contextAfter));
        // Task 58.1: Multi-line mode
        if (multiLine) args.add("--multiline-dotall");
        // Task 58.1: Output mode
        if ("files_with_matches".equals(outputMode)) args.add("--files-with-matches");
        else if ("count".equals(outputMode)) args.add("--count");

        args.add(pattern);

        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(cwd.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines()
                        .limit(MAX_RESULTS)
                        .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
            }

            process.waitFor(30, TimeUnit.SECONDS);

            if (output.isBlank()) {
                return "No matches found";
            }
            return output;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return "Error running ripgrep: " + e.getMessage();
        }
    }

    private String executeJavaGrep(String pattern, String include, String exclude,
                                    boolean caseSensitive, String outputMode,
                                    int contextBefore, int contextAfter,
                                    boolean multiLine, boolean sortByMtime, Path cwd) {
        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            if (multiLine) {
                flags |= Pattern.DOTALL;
            }
            Pattern regex = Pattern.compile(pattern, flags);

            PathMatcher includeMatcher = include.isBlank() ? null :
                    FileSystems.getDefault().getPathMatcher("glob:" + include);
            PathMatcher excludeMatcher = exclude.isBlank() ? null :
                    FileSystems.getDefault().getPathMatcher("glob:" + exclude);

            List<GrepResult> results = new ArrayList<>();

            Files.walkFileTree(cwd, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Task 58.1: VCS directory exclusion
                    String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (!dir.equals(cwd) && VCS_DIRS.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (results.size() >= MAX_RESULTS) return FileVisitResult.TERMINATE;
                    if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;

                    Path relative = cwd.relativize(file);
                    if (includeMatcher != null && !includeMatcher.matches(relative)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (excludeMatcher != null && excludeMatcher.matches(relative)) {
                        return FileVisitResult.CONTINUE;
                    }

                    try {
                        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                        if ("files_with_matches".equals(outputMode)) {
                            for (int i = 0; i < lines.size(); i++) {
                                if (regex.matcher(lines.get(i)).find()) {
                                    results.add(new GrepResult(relative.toString(), 0, "", attrs.lastModifiedTime().toMillis()));
                                    return FileVisitResult.CONTINUE;
                                }
                            }
                        } else if ("count".equals(outputMode)) {
                            int count = 0;
                            for (String line : lines) {
                                if (regex.matcher(line).find()) count++;
                            }
                            if (count > 0) {
                                results.add(new GrepResult(relative.toString(), 0, String.valueOf(count), attrs.lastModifiedTime().toMillis()));
                            }
                        } else {
                            for (int i = 0; i < lines.size() && results.size() < MAX_RESULTS; i++) {
                                if (regex.matcher(lines.get(i)).find()) {
                                    // Task 58.1: Context lines
                                    StringBuilder matchLine = new StringBuilder();
                                    for (int b = Math.max(0, i - contextBefore); b < i; b++) {
                                        matchLine.append(relative).append(":").append(b + 1).append("-").append(lines.get(b)).append("\n");
                                    }
                                    matchLine.append(relative).append(":").append(i + 1).append(":").append(lines.get(i));
                                    for (int a = i + 1; a <= Math.min(lines.size() - 1, i + contextAfter); a++) {
                                        matchLine.append("\n").append(relative).append(":").append(a + 1).append("-").append(lines.get(a));
                                    }
                                    results.add(new GrepResult(relative.toString(), i + 1, matchLine.toString(), attrs.lastModifiedTime().toMillis()));
                                }
                            }
                        }
                    } catch (IOException ignored) {
                        // Skip files that can't be read
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            // Task 58.1: Sort by mtime if requested
            if (sortByMtime) {
                results.sort(Comparator.comparingLong(GrepResult::mtime).reversed());
            }

            if (results.isEmpty()) {
                return "No matches found";
            }

            if ("count".equals(outputMode)) {
                return results.stream()
                    .map(r -> r.relativePath() + ":" + r.content())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("No matches found");
            }

            return results.stream()
                .map(GrepResult::content)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("No matches found");
        } catch (PatternSyntaxException e) {
            return "Error: invalid regex pattern: " + e.getMessage();
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Task 58.1: Grep result record for structured output.
     */
    private record GrepResult(String relativePath, int lineNumber, String content, long mtime) {}

    /**
     * Checks if ripgrep (rg) is available on the system.
     */
    static boolean isRipgrepAvailable() {
        try {
            Process p = new ProcessBuilder("rg", "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean completed = p.waitFor(5, TimeUnit.SECONDS);
            return completed && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode patternProp = properties.putObject("pattern");
        patternProp.put("type", "string");
        patternProp.put("description", "The regex pattern to search for");

        ObjectNode includeProp = properties.putObject("include");
        includeProp.put("type", "string");
        includeProp.put("description", "Glob pattern for files to include");

        ObjectNode excludeProp = properties.putObject("exclude");
        excludeProp.put("type", "string");
        excludeProp.put("description", "Glob pattern for files to exclude");

        ObjectNode caseProp = properties.putObject("case_sensitive");
        caseProp.put("type", "boolean");
        caseProp.put("description", "Whether the search is case sensitive");
        caseProp.put("default", false);

        // Task 58.1: New schema properties
        ObjectNode modeProp = properties.putObject("output_mode");
        modeProp.put("type", "string");
        modeProp.put("enum", mapper().createArrayNode().add("content").add("files_with_matches").add("count"));
        modeProp.put("description", "Output mode: content (default), files_with_matches, or count");

        ObjectNode beforeProp = properties.putObject("before_context");
        beforeProp.put("type", "integer");
        beforeProp.put("minimum", 0);
        beforeProp.put("maximum", 100);
        beforeProp.put("description", "Number of context lines before each match (default: 0)");

        ObjectNode afterProp = properties.putObject("after_context");
        afterProp.put("type", "integer");
        afterProp.put("minimum", 0);
        afterProp.put("maximum", 100);
        afterProp.put("description", "Number of context lines after each match (default: 0)");

        ObjectNode multiProp = properties.putObject("multiline");
        multiProp.put("type", "boolean");
        multiProp.put("description", "Enable multi-line matching (--multiline-dotall)");
        multiProp.put("default", false);

        ObjectNode sortProp = properties.putObject("sort_by_mtime");
        sortProp.put("type", "boolean");
        sortProp.put("description", "Sort results by modification time (newest first)");
        sortProp.put("default", false);

        ArrayNode required = schema.putArray("required");
        required.add("pattern");

        return schema;
    }
}
