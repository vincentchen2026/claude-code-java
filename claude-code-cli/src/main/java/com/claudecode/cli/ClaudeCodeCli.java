package com.claudecode.cli;

import com.claudecode.core.engine.AbortController;
import com.claudecode.core.engine.CostCalculator;
import com.claudecode.core.engine.QueryEngine;
import com.claudecode.core.engine.QueryEngineConfig;
import com.claudecode.core.engine.StreamingClient;
import com.claudecode.core.engine.SubmitOptions;
import com.claudecode.core.message.SDKMessage;
import com.claudecode.core.message.AssistantMessage;
import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.TextBlock;
import com.claudecode.core.message.Usage;
import com.claudecode.tools.*;
import com.claudecode.services.lsp.LSPTool;
import com.claudecode.services.skills.SkillToolProvider;
import com.claudecode.services.tasks.TaskToolProvider;
import com.claudecode.mcp.McpToolProvider;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Main CLI entry point for Claude Code Java.
 * Uses Picocli for argument parsing and command dispatch.
 */
@Command(
    name = "claude",
    version = "claude-code-java 0.1.0",
    description = "Claude Code — AI-powered CLI tool for software development",
    mixinStandardHelpOptions = true
)
public class ClaudeCodeCli implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCodeCli.class);

    @Option(names = {"--model", "-m"}, description = "Model to use (default: claude-sonnet-4-20250514)")
    private String model;

    @Option(names = {"--api-key"}, description = "Anthropic API key (overrides env/config)")
    private String apiKey;

    @Option(names = {"--system-prompt"}, description = "Custom system prompt")
    private String systemPrompt;

    @Option(names = {"--max-tokens"}, description = "Maximum tokens per response", defaultValue = "16384")
    private int maxTokens;

    @Option(names = {"--max-turns"}, description = "Maximum conversation turns", defaultValue = "100")
    private int maxTurns;

    @Option(names = {"--max-budget-usd"}, description = "Maximum USD budget for the session", defaultValue = "-1.0")
    private double maxBudgetUsd;

    @Option(names = {"--output-format"}, description = "Output format: text or json", defaultValue = "text")
    private String outputFormat;

    @Option(names = {"--base-url"}, description = "Custom API base URL (e.g., https://api.minimaxi.com/anthropic)")
    private String baseUrl;

    @Option(names = {"--no-interactive"}, description = "Disable interactive REPL mode (process single prompt and exit)")
    private boolean noInteractive;

    @Parameters(index = "0", arity = "0..1", description = "Initial prompt (optional)")
    private String initialPrompt;

    // Visible for testing — allows injecting a custom StreamingClient
    private StreamingClient streamingClientOverride;

    // Visible for testing — allows injecting custom I/O
    private PrintWriter outputWriter;

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ClaudeCodeCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try {
            PrintWriter out = outputWriter != null ? outputWriter : new PrintWriter(System.out, true);

            // Resolve model
            String resolvedModel = model != null ? model : "claude-sonnet-4-20250514";

            // Create streaming client (skip config loading if override is set)
            StreamingClient client;
            if (streamingClientOverride != null) {
                client = streamingClientOverride;
            } else {
                ConfigLoader configLoader = new ConfigLoader();
                String resolvedApiKey = configLoader.resolveApiKey(apiKey);
                String resolvedBaseUrl = baseUrl != null ? baseUrl : configLoader.resolveBaseUrl();
                client = configLoader.createStreamingClient(resolvedApiKey, resolvedModel, resolvedBaseUrl);
            }

            // Set up tool registry with all tool implementations
            ToolRegistry toolRegistry = new ToolRegistry();
            
            // File operations
            toolRegistry.register(new BashTool());
            toolRegistry.register(new FileReadTool());
            toolRegistry.register(new FileWriteTool());
            toolRegistry.register(new FileEditTool());
            toolRegistry.register(new GlobTool());
            toolRegistry.register(new GrepTool());
            toolRegistry.register(new TodoWriteTool());
            toolRegistry.register(new NotebookEditTool());
            toolRegistry.register(new SendUserFileTool());
            
            // Web tools
            toolRegistry.register(new WebFetchTool());
            toolRegistry.register(new WebSearchTool());
            toolRegistry.register(new WebBrowserTool());
            
            // Agent & SubAgent
            toolRegistry.register(new AgentTool(client, toolRegistry));
            
            // Communication
            toolRegistry.register(new AskUserQuestionTool());
            toolRegistry.register(new SendMessageTool());
            
            // MCP tools
            McpToolProvider mcpToolProvider = new McpToolProvider();
            mcpToolProvider.initialize(Path.of(System.getProperty("user.dir")), toolRegistry);
            
            // Skill tools
            SkillToolProvider skillToolProvider = new SkillToolProvider();
            skillToolProvider.initialize(Path.of(System.getProperty("user.dir")), toolRegistry);
            
            // Task tools
            TaskToolProvider taskToolProvider = new TaskToolProvider();
            taskToolProvider.initialize(toolRegistry);
            
            // Team
            toolRegistry.register(new TeamCreateTool());
            toolRegistry.register(new TeamDeleteTool());
            
            // LSP
            toolRegistry.register(new LSPTool());
            
            // Planning & Worktree
            toolRegistry.register(new EnterPlanModeTool());
            toolRegistry.register(new ExitPlanModeTool());
            toolRegistry.register(new EnterWorktreeTool());
            toolRegistry.register(new ExitWorktreeTool());
            toolRegistry.register(new VerifyPlanExecutionTool());
            
            // Utility tools
            toolRegistry.register(new BriefTool());
            toolRegistry.register(new ConfigTool());
            toolRegistry.register(new FeatureGateTool("default", "default", "Default feature gate"));
            toolRegistry.register(new MonitorTool());
            toolRegistry.register(new OverflowTestTool());
            toolRegistry.register(new PowerShellTool());
            toolRegistry.register(new REPLTool());
            toolRegistry.register(new RemoteTriggerTool());
            toolRegistry.register(new ReviewArtifactTool());
            toolRegistry.register(new ScheduleCronTool());
            toolRegistry.register(new SleepTool());
            toolRegistry.register(new SnipTool());
            toolRegistry.register(new SyntheticOutputTool());
            toolRegistry.register(new TerminalCaptureTool());
            toolRegistry.register(new ToolSearchTool(toolRegistry));
            toolRegistry.register(new TungstenTool());
            toolRegistry.register(new WorkflowTool());

            // Build engine config
            QueryEngineConfig config = QueryEngineConfig.builder()
                .llmClient(client)
                .model(resolvedModel)
                .systemPrompt(systemPrompt != null ? systemPrompt : QueryEngine.FULL_SYSTEM_PROMPT)
                .maxTokens(maxTokens)
                .maxTurns(maxTurns)
                .maxBudgetUsd(maxBudgetUsd)
                .toolExecutor(toolRegistry)
                .build();

            QueryEngine engine = new QueryEngine(config);

            // If initial prompt provided in non-interactive mode, process and exit
            if (initialPrompt != null && noInteractive) {
                return processSinglePrompt(engine, initialPrompt, out);
            }

            // If initial prompt provided in interactive mode, process it first then enter REPL
            if (initialPrompt != null) {
                processPrompt(engine, initialPrompt, out);
            }

            // If non-interactive with no prompt, nothing to do
            if (noInteractive) {
                return 0;
            }

            // Enter REPL loop
            ReplLoop repl = new ReplLoop(engine, out, 
                new java.io.BufferedReader(new java.io.InputStreamReader(System.in)),
                resolvedModel, System.getProperty("user.dir"));
            return repl.run();

        } catch (ConfigLoader.ConfigException e) {
            System.err.println("Configuration error: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            log.error("Fatal error", e);
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private int processSinglePrompt(QueryEngine engine, String prompt, PrintWriter out) {
        processPrompt(engine, prompt, out);
        return 0;
    }

    /**
     * Processes a single prompt through the engine and prints the response.
     * Renders: thinking → tool calls with timing → tool results → text → token stats.
     */
    // ANSI constants
    private static final String RST = "\u001B[0m";
    private static final String BLD = "\u001B[1m";
    private static final String DM = "\u001B[2m";
    private static final String GRN = "\u001B[32m";
    private static final String RD = "\u001B[31m";
    private static final String CYN = "\u001B[36m";

    static void processPrompt(QueryEngine engine, String prompt, PrintWriter out) {
        long startTime = System.currentTimeMillis();
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        String model = engine.getConfig().model();
        // Track per-tool start times for execution duration display
        Map<String, Long> toolStartTimes = new HashMap<>();

        Iterator<SDKMessage> messages = engine.submitMessage(prompt, SubmitOptions.DEFAULT);
        boolean streamedAny = false;
        boolean inThinking = false;
        while (messages.hasNext()) {
            SDKMessage msg = messages.next();
            if (msg instanceof SDKMessage.StreamEvent se) {
                String et = se.eventType();
                Object data = se.data();
                switch (et) {
                    case "thinking_delta" -> {
                        if (data instanceof String t) {
                            if (!inThinking) { out.print(DM + "💭 "); inThinking = true; }
                            out.print(t); out.flush();
                        }
                    }
                    case "content_block_delta" -> {
                        if (data instanceof String t) {
                            if (inThinking) { out.println(RST + "\n"); inThinking = false; }
                            out.print(t); out.flush(); streamedAny = true;
                        }
                    }
                    case "tool_call_start" -> {
                        if (inThinking) { out.println(RST); inThinking = false; }
                        if (streamedAny) { out.println(); streamedAny = false; }
                        if (data instanceof String info) {
                            String[] p = info.split("\\|", 3);
                            String name = p.length > 0 ? p[0] : "?";
                            String rawInput = p.length > 2 ? p[2] : "";
                            String inputSummary = extractToolInputSummary(name, rawInput);
                            out.println(CYN + "⏺ " + BLD + name + RST +
                                (inputSummary.isEmpty() ? "" : "  " + inputSummary));
                            toolStartTimes.put(name, System.currentTimeMillis());
                        }
                    }
                    case "tool_result_success" -> {
                        if (data instanceof String info) {
                            String[] p = info.split("\\|", 2);
                            String toolName = p[0];
                            String output = p.length > 1 ? p[1] : "";
                            if (!output.isEmpty()) {
                                long lineCount = output.lines().count();
                                out.println(DM + "  stdout (" + lineCount + " line" + (lineCount != 1 ? "s" : "") + ")" + RST);
                            }
                            long elapsed = System.currentTimeMillis() - toolStartTimes.getOrDefault(toolName, System.currentTimeMillis());
                            out.println(GRN + "  ✓ " + formatElapsed(elapsed) + RST);
                        }
                    }
                    case "tool_result_error" -> {
                        if (data instanceof String info) {
                            String[] p = info.split("\\|", 2);
                            String err = p.length > 1 ? p[1] : "Unknown error";
                            out.println(RD + "  ✗ " + p[0] + ": " + err + RST);
                        }
                    }
                    default -> { }
                }
            } else if (msg instanceof SDKMessage.Assistant assistant) {
                if (inThinking) { out.println(RST); inThinking = false; }
                if (streamedAny) { out.println(); out.flush(); } else { printAssistantMessage(assistant, out); }
                streamedAny = false;
                // Track token usage from assistant messages
                if (assistant.usage() != null) {
                    totalInputTokens += assistant.usage().inputTokens();
                    totalOutputTokens += assistant.usage().outputTokens();
                }
            } else if (msg instanceof SDKMessage.Result result) {
                // Print token usage summary after the response completes
                long elapsed = System.currentTimeMillis() - startTime;
                double cost = CostCalculator.forModel(model).calculateCost(
                    new Usage(totalInputTokens, totalOutputTokens, 0, 0));
                out.println();
                out.println(DM + "  ─ " + formatTokens(totalInputTokens) + " input · "
                    + formatTokens(totalOutputTokens) + " output · $"
                    + String.format("%.4f", cost) + " · "
                    + formatElapsed(elapsed) + RST);
            } else if (msg instanceof SDKMessage.Error error) {
                if (inThinking) { out.println(RST); inThinking = false; }
                if (streamedAny) { out.println(); }
                out.println(RD + "Error: " + error.exception().getMessage() + RST);
                streamedAny = false;
            }
        }
    }

    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * Formats a token count for display, using "k" suffix for thousands.
     */
    static String formatTokens(long tokens) {
        if (tokens >= 1000) return String.format("%.1fk", tokens / 1000.0);
        return String.valueOf(tokens);
    }

    /**
     * Formats elapsed milliseconds as a human-readable duration string.
     */
    static String formatElapsed(long millis) {
        if (millis < 1000) return millis + "ms";
        return String.format("%.1fs", millis / 1000.0);
    }

    /**
     * Extracts a human-readable input summary for a tool call,
     * showing the most relevant field instead of raw JSON.
     */
    static String extractToolInputSummary(String toolName, String rawInput) {
        if (rawInput == null || rawInput.isEmpty()) return "";
        try {
            JsonNode node = com.claudecode.utils.JsonUtils.getMapper().readTree(rawInput);
            return switch (toolName) {
                case "Bash" -> getJsonField(node, "command");
                case "Read", "FileRead" -> getJsonField(node, "file_path");
                case "Write", "FileWrite" -> getJsonField(node, "file_path");
                case "Edit", "FileEdit" -> getJsonField(node, "file_path");
                case "Grep", "GrepTool" -> getJsonField(node, "pattern");
                case "Glob", "GlobTool" -> getJsonField(node, "pattern");
                default -> trunc(rawInput, 80);
            };
        } catch (Exception e) {
            return trunc(rawInput, 80);
        }
    }

    private static String getJsonField(JsonNode node, String field) {
        if (node != null && node.has(field)) {
            JsonNode val = node.get(field);
            return val.isTextual() ? val.asText() : val.toString();
        }
        return "";
    }

    /**
     * Extracts and prints text from an assistant message.
     */
    static void printAssistantMessage(SDKMessage.Assistant assistant, PrintWriter out) {
        if (assistant.message() != null && assistant.message().message() != null) {
            for (ContentBlock block : assistant.message().message().content()) {
                if (block instanceof TextBlock textBlock) {
                    out.println(textBlock.text());
                }
            }
        }
    }
    // -- Test support methods --

    void setStreamingClientOverride(StreamingClient client) {
        this.streamingClientOverride = client;
    }

    void setOutputWriter(PrintWriter writer) {
        this.outputWriter = writer;
    }

    String getModel() { return model; }
    String getApiKey() { return apiKey; }
    String getSystemPrompt() { return systemPrompt; }
    int getMaxTokens() { return maxTokens; }
    int getMaxTurns() { return maxTurns; }
    double getMaxBudgetUsd() { return maxBudgetUsd; }
    String getOutputFormat() { return outputFormat; }
    boolean isNoInteractive() { return noInteractive; }
    String getInitialPrompt() { return initialPrompt; }
}
