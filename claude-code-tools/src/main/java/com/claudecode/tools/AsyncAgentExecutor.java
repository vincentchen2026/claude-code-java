package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncAgentExecutor {

    private static final Map<String, AgentTask> ACTIVE_AGENTS = new ConcurrentHashMap<>();

    private final SubAgentRunner runner;

    public AsyncAgentExecutor(SubAgentRunner runner) {
        this.runner = runner != null ? runner : new DefaultSubAgentRunner();
    }

    public AsyncAgentExecutor() {
        this(new DefaultSubAgentRunner());
    }

    public AgentTask executeAsync(AgentRequest request) {
        String taskId = "agent_" + System.currentTimeMillis();
        
        CompletableFuture<AgentResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                return runner.run(request);
            } catch (Exception e) {
                return AgentResult.error(e.getMessage());
            }
        });

        AgentTask task = new AgentTask(taskId, request.prompt(), future, request, Instant.now(), AgentStatus.RUNNING);
        ACTIVE_AGENTS.put(taskId, task);

        future.thenAccept(result -> {
            AgentTask current = ACTIVE_AGENTS.get(taskId);
            if (current != null) {
                ACTIVE_AGENTS.put(taskId, new AgentTask(
                    taskId,
                    current.prompt(),
                    future,
                    current.request(),
                    current.startedAt(),
                    result.success() ? AgentStatus.COMPLETED : AgentStatus.FAILED
                ));
            }
        });

        return task;
    }

    public AgentTask getTask(String taskId) {
        return ACTIVE_AGENTS.get(taskId);
    }

    public boolean cancelTask(String taskId) {
        AgentTask task = ACTIVE_AGENTS.get(taskId);
        if (task != null && task.status() == AgentStatus.RUNNING) {
            boolean cancelled = task.future().cancel(true);
            if (cancelled) {
                ACTIVE_AGENTS.put(taskId, new AgentTask(
                    taskId,
                    task.prompt(),
                    task.future(),
                    task.request(),
                    task.startedAt(),
                    AgentStatus.CANCELLED
                ));
            }
            return cancelled;
        }
        return false;
    }

    public boolean convertToBackground(String taskId) {
        AgentTask task = ACTIVE_AGENTS.get(taskId);
        if (task != null && task.status() == AgentStatus.RUNNING) {
            ACTIVE_AGENTS.put(taskId, new AgentTask(
                taskId,
                task.prompt(),
                task.future(),
                task.request(),
                task.startedAt(),
                AgentStatus.BACKGROUND
            ));
            return true;
        }
        return false;
    }

    public List<AgentTask> getActiveTasks() {
        return ACTIVE_AGENTS.values().stream()
            .filter(t -> t.status() == AgentStatus.RUNNING || t.status() == AgentStatus.BACKGROUND)
            .toList();
    }

    public List<AgentTask> getAllTasks() {
        return List.copyOf(ACTIVE_AGENTS.values());
    }

    public static int getActiveCount() {
        return ACTIVE_AGENTS.size();
    }

    public enum AgentStatus {
        PENDING,
        RUNNING,
        BACKGROUND,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public record AgentRequest(
        String prompt,
        List<String> tools,
        String model,
        Double budgetUsd,
        String worktreeBranch,
        String teamId,
        String remoteAgentId,
        Map<String, String> env
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String prompt = "";
            private List<String> tools = List.of();
            private String model = null;
            private Double budgetUsd = null;
            private String worktreeBranch = null;
            private String teamId = null;
            private String remoteAgentId = null;
            private Map<String, String> env = Map.of();

            public Builder prompt(String prompt) { this.prompt = prompt; return this; }
            public Builder tools(List<String> tools) { this.tools = tools; return this; }
            public Builder model(String model) { this.model = model; return this; }
            public Builder budgetUsd(Double budgetUsd) { this.budgetUsd = budgetUsd; return this; }
            public Builder worktreeBranch(String worktreeBranch) { this.worktreeBranch = worktreeBranch; return this; }
            public Builder teamId(String teamId) { this.teamId = teamId; return this; }
            public Builder remoteAgentId(String remoteAgentId) { this.remoteAgentId = remoteAgentId; return this; }
            public Builder env(Map<String, String> env) { this.env = env; return this; }
            public AgentRequest build() {
                return new AgentRequest(prompt, tools, model, budgetUsd, worktreeBranch, teamId, remoteAgentId, env);
            }
        }
    }

    public record AgentResult(
        String output,
        boolean success,
        int tokensUsed,
        double costUsd,
        String error
    ) {
        public static AgentResult success(String output, int tokensUsed, double costUsd) {
            return new AgentResult(output, true, tokensUsed, costUsd, null);
        }

        public static AgentResult error(String error) {
            return new AgentResult(null, false, 0, 0, error);
        }
    }

    public record AgentTask(
        String taskId,
        String prompt,
        CompletableFuture<AgentResult> future,
        AgentRequest request,
        Instant startedAt,
        AgentStatus status
    ) {
        public boolean isDone() {
            return future.isDone();
        }

        public boolean isSuccessful() {
            return future.isDone() && !future.isCompletedExceptionally();
        }

        public long elapsedSeconds() {
            long end = isDone() ? future.join().output() != null 
                ? System.currentTimeMillis() : System.currentTimeMillis() : System.currentTimeMillis();
            return (end - startedAt().toEpochMilli()) / 1000;
        }
    }

    @FunctionalInterface
    public interface SubAgentRunner {
        AgentResult run(AgentRequest request) throws Exception;
    }

    private static class DefaultSubAgentRunner implements SubAgentRunner {
        @Override
        public AgentResult run(AgentRequest request) throws Exception {
            Thread.sleep(100);
            return AgentResult.success("Agent completed: " + request.prompt(), 100, 0.01);
        }
    }
}