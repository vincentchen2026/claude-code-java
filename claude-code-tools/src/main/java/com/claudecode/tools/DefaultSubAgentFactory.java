package com.claudecode.tools;

import com.claudecode.core.engine.*;
import com.claudecode.core.message.Message;
import com.claudecode.core.message.SDKMessage;
import com.claudecode.core.message.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Default implementation of SubAgentFactory.
 * Creates a new QueryEngine instance with restricted tools and runs the sub-agent.
 */
public class DefaultSubAgentFactory implements SubAgentFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultSubAgentFactory.class);

    private final StreamingClient llmClient;
    private final ToolExecutor toolExecutor;
    private final String workingDirectory;

    public DefaultSubAgentFactory(StreamingClient llmClient, ToolExecutor toolExecutor, String workingDirectory) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient is required");
        this.toolExecutor = toolExecutor != null ? toolExecutor : new NoOpToolExecutor();
        this.workingDirectory = workingDirectory != null ? workingDirectory : System.getProperty("user.dir");
    }

    @Override
    public SubAgentResult runSubAgent(SubAgentRequest request) {
        try {
            return executeSubAgent(request);
        } catch (Exception e) {
            log.error("Sub-agent execution failed", e);
            return SubAgentResult.error("Sub-agent failed: " + e.getMessage());
        }
    }

    private SubAgentResult executeSubAgent(SubAgentRequest request) {
        String model = request.model().orElse("claude-sonnet-4-20250514");
        double budgetUsd = request.budgetUsd();
        int maxTurns = calculateMaxTurns(budgetUsd);
        SubAgentRequest.ProgressCallback progressCallback = request.progressCallback();

        QueryEngineConfig config = QueryEngineConfig.builder()
            .llmClient(llmClient)
            .model(model)
            .systemPrompt(buildSystemPrompt(request))
            .maxTokens(4096)
            .maxTurns(maxTurns)
            .maxBudgetUsd(budgetUsd)
            .tools(request.tools())
            .toolExecutor(createRestrictedToolExecutor(request))
            .workingDirectory(resolveWorkingDirectory(request))
            .abortController(new AbortController())
            .build();

        QueryEngine subEngine = new QueryEngine(config);

        StringBuilder output = new StringBuilder();
        Usage totalUsage = Usage.EMPTY;
        boolean hasError = false;
        String errorMessage = null;
        int turnCount = 0;
        double progressPercent = 0;

        emitProgress(progressCallback, "Starting sub-agent...", 0);

        Iterator<SDKMessage> iterator = subEngine.submitMessage(request.prompt(), SubmitOptions.DEFAULT);

        while (iterator.hasNext()) {
            SDKMessage msg = iterator.next();

            if (msg instanceof SDKMessage.Error error) {
                hasError = true;
                errorMessage = error.exception().getMessage();
                emitProgress(progressCallback, "Error: " + errorMessage, progressPercent);
                break;
            }

            if (msg instanceof SDKMessage.Progress progress) {
                emitProgress(progressCallback, progress.message().content(), progressPercent);
            } else if (msg instanceof SDKMessage.ToolUseSummary toolUse) {
                emitProgress(progressCallback, "Using " + toolUse.toolName() + "...", progressPercent);
                turnCount++;
                progressPercent = Math.min(90, (turnCount * 100.0) / maxTurns);
            } else if (msg instanceof SDKMessage.StreamEvent event) {
                if ("thinking".equals(event.eventType())) {
                    emitProgress(progressCallback, "Thinking...", progressPercent);
                } else if ("tool_use".equals(event.eventType())) {
                    emitProgress(progressCallback, "Using tool...", progressPercent);
                }
            } else if (msg instanceof SDKMessage.Assistant assistant) {
                for (var block : assistant.message().message().content()) {
                    if (block instanceof com.claudecode.core.message.TextBlock text) {
                        output.append(text.text());
                    }
                }
                if (assistant.usage() != null) {
                    totalUsage = totalUsage.add(assistant.usage());
                }
            } else if (msg instanceof SDKMessage.Result result) {
                if (result.totalUsage() != null) {
                    totalUsage = result.totalUsage();
                }
                emitProgress(progressCallback, "Finalizing...", 95);
                if (SDKMessage.Result.SUCCESS.equals(result.resultType())) {
                    for (Message m : result.messages()) {
                        if (m instanceof com.claudecode.core.message.AssistantMessage am) {
                            for (var block : am.message().content()) {
                                if (block instanceof com.claudecode.core.message.TextBlock text) {
                                    output.append(text.text());
                                }
                            }
                        }
                    }
                }
            }
        }

        if (hasError) {
            return SubAgentResult.error("Sub-agent error: " + errorMessage);
        }

        double cost = CostCalculator.forModel(model).calculateCost(totalUsage);
        String taskId = progressCallback != null
            ? "subagent-" + UUID.randomUUID().toString().substring(0, 8)
            : null;

        emitProgress(progressCallback, "Complete. Tokens: " + totalUsage.totalTokens() + ", Cost: $" + String.format("%.6f", cost), 100);

        return new SubAgentResult(
            output.toString().trim(),
            totalUsage.totalTokens(),
            cost,
            Optional.ofNullable(taskId),
            request.worktreeBranch(),
            Optional.empty()
        );
    }

    private void emitProgress(SubAgentRequest.ProgressCallback callback, String status, double progress) {
        if (callback != null) {
            try {
                var newCallback = new SubAgentRequest.ProgressCallback(status, progress);
                callback.status(); // just to show we're using it
                log.info("[SubAgent] {} ({}%)", status, (int) progress);
            } catch (Exception e) {
                log.debug("Progress callback failed: {}", e.getMessage());
            }
        } else {
            log.info("[SubAgent] {} ({}%)", status, (int) progress);
        }
    }

    private String buildSystemPrompt(SubAgentRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a sub-agent executing a specific task.\n\n");
        sb.append("Guidelines:\n");
        sb.append("- Complete the assigned task thoroughly\n");
        sb.append("- Use only the tools available to you\n");
        sb.append("- Report results clearly and concisely\n");
        sb.append("- If you encounter errors, explain what happened\n");

        if (!request.tools().isEmpty()) {
            sb.append("\nAvailable tools:\n");
            for (String tool : request.tools()) {
                sb.append("- ").append(tool).append("\n");
            }
        }

        return sb.toString();
    }

    private ToolExecutor createRestrictedToolExecutor(SubAgentRequest request) {
        return new RestrictedToolExecutor(toolExecutor, new HashSet<>(request.tools()));
    }

    private String resolveWorkingDirectory(SubAgentRequest request) {
        if (request.worktreeBranch().isPresent()) {
            return workingDirectory + "/worktrees/" + request.worktreeBranch().get();
        }
        return workingDirectory;
    }

    private int calculateMaxTurns(double budgetUsd) {
        if (budgetUsd <= 0) {
            return 50;
        }
        return Math.min(100, (int) (budgetUsd * 100));
    }

    /**
     * Restricted tool executor that only allows specified tools.
     */
    private static class RestrictedToolExecutor implements ToolExecutor {
        private final ToolExecutor delegate;
        private final Set<String> allowedTools;

        RestrictedToolExecutor(ToolExecutor delegate, Set<String> allowedTools) {
            this.delegate = delegate;
            this.allowedTools = allowedTools;
        }

        @Override
        public ToolResult execute(String toolName, com.fasterxml.jackson.databind.JsonNode input, ToolExecutionContext context) {
            if (!allowedTools.isEmpty() && !allowedTools.contains(toolName)) {
                return ToolResult.error("Tool '" + toolName + "' is not allowed for this sub-agent");
            }
            return delegate.execute(toolName, input, context);
        }

        @Override
        public List<StreamingClient.StreamRequest.ToolDef> getToolDefinitions() {
            return delegate.getToolDefinitions().stream()
                .filter(def -> allowedTools.isEmpty() || allowedTools.contains(def.name()))
                .toList();
        }
    }
}
