package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.stream.Collectors;

/**
 * TaskListTool — lists all tasks with their status.
 * Task 50.3
 */
public class TaskListTool extends Tool<JsonNode, String> {

    @Override
    public String name() { return "TaskList"; }

    @Override
    public String description() {
        return "List all tasks with their current status.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode statusSchema = mapper().createObjectNode();
        statusSchema.put("type", "string");
        statusSchema.put("enum", mapper().createArrayNode().add("all").add("pending").add("running").add("completed").add("failed"));
        statusSchema.put("description", "Filter tasks by status (default: all)");
        props.set("status", statusSchema);

        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String filterStatus = input.has("status") ? input.get("status").asText("all") : "all";

        var tasks = TaskCreateTool.getAllTasks();
        var filtered = tasks.values().stream()
            .filter(t -> "all".equals(filterStatus) || t.status().equals(filterStatus))
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return "No tasks found" + ("all".equals(filterStatus) ? "." : " with status: " + filterStatus);
        }

        String list = filtered.stream()
            .map(t -> String.format("  [%s] %s: %s", t.status(), t.id(), t.description()))
            .collect(Collectors.joining("\n"));

        return String.format("Tasks (%d):\n%s", filtered.size(), list);
    }

    @Override
    public boolean isReadOnly() { return true; }

    @Override
    public boolean isConcurrencySafe() { return true; }
}
