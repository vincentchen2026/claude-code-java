package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * ExitPlanModeTool — exit plan mode.
 * Clears the flag and returns confirmation.
 */
public class ExitPlanModeTool extends Tool<JsonNode, String> {

    @Override public String name() { return "ExitPlanMode"; }

    @Override public String description() { return "Exit plan mode"; }

    @Override public JsonNode inputSchema() { return createObjectSchema(); }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        EnterPlanModeTool.resetPlanMode();
        return "Plan mode deactivated. All tools are now available.";
    }
}
