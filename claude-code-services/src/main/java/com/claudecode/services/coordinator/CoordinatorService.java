package com.claudecode.services.coordinator;

import com.claudecode.core.engine.StreamingClient;
import com.claudecode.core.engine.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinator mode service for multi-worker orchestration.
 * Generates coordinator system prompts and manages worker lifecycle.
 */
public class CoordinatorService {

    private static final Logger log = LoggerFactory.getLogger(CoordinatorService.class);

    /** Context remaining threshold by complexity for renew vs regenerate decision */
    private static final Map<TaskComplexity, Long> COMPLEXITY_THRESHOLD = Map.of(
        TaskComplexity.LOW, 10_000L,
        TaskComplexity.MEDIUM, 30_000L,
        TaskComplexity.HIGH, 60_000L
    );

    private final ScratchpadManager scratchpadManager;
    private final StreamingClient llmClient;
    private final ToolExecutor toolExecutor;
    private final Map<String, WorkerAgent> activeWorkers = new ConcurrentHashMap<>();

    public CoordinatorService(ScratchpadManager scratchpadManager, StreamingClient llmClient, ToolExecutor toolExecutor) {
        this.scratchpadManager = scratchpadManager;
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
    }

    /**
     * Generates the coordinator system prompt for multi-worker orchestration.
     */
    public String buildCoordinatorPrompt(CoordinatorContext ctx) {
        var sb = new StringBuilder();
        sb.append("You are a coordinator managing multiple worker agents.\n\n");
        sb.append("## Your Role\n");
        sb.append("- Decompose complex tasks into subtasks\n");
        sb.append("- Assign workers to research, implementation, and verification phases\n");
        sb.append("- Manage worker concurrency (parallel vs sequential)\n");
        sb.append("- Use scratchpad directory for cross-worker knowledge sharing\n\n");

        sb.append("## Worker Management\n");
        sb.append("- Decide: continue existing worker vs spawn fresh\n");
        sb.append("- Simple mode workers: Bash/Read/Edit only\n");
        sb.append("- Full mode workers: all tools available\n");
        sb.append("- Handle task notifications via SendMessageTool\n\n");

        sb.append("## Concurrency Patterns\n");
        sb.append("- Research phase: parallel workers for independent investigation\n");
        sb.append("- Implementation phase: sequential or parallel based on dependencies\n");
        sb.append("- Verification phase: parallel verification of independent components\n\n");

        sb.append("## Available Tools\n");
        for (String tool : ctx.availableTools()) {
            sb.append("- ").append(tool).append("\n");
        }
        sb.append("\n");

        if (!ctx.mcpServers().isEmpty()) {
            sb.append("## MCP Servers\n");
            for (String server : ctx.mcpServers()) {
                sb.append("- ").append(server).append("\n");
            }
            sb.append("\n");
        }

        if (ctx.scratchpadDir() != null) {
            sb.append("## Scratchpad\n");
            sb.append("Directory: ").append(ctx.scratchpadDir()).append("\n");
            sb.append("Use this directory for cross-worker knowledge sharing.\n\n");
        }

        sb.append("## Max Workers: ").append(ctx.maxWorkers()).append("\n");

        return sb.toString();
    }

    /**
     * Executes a worker with the given configuration.
     */
    public WorkerResult executeWorker(WorkerConfig config) {
        WorkerAgent agent = new WorkerAgentImpl(config.workerId(), llmClient, toolExecutor);
        activeWorkers.put(config.workerId(), agent);
        try {
            return agent.execute(config);
        } finally {
            activeWorkers.remove(config.workerId());
        }
    }

    /**
     * Decides whether to continue (renew) an existing worker or regenerate a fresh one.
     * Simple heuristic based on context remaining and progress.
     */
    public boolean shouldContinueWorker(WorkerState state, TaskComplexity complexity) {
        long threshold = COMPLEXITY_THRESHOLD.getOrDefault(complexity, 30_000L);
        return state.contextRemaining() > threshold && state.progress() > 0.5;
    }

    /**
     * Cancels a specific worker.
     */
    public void cancelWorker(String workerId) {
        WorkerAgent agent = activeWorkers.get(workerId);
        if (agent != null) {
            agent.cancel();
            activeWorkers.remove(workerId);
        }
    }

    /**
     * Returns the number of active workers.
     */
    public int activeWorkerCount() {
        return activeWorkers.size();
    }

    /**
     * Detects whether coordinator mode should be active based on session configuration.
     */
    public static boolean isCoordinatorMode(SessionMode sessionMode) {
        return sessionMode == SessionMode.COORDINATOR;
    }

    /**
     * Matches session mode to determine the appropriate execution mode.
     */
    public static SessionMode detectSessionMode(Optional<String> modeHint) {
        if (modeHint.isEmpty()) {
            return SessionMode.STANDARD;
        }
        return switch (modeHint.get().toLowerCase()) {
            case "coordinator", "coord" -> SessionMode.COORDINATOR;
            case "agent" -> SessionMode.AGENT;
            default -> SessionMode.STANDARD;
        };
    }

    public ScratchpadManager getScratchpadManager() {
        return scratchpadManager;
    }

    public enum TaskComplexity {
        LOW, MEDIUM, HIGH
    }

    public enum SessionMode {
        STANDARD,
        AGENT,
        COORDINATOR
    }
}
