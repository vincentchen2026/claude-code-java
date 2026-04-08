package com.claudecode.services.tasks;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tool to list all tasks.
 */
public class TaskListTool extends Tool<JsonNode, String> {

    private final TaskStore taskStore;

    public TaskListTool(TaskStore taskStore) {
        this.taskStore = taskStore;
    }

    @Override
    public String name() { return "TaskList"; }

    @Override
    public String description() { return "List all background tasks"; }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties")
            .putObject("status").put("type", "string").put("description", "Filter by status (optional)");
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        List<TaskState> tasks;
        if (input.has("status") && !input.get("status").asText().isBlank()) {
            try {
                TaskStatus status = TaskStatus.valueOf(input.get("status").asText().toUpperCase());
                tasks = taskStore.listByStatus(status);
            } catch (IllegalArgumentException e) {
                return "Error: Unknown status: " + input.get("status").asText();
            }
        } else {
            tasks = taskStore.list();
        }

        if (tasks.isEmpty()) return "No tasks found.";

        return tasks.stream()
            .map(t -> String.format("  %s [%s] %s: %s", t.id(), t.status(), t.type(), t.description()))
            .collect(Collectors.joining("\n", "Tasks:\n", ""));
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        return PermissionDecision.ALLOW;
    }
}
