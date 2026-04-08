package com.claudecode.services.tasks;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.claudecode.tools.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tool to create a new background task.
 */
public class TaskCreateTool extends Tool<JsonNode, String> {

    private final TaskStore taskStore;

    public TaskCreateTool(TaskStore taskStore) {
        this.taskStore = taskStore;
    }

    @Override
    public String name() { return "TaskCreate"; }

    @Override
    public String description() { return "Create a new background task"; }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("type").put("type", "string").put("description", "Task type");
        props.putObject("description").put("type", "string").put("description", "Task description");
        ArrayNode required = schema.putArray("required");
        required.add("type");
        required.add("description");
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String typeStr = input.get("type").asText();
        String desc = input.get("description").asText();
        TaskType type;
        try {
            type = TaskType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Error: Unknown task type: " + typeStr;
        }
        TaskState task = taskStore.create(type, desc);
        return "Created task " + task.id() + " (" + task.type() + "): " + task.description();
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        return PermissionDecision.ALLOW;
    }
}
