package com.claudecode.services.coordinator;

import com.claudecode.core.engine.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real implementation of WorkerAgent that executes LLM queries with tool execution.
 * Supports multi-turn agent loop with proper state tracking and cancellation.
 */
public class WorkerAgentImpl implements WorkerAgent {

    private static final Logger log = LoggerFactory.getLogger(WorkerAgentImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String workerId;
    private final StreamingClient llmClient;
    private final ToolExecutor toolExecutor;
    private final AbortController abortController;
    private final AtomicBoolean cancelled;

    private volatile WorkerState state;

    public WorkerAgentImpl(String workerId, StreamingClient llmClient, ToolExecutor toolExecutor) {
        this.workerId = workerId;
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
        this.abortController = new AbortController();
        this.cancelled = new AtomicBoolean(false);
        this.state = WorkerState.idle(workerId);
    }

    @Override
    public WorkerResult execute(WorkerConfig config) {
        Instant startTime = Instant.now();
        state = new WorkerState(config.workerId(), WorkerState.Status.RUNNING, 0, 100_000, 0.0);

        List<StreamingClient.StreamRequest.RequestMessage> messages = new ArrayList<>();
        List<String> toolErrors = new ArrayList<>();
        int turnsUsed = 0;
        StringBuilder finalOutput = new StringBuilder();

        String systemPrompt = buildWorkerSystemPrompt(config);
        messages.add(new StreamingClient.StreamRequest.RequestMessage("user", config.taskDescription()));

        try {
            while (turnsUsed < config.maxTurns() && !cancelled.get()) {
                if (abortController.isAborted()) {
                    break;
                }

                turnsUsed++;
                double progress = (double) turnsUsed / config.maxTurns();

                state = new WorkerState(
                    config.workerId(),
                    WorkerState.Status.RUNNING,
                    turnsUsed,
                    Math.max(0, 100_000 - turnsUsed * 1000),
                    progress
                );

                StreamingClient.StreamRequest request = new StreamingClient.StreamRequest(
                    config.model().orElse(llmClient.getModel()),
                    4096,
                    systemPrompt,
                    messages,
                    true,
                    buildToolDefs(config)
                );

                Iterator<StreamingClient.StreamingEvent> events = llmClient.createStream(request);

                StringBuilder responseText = new StringBuilder();
                List<ToolUseRecord> toolCallsThisTurn = new ArrayList<>();
                String stopReason = null;

                while (events.hasNext()) {
                    if (cancelled.get() || abortController.isAborted()) {
                        break;
                    }

                    StreamingClient.StreamingEvent event = events.next();

                    switch (event) {
                        case StreamingClient.StreamingEvent.MessageStartEvent mse -> {
                            // Message started
                        }
                        case StreamingClient.StreamingEvent.ContentBlockStartEvent cbs -> {
                            if ("tool_use".equals(cbs.type())) {
                                toolCallsThisTurn.add(new ToolUseRecord(cbs.id(), cbs.name(), new StringBuilder()));
                            }
                        }
                        case StreamingClient.StreamingEvent.ContentBlockDeltaEvent cbd -> {
                            if ("text_delta".equals(cbd.deltaType())) {
                                responseText.append(cbd.deltaText());
                            } else if ("input_json_delta".equals(cbd.deltaType())) {
                                if (!toolCallsThisTurn.isEmpty()) {
                                    toolCallsThisTurn.get(toolCallsThisTurn.size() - 1).args.append(cbd.deltaText());
                                }
                            }
                        }
                        case StreamingClient.StreamingEvent.ContentBlockStopEvent cbs -> {
                            // Content block stopped
                        }
                        case StreamingClient.StreamingEvent.MessageDeltaEvent mde -> {
                            stopReason = mde.stopReason();
                        }
                        case StreamingClient.StreamingEvent.MessageStopEvent mse -> {
                            // Message complete
                        }
                        case StreamingClient.StreamingEvent.ErrorEvent ee -> {
                            toolErrors.add("Error: " + ee.exception().getMessage());
                        }
                    }
                }

                if (cancelled.get() || abortController.isAborted()) {
                    break;
                }

                String assistantResponse = responseText.toString();

                if (!toolCallsThisTurn.isEmpty()) {
                    messages.add(new StreamingClient.StreamRequest.RequestMessage("assistant", assistantResponse));

                    for (ToolUseRecord toolCall : toolCallsThisTurn) {
                        if (!config.allowedTools().isEmpty() && !config.allowedTools().contains(toolCall.name)) {
                            String error = "Tool '" + toolCall.name + "' not allowed for this worker";
                            toolErrors.add(error);
                            messages.add(new StreamingClient.StreamRequest.RequestMessage("user",
                                "<tool_result tool_use_id=\"" + toolCall.id + "\">" + error + "</tool_result>"));
                            continue;
                        }

                        ToolExecutionContext ctx = ToolExecutionContext.of(abortController, workerId);
                        ToolResult result = toolExecutor.execute(toolCall.name, parseJson(toolCall.args.toString()), ctx);

                        String toolResultText = result.isError()
                            ? "Error: " + result.content().get(0)
                            : result.content().isEmpty() ? "Tool executed" : result.content().get(0).toString();

                        messages.add(new StreamingClient.StreamRequest.RequestMessage("user",
                            "<tool_result tool_use_id=\"" + toolCall.id + "\">" + toolResultText + "</tool_result>"));
                    }
                } else {
                    finalOutput.append(assistantResponse);
                    break;
                }

                if ("end_turn".equals(stopReason)) {
                    finalOutput.append(assistantResponse);
                    break;
                }

                if ("max_tokens".equals(stopReason)) {
                    finalOutput.append(assistantResponse).append("\n[Output truncated due to max tokens]");
                    break;
                }
            }

        } catch (Exception e) {
            log.error("Worker {} execution error", workerId, e);
            toolErrors.add("Execution error: " + e.getMessage());
        }

        Duration elapsed = Duration.between(startTime, Instant.now());

        if (cancelled.get() || abortController.isAborted()) {
            state = new WorkerState(config.workerId(), WorkerState.Status.CANCELLED,
                turnsUsed, 0, (double) turnsUsed / config.maxTurns());
            return new WorkerResult(config.workerId(), WorkerResult.WorkerStatus.CANCELLED,
                Optional.of(finalOutput.toString()),
                Optional.of("Worker cancelled"),
                turnsUsed, elapsed);
        }

        if (turnsUsed >= config.maxTurns()) {
            state = new WorkerState(config.workerId(), WorkerState.Status.COMPLETED,
                turnsUsed, 0, 1.0);
            return new WorkerResult(config.workerId(), WorkerResult.WorkerStatus.TIMED_OUT,
                Optional.of(finalOutput.toString()),
                Optional.of("Max turns exceeded"),
                turnsUsed, elapsed);
        }

        if (!toolErrors.isEmpty() && finalOutput.length() == 0) {
            state = new WorkerState(config.workerId(), WorkerState.Status.FAILED,
                turnsUsed, 0, (double) turnsUsed / config.maxTurns());
            return new WorkerResult(config.workerId(), WorkerResult.WorkerStatus.FAILED,
                Optional.empty(),
                Optional.of(String.join("; ", toolErrors)),
                turnsUsed, elapsed);
        }

        state = new WorkerState(config.workerId(), WorkerState.Status.COMPLETED,
            turnsUsed, Math.max(0, 100_000 - turnsUsed * 1000), 1.0);

        return new WorkerResult(config.workerId(), WorkerResult.WorkerStatus.COMPLETED,
            Optional.of(finalOutput.toString()),
            Optional.empty(),
            turnsUsed, elapsed);
    }

    @Override
    public void cancel() {
        cancelled.set(true);
        abortController.abort();

        if (state != null) {
            WorkerState.Status currentStatus = state.status();
            if (currentStatus != WorkerState.Status.COMPLETED &&
                currentStatus != WorkerState.Status.FAILED &&
                currentStatus != WorkerState.Status.CANCELLED) {
                state = new WorkerState(
                    state.workerId(),
                    WorkerState.Status.CANCELLED,
                    state.turnsCompleted(),
                    state.contextRemaining(),
                    state.progress()
                );
            }
        } else {
            state = new WorkerState(workerId, WorkerState.Status.CANCELLED, 0, 0, 0.0);
        }
        log.info("Worker {} cancelled", workerId);
    }

    @Override
    public WorkerState getState() {
        return state;
    }

    private String buildWorkerSystemPrompt(WorkerConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a worker agent executing a task.\n\n");
        sb.append("Task: ").append(config.taskDescription()).append("\n\n");

        if (config.mode() == WorkerConfig.WorkerMode.SIMPLE) {
            sb.append("Mode: SIMPLE (only Bash, Read, Edit, Write, Glob, Grep tools allowed)\n\n");
        } else {
            sb.append("Mode: FULL (all tools available)\n\n");
        }

        sb.append("Guidelines:\n");
        sb.append("- Use tools as needed to complete the task\n");
        sb.append("- If a tool fails, explain the error and try an alternative approach\n");
        sb.append("- Provide a summary of what was accomplished\n");
        sb.append("- Be concise but thorough\n");

        return sb.toString();
    }

    private List<StreamingClient.StreamRequest.ToolDef> buildToolDefs(WorkerConfig config) {
        if (toolExecutor == null) {
            return List.of();
        }
        return toolExecutor.getToolDefinitions().stream()
            .filter(def -> config.allowedTools().isEmpty() || config.allowedTools().contains(def.name()))
            .toList();
    }

    private com.fasterxml.jackson.databind.JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return MAPPER.createObjectNode();
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return MAPPER.createObjectNode();
        }
    }

    private record ToolUseRecord(String id, String name, StringBuilder args) {}
}
