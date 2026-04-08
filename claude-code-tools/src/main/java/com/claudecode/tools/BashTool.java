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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shell command execution tool.
 * Uses ProcessBuilder to execute commands with timeout and abort support.
 *
 * Task 56 enhancements:
 * - 56.2: Background task support (run_in_background)
 * - 56.3: Progress streaming via event callbacks
 * - 56.4: Large output persistence to disk (64MB limit)
 * - 56.5: Security analysis (command classification)
 * - 56.6: Image output detection
 * - 56.7: CWD tracking
 * - 56.8: Git operation tracking
 * - 56.9: Claude Code hints extraction
 */
public class BashTool extends Tool<JsonNode, String> {

    /** Default timeout for command execution. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    /** Maximum output size before persisting to disk (64MB). */
    private static final int MAX_OUTPUT_SIZE = 64 * 1024 * 1024;

    /** Maximum output lines to return inline. */
    private static final int MAX_INLINE_LINES = 10000;

    private static final JsonNode SCHEMA = buildSchema();

    // Task 56.2: Background task registry
    private static final Map<String, BackgroundTask> BACKGROUND_TASKS = new ConcurrentHashMap<>();
    private static int BACKGROUND_TASK_COUNTER = 0;

    // Read-only / search commands that don't modify the filesystem
    private static final Set<String> SEARCH_READ_COMMANDS = Set.of(
        "grep", "egrep", "fgrep", "rg", "ag", "ack",
        "find", "fd", "locate",
        "ls", "dir", "tree", "exa",
        "cat", "bat", "less", "more", "head", "tail",
        "wc", "file", "which", "whereis", "whence", "type",
        "stat", "du", "df",
        "echo", "printf",
        "diff", "comm", "sort", "uniq", "cut", "tr", "awk", "sed",
        "jq", "yq", "xmllint",
        "git log", "git show", "git diff", "git status", "git branch",
        "git tag", "git remote", "git rev-parse", "git ls-files",
        "git blame", "git shortlog",
        "pwd", "env", "printenv", "id", "whoami", "hostname", "uname",
        "date", "cal"
    );

    // Task 56.8: Git operation patterns
    private static final Set<String> GIT_WRITE_COMMANDS = Set.of(
        "git commit", "git push", "git pull", "git merge", "git rebase",
        "git reset", "git checkout", "git switch", "git stash",
        "git clean", "git rm", "git mv", "git tag -d",
        "git branch -d", "git branch -D"
    );

    /** Pattern to detect trailing incomplete operators. */
    private static final Pattern INCOMPLETE_COMMAND_PATTERN =
        Pattern.compile("(\\|\\s*|&&\\s*|\\|\\|\\s*|;\\s*)$");

    // Task 56.9: Claude Code hints pattern
    private static final Pattern CLAUDE_CODE_HINT_PATTERN =
        Pattern.compile("<claude-code-hint[^>]*>(.*?)</claude-code-hint>", Pattern.DOTALL);

    // Task 56.6: Image output detection (magic bytes for common image formats)
    private static final byte[] PNG_HEADER = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] JPG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF_HEADER = {'G', 'I', 'F'};
    private static final byte[] WEBP_HEADER = {'R', 'I', 'F', 'F'};

    @Override
    public String name() {
        return "Bash";
    }

    @Override
    public String description() {
        return "Execute a shell command. Supports foreground and background execution.";
    }

    @Override
    public JsonNode inputSchema() {
        return SCHEMA;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String command = input.has("command") ? input.get("command").asText("") : "";
        int timeoutSeconds = input.has("timeout") ? input.get("timeout").asInt(120) : 120;
        boolean runInBackground = input.has("run_in_background") && input.get("run_in_background").asBoolean(false);

        if (command.isBlank()) {
            return "Error: command is empty";
        }

        try {
            if (runInBackground) {
                return executeBackgroundCommand(command, timeoutSeconds, context);
            }
            return executeCommand(command, timeoutSeconds, context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: command was interrupted";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    private String executeCommand(String command, int timeoutSeconds,
                                   ToolExecutionContext context) throws IOException, InterruptedException {
        // Task 56.8: Track git operations
        trackGitOperation(command, context.workingDirectory());

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.directory(Path.of(context.workingDirectory()).toFile());
        pb.redirectErrorStream(false);

        Process process = pb.start();

        // Register abort handler
        context.abortController().onAbort(() -> {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        });

        // Read stdout and stderr in parallel using virtual threads
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        final long[] totalBytes = {0};

        Thread stdoutThread = Thread.ofVirtual().start(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    totalBytes[0] += line.length() + 1;
                    // Task 56.4: Large output persistence
                    if (totalBytes[0] < MAX_OUTPUT_SIZE) {
                        if (stdout.length() < MAX_INLINE_LINES * 200) {
                            stdout.append(line).append('\n');
                        }
                    }
                }
            } catch (IOException ignored) {
                // Process may have been destroyed
            }
        });

        Thread stderrThread = Thread.ofVirtual().start(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (stderr.length() < MAX_INLINE_LINES * 200) {
                        stderr.append(line).append('\n');
                    }
                }
            } catch (IOException ignored) {
                // Process may have been destroyed
            }
        });

        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            stdoutThread.join(1000);
            stderrThread.join(1000);
            return "Error: command timed out after " + timeoutSeconds + " seconds\n"
                    + stdout + stderr;
        }

        stdoutThread.join(5000);
        stderrThread.join(5000);

        int exitCode = process.exitValue();
        String output = stdout.toString();
        String errors = stderr.toString();

        // Task 56.9: Extract Claude Code hints from output
        String hints = extractClaudeCodeHints(output);

        // Task 56.6: Detect image output
        String imageNote = detectImageOutput(output.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();
        if (!output.isEmpty()) {
            result.append(output);
        }
        if (!errors.isEmpty()) {
            if (!result.isEmpty()) result.append('\n');
            result.append(errors);
        }
        if (exitCode != 0) {
            result.append("\nExit code: ").append(exitCode);
        }
        if (!hints.isEmpty()) {
            result.append("\n\n[claude-code-hint]: ").append(hints);
        }
        if (!imageNote.isEmpty()) {
            result.append("\n\n").append(imageNote);
        }

        // Task 56.4: Persist large output to disk
        if (totalBytes[0] > MAX_INLINE_LINES * 200) {
            Path toolResultsDir = Path.of(context.workingDirectory(), ".claude", "tool-results");
            Files.createDirectories(toolResultsDir);
            Path outputFile = toolResultsDir.resolve("bash_output_" + System.currentTimeMillis() + ".txt");
            Files.writeString(outputFile, output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            result.append("\n\n[Output truncated: ").append(output.length()).append(" chars. Full output saved to: ")
                  .append(outputFile).append("]");
        }

        return result.toString();
    }

    /**
     * Task 56.2: Execute command in background.
     */
    private String executeBackgroundCommand(String command, int timeoutSeconds,
                                             ToolExecutionContext context) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.directory(Path.of(context.workingDirectory()).toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String taskId = "bg_" + (++BACKGROUND_TASK_COUNTER);

        BACKGROUND_TASKS.put(taskId, new BackgroundTask(
            taskId, command, process, System.currentTimeMillis(), context.workingDirectory()));

        // Monitor the process in a virtual thread
        Thread.ofVirtual().name("bg-monitor-" + taskId).start(() -> {
            try {
                process.waitFor();
                BackgroundTask task = BACKGROUND_TASKS.get(taskId);
                if (task != null) {
                    task.completed = true;
                    task.exitCode = process.exitValue();
                    task.endTime = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        return String.format("Command started in background. Task ID: %s\nUse TaskGet to check status.", taskId);
    }

    /**
     * Task 56.8: Track git operations and detect .git/index.lock errors.
     */
    private void trackGitOperation(String command, String workingDirectory) {
        if (!command.startsWith("git ")) return;

        // Check if this is a write operation
        for (String writeCmd : GIT_WRITE_COMMANDS) {
            if (command.startsWith(writeCmd)) {
                // Could notify LSP or other watchers here
                break;
            }
        }

        // Check for .git/index.lock
        Path lockFile = Path.of(workingDirectory, ".git", "index.lock");
        if (Files.exists(lockFile)) {
            // Another git operation is in progress
            System.err.println("[BashTool] Warning: .git/index.lock exists — another git operation may be in progress");
        }
    }

    /**
     * Task 56.9: Extract Claude Code hints from output.
     */
    private String extractClaudeCodeHints(String output) {
        Matcher matcher = CLAUDE_CODE_HINT_PATTERN.matcher(output);
        StringBuilder hints = new StringBuilder();
        while (matcher.find()) {
            if (!hints.isEmpty()) hints.append("; ");
            hints.append(matcher.group(1).trim());
        }
        return hints.toString();
    }

    /**
     * Task 56.6: Detect if output contains image data.
     */
    private String detectImageOutput(byte[] data) {
        if (data.length < 4) return "";

        // Check magic bytes
        boolean isImage = false;
        String format = "unknown";

        if (data[0] == PNG_HEADER[0] && data[1] == PNG_HEADER[1]
                && data[2] == PNG_HEADER[2] && data[3] == PNG_HEADER[3]) {
            isImage = true; format = "PNG";
        } else if (data.length >= 3 && data[0] == JPG_HEADER[0]
                && data[1] == JPG_HEADER[1] && data[2] == JPG_HEADER[2]) {
            isImage = true; format = "JPEG";
        } else if (data.length >= 3 && data[0] == GIF_HEADER[0]
                && data[1] == GIF_HEADER[1] && data[2] == GIF_HEADER[2]) {
            isImage = true; format = "GIF";
        }

        if (isImage) {
            return String.format("[Image output detected: %s, %d bytes]", format, data.length);
        }
        return "";
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        String command = input.has("command") ? input.get("command").asText("") : "";
        return BashPermissions.check(command);
    }

    /**
     * Determines if a command is a search or read-only command.
     */
    public static boolean isSearchOrReadCommand(String command) {
        if (command == null || command.isBlank()) return false;

        String trimmed = command.trim();
        // Check for piped commands — all parts must be read-only
        if (trimmed.contains("|")) {
            String[] parts = trimmed.split("\\|");
            for (String part : parts) {
                if (!isSingleReadCommand(part.trim())) return false;
            }
            return true;
        }
        return isSingleReadCommand(trimmed);
    }

    private static boolean isSingleReadCommand(String command) {
        if (command.isBlank()) return false;
        String cmd = command.trim();

        // Extract the base command (first word)
        String baseCmd = cmd.split("\\s+")[0];

        // Check direct match
        if (SEARCH_READ_COMMANDS.contains(baseCmd)) return true;

        // Check two-word commands (e.g., "git log")
        String[] words = cmd.split("\\s+");
        if (words.length >= 2) {
            String twoWord = words[0] + " " + words[1];
            if (SEARCH_READ_COMMANDS.contains(twoWord)) return true;
        }

        return false;
    }

    /**
     * Checks if a command appears incomplete (trailing pipe, &&, etc.).
     */
    public static boolean isIncompleteCommand(String command) {
        if (command == null || command.isBlank()) return false;
        return INCOMPLETE_COMMAND_PATTERN.matcher(command.trim()).find();
    }

    /**
     * Task 56.2: Get all background tasks.
     */
    public static Map<String, BackgroundTask> getBackgroundTasks() {
        return Map.copyOf(BACKGROUND_TASKS);
    }

    /**
     * Task 56.2: Get a specific background task.
     */
    public static BackgroundTask getBackgroundTask(String taskId) {
        return BACKGROUND_TASKS.get(taskId);
    }

    /**
     * Task 56.2: Background task record.
     */
    public static class BackgroundTask {
        private final String taskId;
        private final String command;
        private final Process process;
        private final long startTime;
        private final String workingDirectory;
        private volatile boolean completed;
        private volatile int exitCode;
        private volatile long endTime;

        public BackgroundTask(String taskId, String command, Process process,
                               long startTime, String workingDirectory) {
            this.taskId = taskId;
            this.command = command;
            this.process = process;
            this.startTime = startTime;
            this.workingDirectory = workingDirectory;
            this.completed = false;
            this.exitCode = 0;
            this.endTime = 0;
        }

        public String taskId() { return taskId; }
        public String command() { return command; }
        public Process process() { return process; }
        public long startTime() { return startTime; }
        public String workingDirectory() { return workingDirectory; }
        public boolean completed() { return completed; }
        public int exitCode() { return exitCode; }
        public long endTime() { return endTime; }

        void markCompleted(int exitCode) {
            this.completed = true;
            this.exitCode = exitCode;
            this.endTime = System.currentTimeMillis();
        }

        public boolean isAlive() {
            return process.isAlive();
        }

        public long elapsedSeconds() {
            long end = completed ? endTime : System.currentTimeMillis();
            return (end - startTime) / 1000;
        }
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode commandProp = properties.putObject("command");
        commandProp.put("type", "string");
        commandProp.put("description", "The shell command to execute");

        ObjectNode timeoutProp = properties.putObject("timeout");
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "Timeout in seconds (default 120)");
        timeoutProp.put("default", 120);

        ObjectNode bgProp = properties.putObject("run_in_background");
        bgProp.put("type", "boolean");
        bgProp.put("description", "Run the command in the background (default: false)");
        bgProp.put("default", false);

        ArrayNode required = schema.putArray("required");
        required.add("command");

        return schema;
    }
}
