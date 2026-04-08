package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EnterPlanModeTool — switch to plan mode (read-only).
 * Sets a flag and returns confirmation.
 */
public class EnterPlanModeTool extends Tool<JsonNode, String> {

    private static final AtomicBoolean PLAN_MODE_ACTIVE = new AtomicBoolean(false);

    @Override public String name() { return "EnterPlanMode"; }

    @Override public String description() { return "Enter plan mode for structured planning"; }

    @Override public JsonNode inputSchema() { return createObjectSchema(); }

    @Override
    public String call(JsonNode input, ToolExecutionContext context) {
        PLAN_MODE_ACTIVE.set(true);
        return "Plan mode activated. All tools are now read-only.";
    }

    /** Check if plan mode is currently active. */
    public static boolean isPlanModeActive() {
        return PLAN_MODE_ACTIVE.get();
    }

    /** Reset plan mode (used by ExitPlanModeTool). */
    static void resetPlanMode() {
        PLAN_MODE_ACTIVE.set(false);
    }
}
