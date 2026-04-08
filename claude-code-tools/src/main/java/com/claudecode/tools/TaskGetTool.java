package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * TaskGetTool — retrieves task status by ID.
 * Task 50.2
 */
public class TaskGetTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "TaskGet"; }

    @Override
    public String description() {
        return "Get the status and details of a task by its ID.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode idSchema = mapper().createObjectNode();
        idSchema.put("type", "string");
        idSchema.put("description", "Task ID to look up");
        props.set("task_id", idSchema);

        schema.set("required", mapper().createArrayNode().add("task_id"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String taskId = input.has("task_id") ? input.get("task_id").asText() : null;
        if (taskId == null || taskId.isBlank()) {
            return "Error: task_id is required.";
        }

        TaskCreateTool.TaskInfo task = TaskCreateTool.getTask(taskId);
        if (task == null) {
            return String.format("Error: task '%s' not found.", taskId);
        }

        return String.format("Task: %s\n  Description: %s\n  Type: %s\n  Status: %s\n  Command: %s",
            task.id(), task.description(), task.type(), task.status(), task.command());
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public boolean isConcurrencySafe() { return true; }
}
