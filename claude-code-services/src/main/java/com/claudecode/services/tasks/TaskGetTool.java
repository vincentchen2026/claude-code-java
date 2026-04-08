package com.claudecode.services.tasks;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tool to get a task by ID.
 */
public class TaskGetTool extends Tool<JsonNode, String> {

    private final TaskStore taskStore;

    public TaskGetTool(TaskStore taskStore) {
        this.taskStore = taskStore;
    }

    @Override
    public String name() { return "TaskGet"; }

    @Override
    public String description() { return "Get task details by ID"; }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("task_id").put("type", "string").put("description", "Task ID");
        ArrayNode required = schema.putArray("required");
        required.add("task_id");
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String taskId = input.get("task_id").asText();
        return taskStore.get(taskId)
            .map(t -> String.format("Task %s [%s] %s: %s", t.id(), t.status(), t.type(), t.description()))
            .orElse("Error: Task not found: " + taskId);
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        return PermissionDecision.ALLOW;
    }
}
