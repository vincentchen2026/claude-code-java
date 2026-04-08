package com.claudecode.services.tasks;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tool to update a task's status or description.
 */
public class TaskUpdateTool extends Tool<JsonNode, String> {

    private final TaskStore taskStore;

    public TaskUpdateTool(TaskStore taskStore) {
        this.taskStore = taskStore;
    }

    @Override
    public String name() { return "TaskUpdate"; }

    @Override
    public String description() { return "Update a task's status or description"; }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("task_id").put("type", "string").put("description", "Task ID");
        props.putObject("status").put("type", "string").put("description", "New status (optional)");
        props.putObject("description").put("type", "string").put("description", "New description (optional)");
        ArrayNode required = schema.putArray("required");
        required.add("task_id");
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String taskId = input.get("task_id").asText();
        try {
            if (input.has("status") && !input.get("status").asText().isBlank()) {
                TaskStatus status = TaskStatus.valueOf(input.get("status").asText().toUpperCase());
                taskStore.updateStatus(taskId, status);
            }
            if (input.has("description") && !input.get("description").asText().isBlank()) {
                taskStore.updateDescription(taskId, input.get("description").asText());
            }
            return taskStore.get(taskId)
                .map(t -> "Updated task " + t.id() + " [" + t.status() + "]: " + t.description())
                .orElse("Error: Task not found");
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        return PermissionDecision.ALLOW;
    }
}
