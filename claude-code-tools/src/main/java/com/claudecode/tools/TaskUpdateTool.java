package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

/**
 * TaskUpdateTool — updates task metadata/status.
 * Task 50.5
 */
public class TaskUpdateTool extends Tool<JsonNode, String> {

    private static final java.util.Set<String> VALID_STATUSES = 
        java.util.Set.of("pending", "running", "completed", "failed", "stopped", "cancelled");

    @Override
    public String name() { return "TaskUpdate"; }

    @Override
    public String description() {
        return "Update the status or metadata of a task.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode idSchema = mapper().createObjectNode();
        idSchema.put("type", "string");
        idSchema.put("description", "Task ID to update");
        props.set("task_id", idSchema);

        ObjectNode statusSchema = mapper().createObjectNode();
        statusSchema.put("type", "string");
        statusSchema.put("enum", mapper().createArrayNode()
            .add("pending").add("running").add("completed")
            .add("failed").add("stopped").add("cancelled"));
        statusSchema.put("description", "New status for the task");
        props.set("status", statusSchema);

        ObjectNode descriptionSchema = mapper().createObjectNode();
        descriptionSchema.put("type", "string");
        descriptionSchema.put("description", "New description for the task (optional)");
        props.set("description", descriptionSchema);

        ObjectNode progressSchema = mapper().createObjectNode();
        progressSchema.put("type", "integer");
        progressSchema.put("minimum", 0);
        progressSchema.put("maximum", 100);
        progressSchema.put("description", "Progress percentage (0-100, optional)");
        props.set("progress", progressSchema);

        schema.set("required", mapper().createArrayNode().add("task_id"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String taskId = input.has("task_id") ? input.get("task_id").asText() : null;
        String status = input.has("status") ? input.get("status").asText() : null;
        String description = input.has("description") ? input.get("description").asText() : null;
        Integer progress = input.has("progress") ? input.get("progress").asInt() : null;

        if (taskId == null || taskId.isBlank()) {
            return "Error: task_id is required.";
        }

        Optional<TaskCreateTool.TaskInfo> taskOpt = Optional.ofNullable(TaskCreateTool.getTask(taskId));
        if (taskOpt.isEmpty()) {
            return "Error: task '" + taskId + "' not found.";
        }

        TaskCreateTool.TaskInfo task = taskOpt.get();

        if (status != null && !status.isBlank()) {
            if (!VALID_STATUSES.contains(status.toLowerCase())) {
                return "Error: invalid status '" + status + "'. Valid statuses: " + VALID_STATUSES;
            }
        }

        if (progress != null && (progress < 0 || progress > 100)) {
            return "Error: progress must be between 0 and 100.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Task Updated\n");
        sb.append("============\n\n");
        sb.append("Task ID: ").append(taskId).append("\n");
        sb.append("Previous Status: ").append(task.status()).append("\n");

        if (status != null) {
            sb.append("New Status: ").append(status).append("\n");
        } else {
            sb.append("Status: unchanged\n");
        }

        if (description != null && !description.isBlank()) {
            sb.append("Description: ").append(description).append("\n");
        }

        if (progress != null) {
            sb.append("Progress: ").append(progress).append("%\n");
        }

        sb.append("\nTask metadata has been updated.\n");

        return sb.toString();
    }

    @Override
    public boolean isReadOnly() { return false; }
}
