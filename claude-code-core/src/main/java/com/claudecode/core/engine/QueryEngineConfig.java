package com.claudecode.core.engine;

import com.claudecode.core.message.Message;

import java.util.List;
import java.util.Map;

/**
 * Configuration for the QueryEngine.
 * Uses a builder pattern for construction.
 */
public final class QueryEngineConfig {

    private final StreamingClient llmClient;
    private volatile String model;
    private final String systemPrompt;
    private final int maxTokens;
    private final int maxTurns;
    private final double maxBudgetUsd;
    private final List<Message> initialMessages;
    private final AbortController abortController;
    private final List<String> tools;
    private final Map<String, String> readFileCache;
    private final ToolExecutor toolExecutor;
    private final String workingDirectory;
    private final List<String> mcpServers;

    private QueryEngineConfig(Builder builder) {
        this.llmClient = builder.llmClient;
        this.model = builder.model;
        this.systemPrompt = builder.systemPrompt;
        this.maxTokens = builder.maxTokens;
        this.maxTurns = builder.maxTurns;
        this.maxBudgetUsd = builder.maxBudgetUsd;
        this.initialMessages = builder.initialMessages != null
            ? List.copyOf(builder.initialMessages) : List.of();
        this.abortController = builder.abortController;
        this.tools = builder.tools != null
            ? List.copyOf(builder.tools) : List.of();
        this.readFileCache = builder.readFileCache != null
            ? Map.copyOf(builder.readFileCache) : Map.of();
        this.toolExecutor = builder.toolExecutor != null
            ? builder.toolExecutor : new NoOpToolExecutor();
        this.workingDirectory = builder.workingDirectory != null
            ? builder.workingDirectory : System.getProperty("user.dir");
        this.mcpServers = builder.mcpServers != null
            ? List.copyOf(builder.mcpServers) : List.of();
    }

    public StreamingClient llmClient() { return llmClient; }
    public String model() { return model; }
    public String systemPrompt() { return systemPrompt; }
    public int maxTokens() { return maxTokens; }
    public int maxTurns() { return maxTurns; }
    public double maxBudgetUsd() { return maxBudgetUsd; }
    public List<Message> initialMessages() { return initialMessages; }
    public AbortController abortController() { return abortController; }
    public List<String> tools() { return tools; }
    public Map<String, String> readFileCache() { return readFileCache; }
    public ToolExecutor toolExecutor() { return toolExecutor; }
    public String workingDirectory() { return workingDirectory; }
    public List<String> mcpServers() { return mcpServers; }

    /**
     * Allows the model to be changed at runtime (e.g., via /model command).
     */
    public void setUserSpecifiedModel(String model) {
        this.model = model;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private StreamingClient llmClient;
        private String model = "claude-sonnet-4-20250514";
        private String systemPrompt = "";
        private int maxTokens = 16384;
        private int maxTurns = 100;
        private double maxBudgetUsd = -1.0;
        private List<Message> initialMessages;
        private AbortController abortController;
        private List<String> tools;
        private Map<String, String> readFileCache;
        private ToolExecutor toolExecutor;
        private String workingDirectory;
        private List<String> mcpServers;

        private Builder() {}

        public Builder llmClient(StreamingClient llmClient) {
            this.llmClient = llmClient;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder maxTurns(int maxTurns) {
            this.maxTurns = maxTurns;
            return this;
        }

        public Builder maxBudgetUsd(double maxBudgetUsd) {
            this.maxBudgetUsd = maxBudgetUsd;
            return this;
        }

        public Builder initialMessages(List<Message> initialMessages) {
            this.initialMessages = initialMessages;
            return this;
        }

        public Builder abortController(AbortController abortController) {
            this.abortController = abortController;
            return this;
        }

        public Builder tools(List<String> tools) {
            this.tools = tools;
            return this;
        }

        public Builder readFileCache(Map<String, String> readFileCache) {
            this.readFileCache = readFileCache;
            return this;
        }

        public Builder toolExecutor(ToolExecutor toolExecutor) {
            this.toolExecutor = toolExecutor;
            return this;
        }

        public Builder workingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Builder mcpServers(List<String> mcpServers) {
            this.mcpServers = mcpServers;
            return this;
        }

        public QueryEngineConfig build() {
            if (llmClient == null) {
                throw new IllegalStateException("llmClient is required");
            }
            return new QueryEngineConfig(this);
        }
    }
}
