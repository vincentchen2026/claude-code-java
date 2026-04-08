package com.claudecode.tools;

import com.claudecode.core.engine.StreamingClient;
import com.claudecode.core.engine.ToolExecutor;
import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.PermissionMode;
import com.claudecode.permissions.ToolPermissionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * AgentTool — creates a sub-QueryEngine instance with independent message history.
 * The sub-agent runs with a restricted tool set and inherits the parent's permission context.
 * Token budget is allocated as a fraction of the parent's remaining budget.
 *
 * Task 50.8 enhancements:
 * - Async execution (CompletableFuture)
 * - Git worktree isolation
 * - Remote agent support
 * - Team agent support
 * - Fork subagent
 * - MCP verification
 * - Model selection
 * - Permission mode override
 * - Progress tracking
 */
public class AgentTool extends Tool<JsonNode, String> {

    /** Default tools available to sub-agents when none specified. */
    public static final List<String> DEFAULT_SAFE_TOOLS = List.of(
        "Bash", "FileRead", "FileWrite", "FileEdit", "GlobTool", "GrepTool"
    );

    /** Fraction of parent budget allocated to sub-agent. */
    public static final double BUDGET_FRACTION = 0.25;

    private static final JsonNode SCHEMA = buildSchema();

    private final SubAgentFactory subAgentFactory;

    public AgentTool(SubAgentFactory subAgentFactory) {
        this.subAgentFactory = subAgentFactory;
    }

    public AgentTool(StreamingClient llmClient, ToolExecutor toolExecutor) {
        this.subAgentFactory = new DefaultSubAgentFactory(llmClient, toolExecutor, System.getProperty("user.dir"));
    }

    public AgentTool() {
        this.subAgentFactory = new NoOpSubAgentFactory();
    }

    @Override
    public String name() {
        return "Agent";
    }

    @Override
    public String description() {
        return "Launch a sub-agent with an independent conversation to handle a specific task";
    }

    @Override
    public JsonNode inputSchema() {
        return SCHEMA;
    }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        SubAgentRequest request = buildRequest(input, context);

        if (request.prompt() == null || request.prompt().isBlank()) {
            return "Error: prompt is required";
        }

        if (request.async()) {
            return handleAsyncExecution(request);
        }

        try {
            SubAgentResult result = subAgentFactory.runSubAgent(request);
            return formatResult(result);
        } catch (Exception e) {
            return "Error: sub-agent execution failed: " + e.getMessage();
        }
    }

    private String handleAsyncExecution(SubAgentRequest request) {
        try {
            CompletableFuture<SubAgentResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return subAgentFactory.runSubAgent(request);
                } catch (Exception e) {
                    return SubAgentResult.error(e.getMessage());
                }
            });

            if (request.progressCallback() != null) {
                return "Agent started asynchronously. Task ID will be provided upon completion.";
            }

            SubAgentResult result = future.get();
            return formatResult(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: sub-agent execution was interrupted";
        } catch (ExecutionException e) {
            return "Error: sub-agent execution failed: " + e.getCause().getMessage();
        }
    }

    private String formatResult(SubAgentResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.output());

        if (result.taskId().isPresent()) {
            sb.append("\n[Task ID: ").append(result.taskId().get()).append("]");
        }
        if (result.worktreePath().isPresent()) {
            sb.append("\n[Worktree: ").append(result.worktreePath().get()).append("]");
        }
        if (result.tokensUsed() > 0) {
            sb.append("\n[Tokens used: ").append(result.tokensUsed()).append("]");
        }
        if (result.costUsd() > 0) {
            sb.append(String.format("\n[Cost: $%.6f]", result.costUsd()));
        }

        return sb.toString().trim();
    }

    private SubAgentRequest buildRequest(JsonNode input, ToolExecutionContext context) {
        String prompt = input.has("prompt") ? input.get("prompt").asText("") : "";
        if (prompt.isBlank()) {
            prompt = input.has("task") ? input.get("task").asText("") : "";
        }

        SubAgentRequest.Builder builder = SubAgentRequest.builder()
            .prompt(prompt)
            .tools(extractToolList(input))
            .budgetUsd(extractBudget(input))
            .parentContext(context)
            .async(extractBool(input, "async", false))
            .fork(extractBool(input, "fork", false));

        if (input.has("model") && !input.get("model").isNull()) {
            builder.model(input.get("model").asText());
        }

        if (input.has("permission_mode") && !input.get("permission_mode").isNull()) {
            String modeStr = input.get("permission_mode").asText().toUpperCase();
            try {
                builder.permissionMode(PermissionMode.valueOf(modeStr));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (input.has("worktree_branch") && !input.get("worktree_branch").isNull()) {
            builder.worktreeBranch(input.get("worktree_branch").asText());
        }

        if (input.has("team_id") && !input.get("team_id").isNull()) {
            builder.teamId(input.get("team_id").asText());
        }

        if (input.has("remote_agent_id") && !input.get("remote_agent_id").isNull()) {
            builder.remoteAgentId(input.get("remote_agent_id").asText());
        }

        if (input.has("mcp_server_ids") && input.get("mcp_server_ids").isArray()) {
            List<String> serverIds = new ArrayList<>();
            for (JsonNode node : input.get("mcp_server_ids")) {
                serverIds.add(node.asText());
            }
            builder.mcpServerIds(serverIds);
        }

        return builder.build();
    }

    private List<String> extractToolList(JsonNode input) {
        if (input.has("tools") && input.get("tools").isArray()) {
            List<String> tools = new ArrayList<>();
            for (JsonNode toolNode : input.get("tools")) {
                tools.add(toolNode.asText());
            }
            return tools;
        }
        if (input.has("allowed_tools") && input.get("allowed_tools").isArray()) {
            List<String> tools = new ArrayList<>();
            for (JsonNode toolNode : input.get("allowed_tools")) {
                tools.add(toolNode.asText());
            }
            return tools;
        }
        return DEFAULT_SAFE_TOOLS;
    }

    private double extractBudget(JsonNode input) {
        if (input.has("budget_usd")) {
            return input.get("budget_usd").asDouble(BUDGET_FRACTION);
        }
        if (input.has("max_budget_usd")) {
            return input.get("max_budget_usd").asDouble(BUDGET_FRACTION);
        }
        return BUDGET_FRACTION;
    }

    private boolean extractBool(JsonNode input, String field, boolean defaultValue) {
        if (input.has(field) && !input.get(field).isNull()) {
            return input.get(field).asBoolean(defaultValue);
        }
        return defaultValue;
    }

    @Override
    public PermissionDecision checkPermissions(JsonNode input, ToolPermissionContext permCtx) {
        boolean isFork = input.has("fork") && input.get("fork").asBoolean(false);
        boolean isRemote = input.has("remote_agent_id") && !input.get("remote_agent_id").isNull();
        boolean isTeam = input.has("team_id") && !input.get("team_id").isNull();

        if (isFork || isRemote || isTeam) {
            return PermissionDecision.ASK;
        }
        return PermissionDecision.ASK;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    private static JsonNode buildSchema() {
        ObjectNode schema = mapper().createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode promptProp = properties.putObject("prompt");
        promptProp.put("type", "string");
        promptProp.put("description", "The task prompt for the sub-agent");

        ObjectNode taskProp = properties.putObject("task");
        taskProp.put("type", "string");
        taskProp.put("description", "Alias for prompt - the task description for the sub-agent");

        ObjectNode toolsProp = properties.putObject("tools");
        toolsProp.put("type", "array");
        toolsProp.putObject("items").put("type", "string");
        toolsProp.put("description", "List of tool names the sub-agent can use");

        ObjectNode allowedToolsProp = properties.putObject("allowed_tools");
        allowedToolsProp.put("type", "array");
        allowedToolsProp.putObject("items").put("type", "string");
        allowedToolsProp.put("description", "Alias for tools - list of allowed tool names");

        ObjectNode budgetProp = properties.putObject("budget_usd");
        budgetProp.put("type", "number");
        budgetProp.put("description", "Maximum USD budget for the sub-agent");

        ObjectNode maxBudgetProp = properties.putObject("max_budget_usd");
        maxBudgetProp.put("type", "number");
        maxBudgetProp.put("description", "Alias for budget_usd - maximum USD budget");

        ObjectNode modelProp = properties.putObject("model");
        modelProp.put("type", "string");
        modelProp.put("description", "Model to use for the sub-agent (overrides default)");

        ObjectNode permissionModeProp = properties.putObject("permission_mode");
        permissionModeProp.put("type", "string");
        permissionModeProp.put("description", "Permission mode: PERMIT_ALL, DENY_ALL, ASK, BASH_ONLY");

        ObjectNode worktreeBranchProp = properties.putObject("worktree_branch");
        worktreeBranchProp.put("type", "string");
        worktreeBranchProp.put("description", "Git branch name for worktree isolation");

        ObjectNode asyncProp = properties.putObject("async");
        asyncProp.put("type", "boolean");
        asyncProp.put("description", "Run sub-agent asynchronously and return task ID");
        asyncProp.put("default", false);

        ObjectNode teamIdProp = properties.putObject("team_id");
        teamIdProp.put("type", "string");
        teamIdProp.put("description", "Team ID for team agent");

        ObjectNode remoteAgentIdProp = properties.putObject("remote_agent_id");
        remoteAgentIdProp.put("type", "string");
        remoteAgentIdProp.put("description", "Remote agent ID for remote agent execution");

        ObjectNode forkProp = properties.putObject("fork");
        forkProp.put("type", "boolean");
        forkProp.put("description", "Whether this is a fork sub-agent");
        forkProp.put("default", false);

        ObjectNode mcpServerIdsProp = properties.putObject("mcp_server_ids");
        mcpServerIdsProp.put("type", "array");
        mcpServerIdsProp.putObject("items").put("type", "string");
        mcpServerIdsProp.put("description", "MCP server IDs to expose to the sub-agent");

        ArrayNode required = schema.putArray("required");
        required.add("prompt");

        return schema;
    }
}
