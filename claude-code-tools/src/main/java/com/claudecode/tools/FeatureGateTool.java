package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * FeatureGateTool — base for feature-gated tools (P3 stub).
 * Tools that are only enabled when a specific feature flag is active.
 */
public class FeatureGateTool extends Tool<JsonNode, String> {

    private final String featureName;
    private final String toolName;
    private final String toolDescription;

    public FeatureGateTool(String featureName, String toolName, String toolDescription) {
        this.featureName = featureName;
        this.toolName = toolName;
        this.toolDescription = toolDescription;
    }

    @Override public String name() { return toolName; }

    @Override public String description() { return toolDescription; }

    @Override public JsonNode inputSchema() { return createObjectSchema(); }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        return toolName + ": Feature '" + featureName + "' not yet implemented";
    }

    @Override
    public boolean isEnabled() {
        // Feature gate check — always disabled until feature flags are implemented
        return false;
    }

    public String featureName() {
        return featureName;
    }
}
