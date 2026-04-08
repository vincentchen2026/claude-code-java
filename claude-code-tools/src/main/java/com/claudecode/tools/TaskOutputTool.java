package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

/**
 * TaskOutputTool — read output from a background task.
 * Input: {task_id}. Supports streaming output and partial reads.
 *
 * Task 55.6 enhancements:
 * - TaskOutputBuffer-based streaming
 * - Partial output reading
 * - Tail follow mode
 * - Output polling
 */
public class TaskOutputTool extends Tool<JsonNode, String> {

    private static final JsonNode SCHEMA = buildSchema();
    private static final int DEFAULT_TAIL_LINES = 100;
    private static final int MAX_OUTPUT_SIZE = 64 * 1024 * 1024;

    private final TaskOutputBuffer outputBuffer;

    public TaskOutputTool() {
        this(null);
    }

    public TaskOutputTool(TaskOutputBuffer outputBuffer) {
        this.outputBuffer = outputBuffer;
    }

    @Override
    public String name() { return "TaskOutput"; }

    @Override
    public String description() { return "Output task execution results with optional streaming"; }

    @Override
    public JsonNode inputSchema() { return SCHEMA; }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        String taskId = input.has("task_id") ? input.get("task_id").asText("") : "";

        if (taskId.isBlank()) {
            return "Error: task_id is required";
        }

        int offset = input.has("offset") ? input.get("offset").asInt(0) : 0;
        int limit = input.has("limit") ? input.get("limit").asInt(-1) : -1;
        boolean tail = input.has("tail") && input.get("tail").asBoolean(false);
        boolean stream = input.has("stream") && input.get("stream").asBoolean(false);
        long timeout = input.has("timeout") ? input.get("timeout").asLong(-1) : -1;

        if (stream && outputBuffer != null) {
            return streamOutput(taskId, offset, limit, timeout);
        }

        return readOutput(taskId, context.workingDirectory(), offset, limit, tail);
    }

    private String readOutput(String taskId, String workingDir, int offset, int limit, boolean tail) {
        Path outputFile = Path.of(workingDir)
                .resolve(".claude")
                .resolve("tasks")
                .resolve(taskId + ".output");

        if (!Files.exists(outputFile)) {
            Path altPath = Path.of(System.getProperty("user.home"), ".claude", "tasks",
                taskId + ".output");
            if (Files.exists(altPath)) {
                outputFile = altPath;
            } else {
                return "Error: no output found for task '" + taskId + "'";
            }
        }

        try {
            long fileSize = Files.size(outputFile);
            if (fileSize > MAX_OUTPUT_SIZE) {
                return "Error: output file too large (" + fileSize + " bytes, max " + MAX_OUTPUT_SIZE + ")";
            }

            if (tail) {
                return readTail(outputFile, limit > 0 ? limit : DEFAULT_TAIL_LINES);
            }

            String content = Files.readString(outputFile, StandardCharsets.UTF_8);

            if (offset > 0 && offset < content.length()) {
                content = content.substring(offset);
            }

            if (limit > 0 && limit < content.length()) {
                content = content.substring(0, limit);
            }

            return content;
        } catch (IOException e) {
            return "Error: failed to read task output: " + e.getMessage();
        }
    }

    private String readTail(Path file, int lines) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        String[] allLines = content.split("\n");

        if (allLines.length <= lines) {
            return content;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = allLines.length - lines; i < allLines.length; i++) {
            sb.append(allLines[i]);
            if (i < allLines.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String streamOutput(String taskId, int offset, int limit, long timeout) {
        if (outputBuffer == null) {
            return "Error: streaming not available - TaskOutputBuffer not configured";
        }

        try {
            StringBuilder result = new StringBuilder();
            long startTime = System.currentTimeMillis();
            long deadline = timeout > 0 ? startTime + timeout : Long.MAX_VALUE;

            outputBuffer.subscribe(new SimpleSubscriber(chunk -> {
                result.append(chunk);
            }));

            while (result.length() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
            }

            String output = result.toString();
            if (offset > 0 && offset < output.length()) {
                output = output.substring(offset);
            }
            if (limit > 0 && limit < output.length()) {
                output = output.substring(0, limit);
            }

            return output.isEmpty() ? "(no output yet)" : output;
        } catch (Exception e) {
            return "Error: streaming failed: " + e.getMessage();
        }
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");

        ObjectNode taskIdProp = properties.putObject("task_id");
        taskIdProp.put("type", "string");
        taskIdProp.put("description", "The ID of the task to read output from");

        ObjectNode offsetProp = properties.putObject("offset");
        offsetProp.put("type", "integer");
        offsetProp.put("description", "Byte offset to start reading from");
        offsetProp.put("default", 0);

        ObjectNode limitProp = properties.putObject("limit");
        limitProp.put("type", "integer");
        limitProp.put("description", "Maximum bytes to read (-1 for all)");

        ObjectNode tailProp = properties.putObject("tail");
        tailProp.put("type", "boolean");
        tailProp.put("description", "Read last lines instead of full output");
        tailProp.put("default", false);

        ObjectNode streamProp = properties.putObject("stream");
        streamProp.put("type", "boolean");
        streamProp.put("description", "Enable streaming mode for real-time output");
        streamProp.put("default", false);

        ObjectNode timeoutProp = properties.putObject("timeout");
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "Streaming timeout in milliseconds (-1 for no timeout)");

        ArrayNode required = schema.putArray("required");
        required.add("task_id");
        return schema;
    }

    public interface TaskOutputBuffer extends Flow.Publisher<String> {
    }

    private static class SimpleSubscriber implements Flow.Subscriber<String> {
        private final java.util.function.Consumer<String> consumer;

        private SimpleSubscriber(java.util.function.Consumer<String> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(String item) {
            consumer.accept(item);
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }
}
