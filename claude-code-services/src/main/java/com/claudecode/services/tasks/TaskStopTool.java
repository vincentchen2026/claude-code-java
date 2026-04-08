package com.claudecode.services.tasks;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tool to stop/kill a running task.
 */
public class TaskStopTool extends Tool<JsonNode, String> {

    private final TaskStore taskStore;

    public TaskStopTool(TaskStore taskStore) {
        this.taskStore = taskStore;
    }

    @Override
    public String name() { return "TaskStop"; }

    @Override
    public String description() { return "Stop a running task"; }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("task_id").put("type", "string").put("description", "Task ID to stop");
        ArrayNode required = schema.putArray("required");
        required.add("task_id");
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String taskId = input.get("task_id").asText();
        try {
            TaskState updated = taskStore.updateStatus(taskId, TaskStatus.KILLED);
            return "Task " + updated.id() + " stopped.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        return PermissionDecision.ALLOW;
    }
}
