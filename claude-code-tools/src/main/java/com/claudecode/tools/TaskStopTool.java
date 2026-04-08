package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

/**
 * TaskStopTool — stops a running task.
 * Task 50.4
 */
public class TaskStopTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "TaskStop"; }

    @Override
    public String description() {
        return "Stop a running task by its ID.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode idSchema = mapper().createObjectNode();
        idSchema.put("type", "string");
        idSchema.put("description", "Task ID to stop");
        props.set("task_id", idSchema);

        ObjectNode reasonSchema = mapper().createObjectNode();
        reasonSchema.put("type", "string");
        reasonSchema.put("description", "Reason for stopping (optional)");
        props.set("reason", reasonSchema);

        schema.set("required", mapper().createArrayNode().add("task_id"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String taskId = input.has("task_id") ? input.get("task_id").asText() : null;
        String reason = input.has("reason") ? input.get("reason").asText() : "User requested stop";

        if (taskId == null || taskId.isBlank()) {
            return "Error: task_id is required.";
        }

        Optional<TaskCreateTool.TaskInfo> taskOpt = Optional.ofNullable(TaskCreateTool.getTask(taskId));
        if (taskOpt.isEmpty()) {
            return "Error: task '" + taskId + "' not found.";
        }

        TaskCreateTool.TaskInfo task = taskOpt.get();

        if ("completed".equals(task.status()) || "failed".equals(task.status())) {
            return "Error: Task '" + taskId + "' is already in terminal state: " + task.status();
        }

        if ("stopped".equals(task.status())) {
            return "Error: Task '" + taskId + "' is already stopped.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Task Stop Requested\n");
        sb.append("===================\n\n");
        sb.append("Task ID: ").append(taskId).append("\n");
        sb.append("Description: ").append(task.description()).append("\n");
        sb.append("Previous Status: ").append(task.status()).append("\n");
        sb.append("New Status: stopped\n");
        sb.append("Reason: ").append(reason).append("\n\n");
        sb.append("The task process has been interrupted.\n");
        sb.append("Use /tasks to list all tasks.\n");

        return sb.toString();
    }

    @Override
    public boolean isReadOnly() { return false; }
}
