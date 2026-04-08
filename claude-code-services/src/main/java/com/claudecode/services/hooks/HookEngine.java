package com.claudecode.services.hooks;

import com.claudecode.api.CreateMessageRequest;
import com.claudecode.api.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Core hook execution engine.
 * Matches events to configured hooks, checks conditions, executes, and parses output.
 */
public class HookEngine {

    private static final Logger LOG = LoggerFactory.getLogger(HookEngine.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final HooksSettings settings;
    private final String workingDirectory;
    private final HttpClient httpClient;
    private LlmClient llmClient;
    private String llmModel;

    /** Tracks hooks marked as "once" that have already executed. */
    private final Set<String> executedOnceHooks = ConcurrentHashMap.newKeySet();

    public HookEngine(HooksSettings settings, String workingDirectory) {
        this(settings, workingDirectory,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public HookEngine(HooksSettings settings, String workingDirectory, HttpClient httpClient) {
        this.settings = settings != null ? settings : HooksSettings.EMPTY;
        this.workingDirectory = workingDirectory != null ? workingDirectory : System.getProperty("user.dir");
        this.httpClient = httpClient;
    }

    /**
     * Sets the LLM client for PromptHook and AgentHook execution.
     */
    public void setLlmClient(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * Sets the default LLM model for hook execution.
     */
    public void setLlmModel(String model) {
        this.llmModel = model;
    }

    /**
     * Executes all matching hooks for the given event.
     *
     * @param event the hook event type
     * @param input the hook input context
     * @return list of hook results
     */
    public List<HookResult> executeHooks(HookEvent event, HookInput input) {
        List<MatchedHook> matched = getMatchingHooks(event, input);
        if (matched.isEmpty()) {
            return List.of();
        }

        List<HookResult> results = new ArrayList<>();
        for (MatchedHook hook : matched) {
            // Check if-condition
            if (!matchesIfCondition(hook.command(), input)) {
                continue;
            }

            // Check once-mode
            if (hook.command().once()) {
                String hookKey = hookIdentity(hook);
                if (!executedOnceHooks.add(hookKey)) {
                    continue; // Already executed
                }
            }

            // Handle async mode for BashCommandHook
            if (hook.command() instanceof BashCommandHook bash && bash.async()) {
                Thread.ofVirtual().start(() -> {
                    HookResult result = executeHookCommand(hook.command(), input);
                    if (bash.asyncRewake() && result instanceof HookResult.Allow) {
                        LOG.debug("Async rewake hook completed: {}", hookKey(hook));
                    }
                });
                results.add(new HookResult.Skip());
                continue;
            }

            HookResult result = executeHookCommand(hook.command(), input);
            results.add(result);
        }

        return List.copyOf(results);
    }

    /**
     * Executes PreToolUse hooks and returns a permission decision modifier.
     *
     * @param toolName  the tool being invoked
     * @param toolInput the tool input
     * @param toolUseId the tool use ID
     * @return aggregated hook result affecting permission
     */
    public HookResult executePreToolHooks(String toolName, JsonNode toolInput, String toolUseId) {
        HookInput input = HookInput.forPreToolUse(toolName, toolInput, toolUseId);
        List<HookResult> results = executeHooks(HookEvent.PRE_TOOL_USE, input);

        // If any hook blocks, the operation is blocked
        for (HookResult result : results) {
            if (result instanceof HookResult.Block) {
                return result;
            }
        }

        // Collect additional context from Allow results
        StringBuilder context = new StringBuilder();
        for (HookResult result : results) {
            if (result instanceof HookResult.Allow allow && allow.additionalContext().isPresent()) {
                if (!context.isEmpty()) context.append("\n");
                context.append(allow.additionalContext().get());
            }
        }

        if (!context.isEmpty()) {
            return new HookResult.Allow(context.toString());
        }

        return new HookResult.Skip();
    }

    /**
     * Executes PostToolUse hooks.
     */
    public List<HookResult> executePostToolHooks(
            String toolName, JsonNode toolInput, JsonNode toolOutput, String toolUseId) {
        HookInput input = HookInput.forPostToolUse(toolName, toolInput, toolOutput, toolUseId);
        return executeHooks(HookEvent.POST_TOOL_USE, input);
    }

    // ---- Internal matching ----

    private List<MatchedHook> getMatchingHooks(HookEvent event, HookInput input) {
        List<HookMatcher> matchers = settings.getMatchers(event);
        if (matchers.isEmpty()) return List.of();

        String query = input.toolName().orElse("");
        List<MatchedHook> matched = new ArrayList<>();

        for (HookMatcher matcher : matchers) {
            if (matcher.matches(query)) {
                for (HookCommand cmd : matcher.hooks()) {
                    matched.add(new MatchedHook(matcher, cmd));
                }
            }
        }
        return matched;
    }

    private boolean matchesIfCondition(HookCommand command, HookInput input) {
        Optional<String> ifCond = command.ifCondition();
        if (ifCond.isEmpty() || ifCond.get().isBlank()) {
            return true;
        }

        String condition = ifCond.get();
        // Simple pattern matching: "ToolName(pattern)"
        // e.g., "Bash(git *)" matches tool=Bash with input containing "git ..."
        int parenStart = condition.indexOf('(');
        if (parenStart > 0 && condition.endsWith(")")) {
            String toolPattern = condition.substring(0, parenStart);
            String argPattern = condition.substring(parenStart + 1, condition.length() - 1);

            String toolName = input.toolName().orElse("");
            if (!toolPattern.equals(toolName)) return false;

            // Match argument pattern against tool input
            String inputStr = input.toolInput()
                .map(JsonNode::toString).orElse("");
            if (argPattern.contains("*")) {
                String regex = argPattern.replace("*", ".*");
                return inputStr.matches(".*" + regex + ".*");
            }
            return inputStr.contains(argPattern);
        }

        // Simple tool name match
        return condition.equals(input.toolName().orElse(""));
    }

    // ---- Hook execution by type ----

    private HookResult executeHookCommand(HookCommand command, HookInput input) {
        try {
            return switch (command) {
                case BashCommandHook cmd -> executeBashHook(cmd, input);
                case PromptHook cmd -> executePromptHook(cmd, input);
                case HttpHook cmd -> executeHttpHook(cmd, input);
                case AgentHook cmd -> executeAgentHook(cmd, input);
            };
        } catch (Exception e) {
            LOG.warn("Hook execution failed: {}", e.getMessage());
            return new HookResult.Skip();
        }
    }

    /**
     * Executes a BashCommandHook: runs subprocess, captures stdout, parses JSON.
     */
    HookResult executeBashHook(BashCommandHook cmd, HookInput input) {
        int timeout = cmd.timeoutSeconds().orElse(DEFAULT_TIMEOUT_SECONDS);
        String shell = cmd.effectiveShell();

        try {
            ProcessBuilder pb = new ProcessBuilder(shell, "-c", cmd.command());
            pb.directory(Path.of(workingDirectory).toFile());
            pb.environment().put("HOOK_INPUT", input.toJson());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Write hook input to stdin
            process.getOutputStream().write(input.toJson().getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append('\n');
                }
            }

            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return new HookResult.Skip();
            }

            int exitCode = process.exitValue();
            String output = stdout.toString().trim();

            // Exit code 2 with asyncRewake means "rewake the model"
            if (cmd.asyncRewake() && exitCode == 2) {
                return new HookResult.Message("Hook requested model rewake");
            }

            if (output.isEmpty()) {
                return exitCode == 0 ? new HookResult.Allow() : new HookResult.Skip();
            }

            return parseHookOutput(output);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.debug("Bash hook execution error: {}", e.getMessage());
            return new HookResult.Skip();
        }
    }

    /**
     * Executes a PromptHook: calls an LLM to evaluate the prompt.
     */
    HookResult executePromptHook(PromptHook cmd, HookInput input) {
        String resolvedPrompt = cmd.prompt().replace("$ARGUMENTS", input.toJson());

        if (llmClient == null) {
            LOG.debug("PromptHook: LLM client not configured, returning Allow with prompt");
            return new HookResult.Allow("PromptHook: " + resolvedPrompt);
        }

        try {
            String model = cmd.model().orElse(llmModel != null ? llmModel : "claude-sonnet-4-20250514");
            String promptText = buildPromptEvaluationPrompt(resolvedPrompt, input);
            String response = callLlm(promptText, model, cmd.timeoutSeconds().orElse(DEFAULT_TIMEOUT_SECONDS));

            if (response == null || response.isBlank()) {
                return new HookResult.Allow();
            }

            return parseHookOutput(response);

        } catch (Exception e) {
            LOG.warn("PromptHook execution failed: {}", e.getMessage());
            return new HookResult.Skip();
        }
    }

    /**
     * Executes an AgentHook: launches an agent verifier.
     */
    HookResult executeAgentHook(AgentHook cmd, HookInput input) {
        String resolvedPrompt = cmd.prompt().replace("$ARGUMENTS", input.toJson());

        if (llmClient == null) {
            LOG.debug("AgentHook: LLM client not configured, returning Allow with prompt");
            return new HookResult.Allow("AgentHook: " + resolvedPrompt);
        }

        try {
            String model = cmd.model().orElse(llmModel != null ? llmModel : "claude-sonnet-4-20250514");
            String verificationPrompt = buildAgentVerificationPrompt(resolvedPrompt, input);
            String response = callLlm(verificationPrompt, model, cmd.timeoutSeconds().orElse(DEFAULT_TIMEOUT_SECONDS));

            if (response == null || response.isBlank()) {
                return new HookResult.Allow();
            }

            return parseHookOutput(response);

        } catch (Exception e) {
            LOG.warn("AgentHook execution failed: {}", e.getMessage());
            return new HookResult.Skip();
        }
    }

    /**
     * Calls the LLM with the given prompt and returns the response.
     */
    private String callLlm(String prompt, String model, int timeoutSeconds) throws Exception {
        if (llmClient == null) {
            return null;
        }

        CreateMessageRequest request = CreateMessageRequest.builder()
            .model(model)
            .maxTokens(1024)
            .systemPrompt("You are a hook evaluation assistant. Evaluate the following prompt and return a JSON response with: { \"decision\": \"allow\" or \"block\", \"reason\": \"explanation\" }")
            .messages(List.of(new CreateMessageRequest.RequestMessage("user", prompt)))
            .stream(false)
            .build();

        var response = llmClient.createMessage(request);

        if (response.content() != null && !response.content().isEmpty()) {
            var block = response.content().get(0);
            if (block instanceof com.claudecode.core.message.TextBlock tb) {
                return tb.text();
            }
        }
        return null;
    }

    /**
     * Builds a prompt for LLM-based hook evaluation.
     */
    private String buildPromptEvaluationPrompt(String hookPrompt, HookInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("Evaluate the following hook prompt and determine if the operation should be allowed or blocked.\n\n");
        sb.append("Hook Prompt:\n").append(hookPrompt).append("\n\n");
        sb.append("Context:\n").append(input.toJson()).append("\n\n");
        sb.append("Return a JSON object with:\n");
        sb.append("- decision: \"allow\" if the operation should proceed, \"block\" if it should be denied\n");
        sb.append("- reason: explanation for the decision\n");
        sb.append("- additionalContext: (optional) extra context to add to the conversation\n\n");
        sb.append("Example: {\"decision\":\"allow\",\"reason\":\"Operation is safe\"}");
        return sb.toString();
    }

    /**
     * Builds a prompt for agent-based verification.
     */
    private String buildAgentVerificationPrompt(String agentPrompt, HookInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are verifying an action taken by Claude Code.\n\n");
        sb.append("Verification Task:\n").append(agentPrompt).append("\n\n");
        sb.append("Action Context:\n").append(input.toJson()).append("\n\n");
        sb.append("Verify whether the action was performed correctly and safely.\n");
        sb.append("Return a JSON object with:\n");
        sb.append("- decision: \"allow\" if verified OK, \"block\" if there was a problem\n");
        sb.append("- reason: what you found during verification\n");
        sb.append("- additionalContext: (optional) correction suggestions\n\n");
        sb.append("Example: {\"decision\":\"allow\",\"reason\":\"All file edits were correct\"}");
        return sb.toString();
    }

    /**
     * Executes an HttpHook: POSTs hook input JSON to the configured URL.
     */
    HookResult executeHttpHook(HttpHook cmd, HookInput input) {
        int timeout = cmd.timeoutSeconds().orElse(DEFAULT_TIMEOUT_SECONDS);

        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(cmd.url()))
                .timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(input.toJson()));

            // Add resolved headers (with env var interpolation)
            Map<String, String> headers = cmd.resolvedHeaders();
            headers.forEach(reqBuilder::header);

            HttpResponse<String> response = httpClient.send(
                reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                LOG.debug("HTTP hook returned status {}", response.statusCode());
                return new HookResult.Skip();
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                return new HookResult.Allow();
            }

            return parseHookOutput(body);
        } catch (Exception e) {
            LOG.debug("HTTP hook execution error: {}", e.getMessage());
            return new HookResult.Skip();
        }
    }

    // ---- Output parsing ----

    /**
     * Parses hook stdout/response JSON.
     * Expected format: { "decision": "allow"|"block", "reason": "..." }
     */
    HookResult parseHookOutput(String output) {
        try {
            JsonNode node = MAPPER.readTree(output);
            String decision = node.has("decision") ? node.get("decision").asText("") : "";
            String reason = node.has("reason") ? node.get("reason").asText("") : "";

            return switch (decision.toLowerCase()) {
                case "block" -> new HookResult.Block(reason);
                case "allow" -> {
                    String context = node.has("additionalContext")
                        ? node.get("additionalContext").asText() : null;
                    yield context != null
                        ? new HookResult.Allow(context)
                        : new HookResult.Allow();
                }
                case "message" -> new HookResult.Message(reason);
                default -> new HookResult.Allow();
            };
        } catch (Exception e) {
            // Non-JSON output treated as plain text context
            return new HookResult.Allow(output);
        }
    }

    // ---- Helpers ----

    private String hookIdentity(MatchedHook hook) {
        return hook.command().getClass().getSimpleName() + ":" +
            switch (hook.command()) {
                case BashCommandHook cmd -> cmd.command();
                case PromptHook cmd -> cmd.prompt();
                case HttpHook cmd -> cmd.url();
                case AgentHook cmd -> cmd.prompt();
            };
    }

    private String hookKey(MatchedHook hook) {
        return hookIdentity(hook);
    }

    private record MatchedHook(HookMatcher matcher, HookCommand command) {}
}
