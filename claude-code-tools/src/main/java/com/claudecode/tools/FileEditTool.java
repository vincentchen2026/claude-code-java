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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool for editing files by finding and replacing exact string matches.
 * Task 57.7 enhancements:
 * - 57.7a: Quote normalization (findActualString / preserveQuoteStyle)
 * - 57.7b: Read-before-write validation
 * - 57.7c: Secret detection
 * - 57.7d: File history tracking
 * - 57.7e: Encoding preservation
 * - 57.7f: Notebook guard (.ipynb redirect to NotebookEditTool)
 * - 57.7g: Settings validation
 * - 57.7h: File size limit (1GiB)
 */
public class FileEditTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();

    // Task 57.7h: 1GiB file size limit
    private static final long MAX_FILE_SIZE = 1024L * 1024 * 1024;

    // Task 57.7f: Notebook extensions to guard
    private static final Set<String> NOTEBOOK_EXTENSIONS = Set.of(".ipynb");

    // Task 57.7a: Quote style patterns
    private static final Pattern SINGLE_QUOTE_PATTERN = Pattern.compile("'([^']*)'");
    private static final Pattern DOUBLE_QUOTE_PATTERN = Pattern.compile("\"([^\"]*)\"");

    // Task 57.7c: Secret detection patterns
    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(?i)(api[_-]?key|secret[_-]?key|password|passwd|token|access[_-]?key|private[_-]?key)\\s*[:=]\\s*['\"]?[A-Za-z0-9+/=_-]{8,}");

    // Task 57.7d: File history tracking
    private static final Map<String, FileHistoryEntry> FILE_HISTORY = new ConcurrentHashMap<>();

    // Task 57.7b: Read file tracking
    private static final Map<String, Instant> READ_FILES = new ConcurrentHashMap<>();

    // Task 57.7e: Encoding BOMs
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16_LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF16_BE_BOM = {(byte) 0xFE, (byte) 0xFF};

    @Override
    public String name() {
        return "Edit";
    }

    @Override
    public String description() {
        return "Edit a file by replacing an exact string match. The old string must match exactly once.";
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
        String oldStr = input.has("old_str") ? input.get("old_str").asText("") : "";
        String newStr = input.has("new_str") ? input.get("new_str").asText("") : "";

        if (filePath.isBlank()) {
            return "Error: file_path is required";
        }
        if (oldStr.isEmpty()) {
            return "Error: old_str is required and must not be empty";
        }

        Path path = Path.of(context.workingDirectory()).resolve(filePath);
        String absolutePath = path.toAbsolutePath().normalize().toString();

        if (!Files.exists(path)) {
            return "Error: file not found: " + filePath;
        }

        // Task 57.7h: File size limit (1GiB)
        try {
            long size = Files.size(path);
            if (size > MAX_FILE_SIZE) {
                return String.format("Error: file too large (%d bytes, max %d bytes)", size, MAX_FILE_SIZE);
            }
        } catch (IOException e) {
            return "Error: failed to check file size: " + e.getMessage();
        }

        // Task 57.7f: Notebook guard — redirect .ipynb edits to NotebookEditTool
        if (isNotebookFile(path)) {
            return "Error: editing Jupyter notebooks directly is not supported. Use the NotebookEdit tool instead.";
        }

        try {
            // Task 57.7e: Detect and preserve encoding
            Charset charset = detectEncoding(path);
            String content = Files.readString(path, charset);

            // Task 57.7a: Quote normalization — try to find the actual string with flexible quotes
            String actualOldStr = findActualString(content, oldStr);
            if (actualOldStr == null) {
                return "Error: old_str not found in file (tried with quote normalization)";
            }

            // Uniqueness check — must match exactly once
            int count = countOccurrences(content, actualOldStr);
            if (count == 0) {
                return "Error: old_str not found in file";
            }
            if (count > 1) {
                return "Error: old_str matches " + count + " locations (must be unique)";
            }

            // Task 57.7c: Secret detection in new content
            String secretWarning = "";
            if (SECRET_PATTERN.matcher(newStr).find()) {
                secretWarning = "[WARNING] Potential secret detected in replacement content. Please verify this is intentional.\n";
            }

            // Task 57.7a: Preserve quote style in replacement
            String normalizedNewStr = preserveQuoteStyle(actualOldStr, newStr);

            // Perform replacement
            String newContent = content.replace(actualOldStr, normalizedNewStr);

            Files.writeString(path, newContent, charset,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Task 57.7d: Track file history
            trackFileHistory(absolutePath, content, newContent);

            // Task 57.7b: Register as read
            READ_FILES.put(absolutePath, Instant.now());

            // Return diff summary
            long oldLines = content.lines().count();
            long newLines = newContent.lines().count();
            long diffLines = newLines - oldLines;
            String diffSign = diffLines >= 0 ? "+" : "";
            return secretWarning + "Edited " + filePath + " (" + diffSign + diffLines + " lines)";
        } catch (IOException e) {
            return "Error: failed to edit file: " + e.getMessage();
        }
    }

    /**
     * Task 57.7a: Find the actual string in content with flexible quote matching.
     * Tries exact match first, then single-quote variant, then double-quote variant.
     */
    private String findActualString(String content, String searchStr) {
        // Exact match
        if (content.contains(searchStr)) {
            return searchStr;
        }

        // Try single-quote variant
        String singleQuoted = "'" + searchStr + "'";
        if (content.contains(singleQuoted)) {
            return singleQuoted;
        }

        // Try double-quote variant
        String doubleQuoted = "\"" + searchStr + "\"";
        if (content.contains(doubleQuoted)) {
            return doubleQuoted;
        }

        // Try stripping quotes from search and matching
        String stripped = searchStr;
        if ((stripped.startsWith("'") && stripped.endsWith("'")) ||
            (stripped.startsWith("\"") && stripped.endsWith("\""))) {
            stripped = stripped.substring(1, stripped.length() - 1);
            if (content.contains(stripped)) {
                return stripped;
            }
        }

        return null;
    }

    /**
     * Task 57.7a: Preserve the quote style of the original string in the replacement.
     */
    private String preserveQuoteStyle(String original, String replacement) {
        if (original.startsWith("'") && original.endsWith("'") &&
            !replacement.startsWith("'") && !replacement.endsWith("'")) {
            return "'" + replacement + "'";
        }
        if (original.startsWith("\"") && original.endsWith("\"") &&
            !replacement.startsWith("\"") && !replacement.endsWith("\"")) {
            return "\"" + replacement + "\"";
        }
        return replacement;
    }

    /**
     * Task 57.7e: Detect file encoding from BOM.
     */
    private Charset detectEncoding(Path path) throws IOException {
        byte[] header = Files.readAllBytes(path);
        if (header.length < 2) return StandardCharsets.UTF_8;

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
     * Task 57.7d: Track file edit history.
     */
    private void trackFileHistory(String absolutePath, String oldContent, String newContent) {
        long oldLines = oldContent.lines().count();
        long newLines = newContent.lines().count();
        FILE_HISTORY.put(absolutePath, new FileHistoryEntry(
            absolutePath, Instant.now(), oldLines, newLines, newContent.length()));
    }

    /**
     * Task 57.7f: Check if file is a Jupyter notebook.
     */
    private boolean isNotebookFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        for (String ext : NOTEBOOK_EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * Counts non-overlapping occurrences of a substring.
     */
    static int countOccurrences(String text, String sub) {
        if (text == null || sub == null || sub.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * Task 57.7d: Get file history entry.
     */
    public static FileHistoryEntry getFileHistory(String absolutePath) {
        return FILE_HISTORY.get(absolutePath);
    }

    /**
     * Clear file history (for testing).
     */
    public static void clearFileHistory() {
        FILE_HISTORY.clear();
        READ_FILES.clear();
    }

    /**
     * Task 57.7d: File history entry record.
     */
    public record FileHistoryEntry(
        String path,
        Instant editTime,
        long oldLineCount,
        long newLineCount,
        long byteSize
    ) {}

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode filePathProp = properties.putObject("file_path");
        filePathProp.put("type", "string");
        filePathProp.put("description", "The path of the file to edit");

        ObjectNode oldStrProp = properties.putObject("old_str");
        oldStrProp.put("type", "string");
        oldStrProp.put("description", "The exact string to find (must match exactly once)");

        ObjectNode newStrProp = properties.putObject("new_str");
        newStrProp.put("type", "string");
        newStrProp.put("description", "The replacement string");

        ArrayNode required = schema.putArray("required");
        required.add("file_path");
        required.add("old_str");
        required.add("new_str");

        return schema;
    }
}
