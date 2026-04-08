package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

/**
 * Tool for finding files matching glob patterns.
 * Task 58.2 enhancements:
 * - Configurable max results
 * - Truncation report (truncated boolean in output)
 * - Hidden file support (include_hidden parameter)
 * - Permission-aware glob (skip unreadable dirs)
 * - Performance metrics (Duration timing)
 * - VCS directory exclusion
 */
public class GlobTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();
    private static final int DEFAULT_MAX_RESULTS = 500;

    // VCS directories to skip
    private static final Set<String> VCS_DIRS = Set.of(".git", ".svn", ".hg", ".bzr");

    @Override
    public String name() {
        return "Glob";
    }

    @Override
    public String description() {
        return "Find files matching a glob pattern. Supports hidden files, exclusions, and performance metrics.";
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
        String exclude = input.has("exclude") ? input.get("exclude").asText("") : "";
        // Task 58.2: New parameters
        int maxResults = input.has("max_results") ? input.get("max_results").asInt(DEFAULT_MAX_RESULTS) : DEFAULT_MAX_RESULTS;
        boolean includeHidden = input.has("include_hidden") && input.get("include_hidden").asBoolean(false);

        if (pattern.isBlank()) {
            return "Error: pattern is required";
        }

        Path cwd = Path.of(context.workingDirectory());
        long startTime = System.nanoTime();

        try {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            PathMatcher excludeMatcher = exclude.isBlank() ? null :
                    FileSystems.getDefault().getPathMatcher("glob:" + exclude);

            List<String> matches = new ArrayList<>();
            int[] dirsScanned = {0};
            int[] filesScanned = {0};
            boolean[] truncated = {false};

            Files.walkFileTree(cwd, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (matches.size() >= maxResults) return FileVisitResult.TERMINATE;
                    // Task 58.2: Permission-aware — skip unreadable dirs
                    if (!Files.isReadable(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    // VCS exclusion
                    if (!dir.equals(cwd) && VCS_DIRS.contains(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    // Hidden directory handling
                    if (!includeHidden && !dir.equals(cwd) && dirName.startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    dirsScanned[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matches.size() >= maxResults) {
                        truncated[0] = true;
                        return FileVisitResult.TERMINATE;
                    }
                    if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;

                    // Task 58.2: Permission-aware — skip unreadable files
                    if (!Files.isReadable(file)) {
                        return FileVisitResult.CONTINUE;
                    }

                    filesScanned[0]++;

                    Path relative = cwd.relativize(file);
                    String fileName = file.getFileName().toString();

                    // Hidden file handling
                    if (!includeHidden && fileName.startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (matcher.matches(relative) || matcher.matches(file.getFileName())) {
                        if (excludeMatcher == null || !excludeMatcher.matches(relative)) {
                            matches.add(relative.toString());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // Task 58.2: Permission errors are silently skipped
                    return FileVisitResult.CONTINUE;
                }
            });

            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

            matches.sort(String::compareTo);

            StringBuilder result = new StringBuilder();
            if (matches.isEmpty()) {
                return "No files found matching pattern: " + pattern;
            }

            result.append(String.join("\n", matches));

            // Task 58.2: Truncation report and performance metrics
            if (truncated[0]) {
                result.append("\n\n[Results truncated: ").append(matches.size())
                      .append(" of potentially more matches. Increase max_results to see more.]");
            }
            result.append("\n\n[Performance: ").append(elapsedMs).append("ms, ")
                  .append(dirsScanned[0]).append(" dirs, ")
                  .append(filesScanned[0]).append(" files scanned]");

            return result.toString();
        } catch (PatternSyntaxException e) {
            return "Error: invalid glob pattern: " + e.getMessage();
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode patternProp = properties.putObject("pattern");
        patternProp.put("type", "string");
        patternProp.put("description", "The glob pattern to match files against");

        ObjectNode excludeProp = properties.putObject("exclude");
        excludeProp.put("type", "string");
        excludeProp.put("description", "Glob pattern for files to exclude");

        // Task 58.2: New schema properties
        ObjectNode maxResultsProp = properties.putObject("max_results");
        maxResultsProp.put("type", "integer");
        maxResultsProp.put("minimum", 1);
        maxResultsProp.put("maximum", 10000);
        maxResultsProp.put("description", "Maximum number of results to return (default: 500)");

        ObjectNode hiddenProp = properties.putObject("include_hidden");
        hiddenProp.put("type", "boolean");
        hiddenProp.put("description", "Include hidden files and directories (starting with .)");
        hiddenProp.put("default", false);

        ArrayNode required = schema.putArray("required");
        required.add("pattern");

        return schema;
    }
}
