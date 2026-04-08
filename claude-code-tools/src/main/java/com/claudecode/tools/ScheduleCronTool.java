package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ScheduleCronTool — cron-like scheduled task management.
 * Task 53.3
 */
public class ScheduleCronTool extends Tool<JsonNode, String> {

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(4);
    private static final Map<String, ScheduledJob> JOBS = new ConcurrentHashMap<>();

    @Override
    public String name() { return "ScheduleCron"; }

    @Override
    public String description() {
        return "Schedule, list, or delete timed tasks. " +
               "Supports one-time delays (in seconds) with a command to execute.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = createObjectSchema();
        ObjectNode props = (ObjectNode) schema.get("properties");

        ObjectNode actionSchema = mapper().createObjectNode();
        actionSchema.put("type", "string");
        actionSchema.put("enum", mapper().createArrayNode().add("create").add("delete").add("list"));
        actionSchema.put("description", "Action to perform");
        props.set("action", actionSchema);

        ObjectNode delaySchema = mapper().createObjectNode();
        delaySchema.put("type", "integer");
        delaySchema.put("minimum", 1);
        delaySchema.put("description", "Delay in seconds before executing the command (required for create)");
        props.set("delay_seconds", delaySchema);

        ObjectNode commandSchema = mapper().createObjectNode();
        commandSchema.put("type", "string");
        commandSchema.put("description", "Command or description to execute (required for create)");
        props.set("command", commandSchema);

        ObjectNode jobIdSchema = mapper().createObjectNode();
        jobIdSchema.put("type", "string");
        jobIdSchema.put("description", "Job ID (required for delete)");
        props.set("job_id", jobIdSchema);

        schema.set("required", mapper().createArrayNode().add("action"));
        return schema;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String action = input.has("action") ? input.get("action").asText() : "";

        return switch (action) {
            case "create" -> createJob(input);
            case "delete" -> deleteJob(input);
            case "list" -> listJobs();
            default -> "Error: action must be one of: create, delete, list.";
        };
    }

    private String createJob(JsonNode input) {
        String command = input.has("command") ? input.get("command").asText() : null;
        int delaySeconds = input.has("delay_seconds") ? input.get("delay_seconds").asInt(0) : 0;

        if (command == null || command.isBlank()) {
            return "Error: command is required for create action.";
        }
        if (delaySeconds <= 0) {
            return "Error: delay_seconds must be > 0 for create action.";
        }

        String jobId = "job_" + System.currentTimeMillis();
        ScheduledFuture<?> future = SCHEDULER.schedule(
            () -> System.out.println("[Scheduled] " + command),
            delaySeconds, TimeUnit.SECONDS);

        JOBS.put(jobId, new ScheduledJob(command, delaySeconds, future));
        return String.format("Scheduled job '%s': '%s' will run in %d second(s).",
            jobId, command, delaySeconds);
    }

    private String deleteJob(JsonNode input) {
        String jobId = input.has("job_id") ? input.get("job_id").asText() : null;
        if (jobId == null || jobId.isBlank()) {
            return "Error: job_id is required for delete action.";
        }

        ScheduledJob job = JOBS.remove(jobId);
        if (job == null) {
            return String.format("Error: job '%s' not found.", jobId);
        }
        job.future().cancel(false);
        return String.format("Deleted scheduled job '%s'.", jobId);
    }

    private String listJobs() {
        if (JOBS.isEmpty()) {
            return "No scheduled jobs.";
        }
        return JOBS.entrySet().stream()
            .map(e -> String.format("  %s: '%s' in %ds (done: %s)",
                e.getKey(), e.getValue().command(), e.getValue().delaySeconds(),
                e.getValue().future().isDone()))
            .collect(Collectors.joining("\n"));
    }

    @Override
    public boolean isReadOnly() { return false; }

    private record ScheduledJob(String command, int delaySeconds, ScheduledFuture<?> future) {}
}
