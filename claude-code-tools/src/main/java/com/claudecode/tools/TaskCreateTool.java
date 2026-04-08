package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TaskCreateTool — creates a background task.
 * Task 50.1
 */
public class TaskCreateTool extends Tool<JsonNode, String> {

    private static final Map<String, TaskInfo> TASKS = new ConcurrentHashMap<>();
    private static int TASK_COUNTER = 0;

    @Override
    public String name() { return "TaskCreate"; }

    @Override
    public String description() {
        return "Create a background task for asynchronous execution. " +
               "Returns a task ID that can be used to check status.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode descSchema = mapper().createObjectNode();
        descSchema.put("type", "string");
        descSchema.put("description", "Description of the task to create");
        props.set("description", descSchema);

        ObjectNode typeSchema = mapper().createObjectNode();
        typeSchema.put("type", "string");
        typeSchema.put("enum", mapper().createArrayNode().add("shell").add("agent").add("workflow"));
        typeSchema.put("description", "Type of task (default: shell)");
        props.set("type", typeSchema);

        ObjectNode cmdSchema = mapper().createObjectNode();
        cmdSchema.put("type", "string");
        cmdSchema.put("description", "Command or prompt to execute");
        props.set("command", cmdSchema);

        schema.set("required", mapper().createArrayNode().add("description"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String description = input.has("description") ? input.get("description").asText() : "";
        String type = input.has("type") ? input.get("type").asText("shell") : "shell";
        String command = input.has("command") ? input.get("command").asText("") : "";

        if (description.isBlank()) {
            return "Error: description is required.";
        }

        String taskId = "task_" + (++TASK_COUNTER);
        TASKS.put(taskId, new TaskInfo(taskId, description, type, command, "pending"));

        return String.format("Task created: %s\n  Description: %s\n  Type: %s\n  Status: pending",
            taskId, description, type);
    }

    /** Get task info by ID (used by other task tools). */
    public static TaskInfo getTask(String taskId) {
        return TASKS.get(taskId);
    }

    /** Get all tasks. */
    public static Map<String, TaskInfo> getAllTasks() {
        return Map.copyOf(TASKS);
    }

    public record TaskInfo(String id, String description, String type, String command, String status) {}
}
