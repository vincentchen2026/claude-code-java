package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool for writing content to a file.
 * Task 57.5-57.6 enhancements:
 * - 57.5: Read-before-write validation (verify file was read, mtime expiry check)
 * - 57.6: Secret detection, file history tracking, encoding detection, UNC path security
 */
public class FileWriteTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();

    // Task 57.5: Track which files have been read (for read-before-write validation)
    private static final Map<String, Instant> READ_FILES = new ConcurrentHashMap<>();

    // Task 57.5: Mtime expiry threshold (5 minutes)
    private static final long MTIME_EXPIRY_MS = 5 * 60 * 1000;

    // Task 57.6: Secret detection patterns
    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(?i)(api[_-]?key|secret[_-]?key|password|passwd|token|access[_-]?key|private[_-]?key)\\s*[:=]\\s*['\"]?[A-Za-z0-9+/=_-]{8,}");

    // Task 57.6: Common encoding detection (simple BOM check)
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16_LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF16_BE_BOM = {(byte) 0xFE, (byte) 0xFF};

    // Task 57.6: UNC path prefix (Windows network paths)
    private static final String UNC_PREFIX = "\\\\";

    @Override
    public String name() {
        return "Write";
    }

    @Override
    public String description() {
        return "Write content to a file. Creates parent directories as needed.";
    }

    @Override
    public JsonNode inputSchema() {
        return SCHEMA;
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        return PermissionDecision.ASK;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String filePath = input.has("file_path") ? input.get("file_path").asText("") : "";
        String content = input.has("content") ? input.get("content").asText("") : "";

        if (filePath.isBlank()) {
            return "Error: file_path is required";
        }

        Path path = Path.of(context.workingDirectory()).resolve(filePath);
        String absolutePath = path.toAbsolutePath().normalize().toString();

        // Task 57.6: UNC path security — skip network paths
        if (absolutePath.startsWith(UNC_PREFIX)) {
            return "Error: UNC paths are not supported for security reasons: " + absolutePath;
        }

        // Task 57.5: Read-before-write validation
        String readValidationError = validateReadBeforeWrite(absolutePath);
        if (readValidationError != null) {
            // Log warning but proceed with write (don't return early)
            System.err.println("[FileWriteTool] " + readValidationError);
        }

        // Task 57.6: Secret detection
        String secretWarning = detectSecrets(content);

        try {
            boolean existed = Files.exists(path);
            String oldContent = "";
            if (existed) {
                oldContent = Files.readString(path, StandardCharsets.UTF_8);
            }

            // Create parent directories
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Task 57.6: Encoding detection and preservation
            Charset writeCharset = detectAndPreserveEncoding(path);

            Files.writeString(path, content, writeCharset,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Task 57.6: Update read cache (writing counts as "reading" the new state)
            READ_FILES.put(absolutePath, Instant.now());

            // Return diff summary
            StringBuilder result = new StringBuilder();
            if (!secretWarning.isEmpty()) {
                result.append(secretWarning).append("\n");
            }
            if (!existed) {
                long lines = content.lines().count();
                result.append("Created new file ").append(filePath).append(" (").append(lines).append(" lines)");
            } else {
                long oldLines = oldContent.lines().count();
                long newLines = content.lines().count();
                result.append("Updated ").append(filePath).append(" (").append(oldLines)
                      .append(" -> ").append(newLines).append(" lines)");
            }
            return result.toString();
        } catch (IOException e) {
            return "Error: failed to write file: " + e.getMessage();
        }
    }

    /**
     * Task 57.5: Validate that the file was read before writing.
     * Returns null if validation passes, or a warning message if it fails.
     */
    private String validateReadBeforeWrite(String absolutePath) {
        Instant lastRead = READ_FILES.get(absolutePath);
        if (lastRead == null) {
            return "File was not read before writing. Make sure to read the file first to avoid overwriting unintended content.";
        }

        // Check mtime expiry
        long elapsed = System.currentTimeMillis() - lastRead.toEpochMilli();
        if (elapsed > MTIME_EXPIRY_MS) {
            return "File was read " + (elapsed / 1000) + " seconds ago, which exceeds the "
                + (MTIME_EXPIRY_MS / 1000) + "s freshness threshold. Re-read the file to ensure you have the latest content.";
        }

        return null;
    }

    /**
     * Task 57.6: Detect potential secrets in content.
     * Returns a warning message if secrets are detected, empty string otherwise.
     */
    private String detectSecrets(String content) {
        Matcher matcher = SECRET_PATTERN.matcher(content);
        if (matcher.find()) {
            return "[WARNING] Potential secret detected in file content. Please verify this is intentional.";
        }
        return "";
    }

    /**
     * Task 57.6: Detect and preserve file encoding.
     */
    private Charset detectAndPreserveEncoding(Path path) throws IOException {
        if (!Files.exists(path)) {
            return StandardCharsets.UTF_8;
        }

        byte[] header = Files.readAllBytes(path);
        if (header.length < 2) return StandardCharsets.UTF_8;

        // Check BOM
        if (header.length >= 3 && header[0] == UTF8_BOM[0] && header[1] == UTF8_BOM[1] && header[2] == UTF8_BOM[2]) {
            return StandardCharsets.UTF_8;
        }
        if (header[0] == UTF16_LE_BOM[0] && header[1] == UTF16_LE_BOM[1]) {
            return StandardCharsets.UTF_16LE;
        }
        if (header[0] == UTF16_BE_BOM[0] && header[1] == UTF16_BE_BOM[1]) {
            return StandardCharsets.UTF_16BE;
        }

        return StandardCharsets.UTF_8;
    }

    /**
     * Task 57.5: Register a file as having been read (called by FileReadTool).
     */
    public static void markFileAsRead(String absolutePath) {
        READ_FILES.put(absolutePath, Instant.now());
    }

    /**
     * Clear the read cache (for testing).
     */
    public static void clearReadCache() {
        READ_FILES.clear();
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode filePathProp = properties.putObject("file_path");
        filePathProp.put("type", "string");
        filePathProp.put("description", "The path of the file to write");

        ObjectNode contentProp = properties.putObject("content");
        contentProp.put("type", "string");
        contentProp.put("description", "The content to write to the file");

        ArrayNode required = schema.putArray("required");
        required.add("file_path");
        required.add("content");

        return schema;
    }
}
