package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionMode;

import java.util.List;
import java.util.Optional;

/**
 * Request parameters for creating a sub-agent.
 *
 * @param prompt           the task prompt for the sub-agent
 * @param tools            list of tool names the sub-agent can use
 * @param budgetUsd        maximum USD budget for the sub-agent
 * @param parentContext    the parent's execution context (for permission inheritance)
 * @param model            optional model override for the sub-agent
 * @param permissionMode   optional permission mode override
 * @param worktreeBranch   optional git worktree branch for isolation
 * @param async            whether to run asynchronously
 * @param teamId           optional team ID for team agents
 * @param remoteAgentId    optional remote agent ID for remote agents
 * @param fork             whether this is a fork subagent
 * @param mcpServerIds     list of MCP server IDs to verify/expose to sub-agent
 * @param progressCallback optional callback for progress updates
 */
public record SubAgentRequest(
    String prompt,
    List<String> tools,
    double budgetUsd,
    ToolExecutionContext parentContext,
    Optional<String> model,
    Optional<PermissionMode> permissionMode,
    Optional<String> worktreeBranch,
    boolean async,
    Optional<String> teamId,
    Optional<String> remoteAgentId,
    boolean fork,
    List<String> mcpServerIds,
    ProgressCallback progressCallback
) {

    public record ProgressCallback(String status, double progressPercent) {}

    public static Builder builder() {
        return new Builder();
    }

    /** Fraction of parent budget allocated to sub-agent (default 0.25). */
    public static final double DEFAULT_BUDGET_FRACTION = 0.25;

    public static class Builder {
        private String prompt = "";
        private List<String> tools = List.of();
        private double budgetUsd = DEFAULT_BUDGET_FRACTION;
        private ToolExecutionContext parentContext;
        private Optional<String> model = Optional.empty();
        private Optional<PermissionMode> permissionMode = Optional.empty();
        private Optional<String> worktreeBranch = Optional.empty();
        private boolean async = false;
        private Optional<String> teamId = Optional.empty();
        private Optional<String> remoteAgentId = Optional.empty();
        private boolean fork = false;
        private List<String> mcpServerIds = List.of();
        private ProgressCallback progressCallback = null;

        public Builder prompt(String prompt) { this.prompt = prompt; return this; }
        public Builder tools(List<String> tools) { this.tools = tools; return this; }
        public Builder budgetUsd(double budgetUsd) { this.budgetUsd = budgetUsd; return this; }
        public Builder parentContext(ToolExecutionContext parentContext) { this.parentContext = parentContext; return this; }
        public Builder model(String model) { this.model = Optional.of(model); return this; }
        public Builder permissionMode(PermissionMode permissionMode) { this.permissionMode = Optional.of(permissionMode); return this; }
        public Builder worktreeBranch(String worktreeBranch) { this.worktreeBranch = Optional.of(worktreeBranch); return this; }
        public Builder async(boolean async) { this.async = async; return this; }
        public Builder teamId(String teamId) { this.teamId = Optional.of(teamId); return this; }
        public Builder remoteAgentId(String remoteAgentId) { this.remoteAgentId = Optional.of(remoteAgentId); return this; }
        public Builder fork(boolean fork) { this.fork = fork; return this; }
        public Builder mcpServerIds(List<String> mcpServerIds) { this.mcpServerIds = mcpServerIds; return this; }
        public Builder progressCallback(ProgressCallback progressCallback) { this.progressCallback = progressCallback; return this; }

        public SubAgentRequest build() {
            return new SubAgentRequest(
                prompt, tools, budgetUsd, parentContext,
                model, permissionMode, worktreeBranch, async,
                teamId, remoteAgentId, fork, mcpServerIds, progressCallback
            );
        }
    }
}
