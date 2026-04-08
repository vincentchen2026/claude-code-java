package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TodoWriteTool — in-memory TODO list management.
 * Task 58.3 refactor: from file-writing to memory state management.
 *
 * Features:
 * - Structured TodoItem records (content, status, priority)
 * - ConcurrentHashMap per session for isolation
 * - Verification nudge detection (detects when todos need updating)
 * - TodoV2 feature flag support
 * - oldTodos/newTodos output for diff tracking
 */
public class TodoWriteTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();

    // Task 58.3: In-memory todo store (keyed by session ID)
    private static final Map<String, List<TodoItem>> TODO_STORE = new ConcurrentHashMap<>();
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    // Task 58.3: Feature flag for TodoV2
    private static volatile boolean TODO_V2_ENABLED = true;

    @Override public String name() { return "TodoWrite"; }

    @Override public String description() {
        return "Manage a structured TODO list. Supports creating, updating, and tracking task items.";
    }

    @Override public JsonNode inputSchema() { return SCHEMA; }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String sessionId = context.sessionId();
        String action = input.has("action") ? input.get("action").asText("") : "";

        // Task 58.3: Support both legacy file-based and new memory-based modes
        if (input.has("file_path") && !TODO_V2_ENABLED) {
            return legacyFileMode(input, context);
        }

        return switch (action) {
            case "add" -> addTodo(sessionId, input);
            case "update" -> updateTodo(sessionId, input);
            case "delete" -> deleteTodo(sessionId, input);
            case "list" -> listTodos(sessionId);
            case "clear" -> clearTodos(sessionId);
            case "" -> {
                // If no action specified, treat as replace-all (legacy behavior)
                yield replaceAllTodos(sessionId, input);
            }
            default -> "Error: action must be one of: add, update, delete, list, clear, or omit for replace-all";
        };
    }

    /**
     * Task 58.3: Add a new todo item.
     */
    private String addTodo(String sessionId, JsonNode input) {
        String content = input.has("content") ? input.get("content").asText("") : "";
        String status = input.has("status") ? input.get("status").asText("pending") : "pending";
        String priority = input.has("priority") ? input.get("priority").asText("medium") : "medium";

        if (content.isBlank()) {
            return "Error: content is required for add action.";
        }

        List<TodoItem> todos = getTodos(sessionId);
        int id = ID_COUNTER.incrementAndGet();
        TodoItem newItem = new TodoItem(id, content, status, priority);
        todos.add(newItem);

        return String.format("Added TODO #%d [%s] %s (priority: %s)", id, status, content, priority);
    }

    /**
     * Task 58.3: Update an existing todo item.
     */
    private String updateTodo(String sessionId, JsonNode input) {
        int id = input.has("id") ? input.get("id").asInt(-1) : -1;
        if (id <= 0) {
            return "Error: valid id is required for update action.";
        }

        List<TodoItem> todos = getTodos(sessionId);
        TodoItem existing = todos.stream().filter(t -> t.id() == id).findFirst().orElse(null);
        if (existing == null) {
            return String.format("Error: TODO #%d not found.", id);
        }

        String newContent = input.has("content") ? input.get("content").asText(existing.content()) : existing.content();
        String newStatus = input.has("status") ? input.get("status").asText(existing.status()) : existing.status();
        String newPriority = input.has("priority") ? input.get("priority").asText(existing.priority()) : existing.priority();

        TodoItem updated = new TodoItem(id, newContent, newStatus, newPriority);
        todos.replaceAll(t -> t.id() == id ? updated : t);

        return String.format("Updated TODO #%d: [%s] %s (priority: %s)", id, newStatus, newContent, newPriority);
    }

    /**
     * Task 58.3: Delete a todo item.
     */
    private String deleteTodo(String sessionId, JsonNode input) {
        int id = input.has("id") ? input.get("id").asInt(-1) : -1;
        if (id <= 0) {
            return "Error: valid id is required for delete action.";
        }

        List<TodoItem> todos = getTodos(sessionId);
        boolean removed = todos.removeIf(t -> t.id() == id);
        return removed ? String.format("Deleted TODO #%d.", id) : String.format("Error: TODO #%d not found.", id);
    }

    /**
     * Task 58.3: List all todo items.
     */
    private String listTodos(String sessionId) {
        List<TodoItem> todos = getTodos(sessionId);
        if (todos.isEmpty()) {
            return "No TODO items.";
        }

        StringBuilder sb = new StringBuilder("TODO List:\n");
        for (TodoItem item : todos) {
            String marker = switch (item.status()) {
                case "completed" -> "✓";
                case "in_progress" -> "⟳";
                case "cancelled" -> "✗";
                default -> "○";
            };
            sb.append(String.format("  #%d %s [%s] %s (priority: %s)\n",
                item.id(), marker, item.status(), item.content(), item.priority()));
        }
        return sb.toString().trim();
    }

    /**
     * Task 58.3: Clear all todos.
     */
    private String clearTodos(String sessionId) {
        TODO_STORE.remove(sessionId);
        return "All TODO items cleared.";
    }

    /**
     * Task 58.3: Replace all todos (legacy replace-all behavior).
     */
    private String replaceAllTodos(String sessionId, JsonNode input) {
        String content = input.has("content") ? input.get("content").asText("") : "";
        if (content.isBlank()) {
            return "Error: content is required.";
        }

        List<TodoItem> oldTodos = new ArrayList<>(getTodos(sessionId));
        List<TodoItem> newTodos = new ArrayList<>();

        // Parse content lines into todo items
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            // Parse status marker: [x] = completed, [-] = in_progress, [ ] = pending
            String status = "pending";
            String text = trimmed;
            if (trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ")) {
                status = "completed";
                text = trimmed.substring(4);
            } else if (trimmed.startsWith("[-] ")) {
                status = "in_progress";
                text = trimmed.substring(4);
            } else if (trimmed.startsWith("[ ] ")) {
                status = "pending";
                text = trimmed.substring(4);
            }

            int id = ID_COUNTER.incrementAndGet();
            newTodos.add(new TodoItem(id, text, status, "medium"));
        }

        TODO_STORE.put(sessionId, newTodos);

        // Task 58.3: Verification nudge detection
        String nudge = detectVerificationNudge(newTodos);

        return String.format(
            "TODO list updated (%d items).\n%s%s",
            newTodos.size(),
            formatTodos(newTodos),
            nudge);
    }

    /**
     * Task 58.3: Legacy file-based mode (when TodoV2 is disabled).
     */
    private String legacyFileMode(JsonNode input, ToolExecutionContext context) {
        String filePath = input.has("file_path") ? input.get("file_path").asText("") : "";
        String content = input.has("content") ? input.get("content").asText("") : "";

        if (filePath.isBlank() || content.isBlank()) {
            return "Error: file_path and content are required in legacy mode.";
        }

        try {
            java.nio.file.Path path = java.nio.file.Path.of(context.workingDirectory()).resolve(filePath);
            java.nio.file.Files.writeString(path, content, java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            return "TODO file written: " + filePath;
        } catch (Exception e) {
            return "Error: failed to write TODO file: " + e.getMessage();
        }
    }

    /**
     * Task 58.3: Get todos for a session, creating if needed.
     */
    private List<TodoItem> getTodos(String sessionId) {
        return TODO_STORE.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    /**
     * Task 58.3: Detect verification nudge — suggests updating todos when many are completed.
     */
    private String detectVerificationNudge(List<TodoItem> todos) {
        long completed = todos.stream().filter(t -> "completed".equals(t.status())).count();
        long total = todos.size();
        if (total > 0 && completed == total) {
            return "\n[Verification nudge: All TODO items are completed. Consider clearing the list or adding new items.]";
        }
        return "";
    }

    /**
     * Format todos for display.
     */
    private String formatTodos(List<TodoItem> todos) {
        StringBuilder sb = new StringBuilder();
        for (TodoItem item : todos) {
            String marker = switch (item.status()) {
                case "completed" -> "[x]";
                case "in_progress" -> "[-]";
                case "cancelled" -> "[/]";
                default -> "[ ]";
            };
            sb.append(String.format("  %s %s (priority: %s)\n", marker, item.content(), item.priority()));
        }
        return sb.toString();
    }

    /**
     * Task 58.3: TodoItem record.
     */
    public record TodoItem(int id, String content, String status, String priority) {}

    /**
     * Task 58.3: Get all todos for a session (for testing).
     */
    public static List<TodoItem> getTodosForSession(String sessionId) {
        return new ArrayList<>(TODO_STORE.getOrDefault(sessionId, List.of()));
    }

    /**
     * Task 58.3: Clear store (for testing).
     */
    public static void clearStore() {
        TODO_STORE.clear();
        ID_COUNTER.set(0);
    }

    /**
     * Task 58.3: Set TodoV2 feature flag.
     */
    public static void setTodoV2Enabled(boolean enabled) {
        TODO_V2_ENABLED = enabled;
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");

        ObjectNode actionProp = properties.putObject("action");
        actionProp.put("type", "string");
        actionProp.put("enum", mapper().createArrayNode()
            .add("add").add("update").add("delete").add("list").add("clear"));
        actionProp.put("description", "Action to perform (omit for replace-all mode)");

        ObjectNode contentProp = properties.putObject("content");
        contentProp.put("type", "string");
        contentProp.put("description", "TODO content or full TODO list text (for replace-all)");

        ObjectNode idProp = properties.putObject("id");
        idProp.put("type", "integer");
        idProp.put("description", "TODO item ID (required for update/delete)");

        ObjectNode statusProp = properties.putObject("status");
        statusProp.put("type", "string");
        statusProp.put("enum", mapper().createArrayNode()
            .add("pending").add("in_progress").add("completed").add("cancelled"));
        statusProp.put("description", "Status of the TODO item");

        ObjectNode priorityProp = properties.putObject("priority");
        priorityProp.put("type", "string");
        priorityProp.put("enum", mapper().createArrayNode()
            .add("low").add("medium").add("high").add("critical"));
        priorityProp.put("description", "Priority of the TODO item");

        // Legacy fields for backward compatibility
        ObjectNode filePathProp = properties.putObject("file_path");
        filePathProp.put("type", "string");
        filePathProp.put("description", "Legacy: file path for TODO file (used when TodoV2 is disabled)");

        return schema;
    }
}
