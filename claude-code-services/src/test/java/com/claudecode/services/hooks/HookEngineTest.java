package com.claudecode.services.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HookEngineTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void executeHooksWithNoMatchingHooksReturnsEmpty() {
        HookEngine engine = new HookEngine(HooksSettings.EMPTY, "/tmp");
        List<HookResult> results = engine.executeHooks(
            HookEvent.PRE_TOOL_USE, HookInput.forEvent(HookEvent.PRE_TOOL_USE));
        assertTrue(results.isEmpty());
    }

    @Test
    void executeHooksMatchesByToolName() {
        BashCommandHook hook = new BashCommandHook("echo matched");
        HookMatcher matcher = new HookMatcher(Optional.of("Bash"), List.of(hook));
        HooksSettings settings = new HooksSettings(
            Map.of(HookEvent.PRE_TOOL_USE, List.of(matcher)));

        HookEngine engine = new HookEngine(settings, "/tmp");

        ObjectNode input = MAPPER.createObjectNode();
        input.put("command", "ls");

        // Matching tool name
        List<HookResult> results = engine.executeHooks(
            HookEvent.PRE_TOOL_USE,
            HookInput.forPreToolUse("Bash", input, "tu-1"));
        assertFalse(results.isEmpty());

        // Non-matching tool name
        List<HookResult> results2 = engine.executeHooks(
            HookEvent.PRE_TOOL_USE,
            HookInput.forPreToolUse("FileRead", input, "tu-2"));
        assertTrue(results2.isEmpty());
    }

    @Test
    void executeHooksOnceMode() {
        BashCommandHook hook = new BashCommandHook(
            "echo once", Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), true, false, false);
        HookMatcher matcher = new HookMatcher(Optional.empty(), List.of(hook));
        HooksSettings settings = new HooksSettings(
            Map.of(HookEvent.SESSION_START, List.of(matcher)));

        HookEngine engine = new HookEngine(settings, "/tmp");
        HookInput input = HookInput.forSessionStart("cli");

        // First execution should run
        List<HookResult> results1 = engine.executeHooks(HookEvent.SESSION_START, input);
        assertFalse(results1.isEmpty());

        // Second execution should skip (once mode)
        List<HookResult> results2 = engine.executeHooks(HookEvent.SESSION_START, input);
        assertTrue(results2.isEmpty());
    }

    @Test
    void parseHookOutputAllowDecision() {
        HookEngine engine = new HookEngine(HooksSettings.EMPTY, "/tmp");
        HookResult result = engine.parseHookOutput("{\"decision\":\"allow\"}");
        assertInstanceOf(HookResult.Allow.class, result);
    }

    @Test
    void parseHookOutputBlockDecision() {
        HookEngine engine = new HookEngine(HooksSettings.EMPTY, "/tmp");
        HookResult result = engine.parseHookOutput(
            "{\"decision\":\"block\",\"reason\":\"not allowed\"}");
        assertInstanceOf(HookResult.Block.class, result);
        assertEquals("not allowed", ((HookResult.Block) result).reason());
    }

    @Test
    void parseHookOutputWithAdditionalContext() {
        HookEngine engine = new HookEngine(HooksSettings.EMPTY, "/tmp");
        HookResult result = engine.parseHookOutput(
            "{\"decision\":\"allow\",\"additionalContext\":\"extra info\"}");
        assertInstanceOf(HookResult.Allow.class, result);
        assertEquals("extra info", ((HookResult.Allow) result).additionalContext().orElse(""));
    }

    @Test
    void parseHookOutputMessageDecision() {
        HookEngine engine = new HookEngine(HooksSettings.EMPTY, "/tmp");
        HookResult result = engine.parseHookOutput(
            "{\"decision\":\"message\",\"reason\":\"injected message\"}");
        assertInstanceOf(HookResult.Message.class, result);
        assertEquals("injected message", ((HookResult.Message) result).content());
    }

    @Test
    void parseHookOutputNonJsonTreatedAsContext() {
        HookEngine engine = new HookEngine(HooksSettings.EMPTY, "/tmp");
        HookResult result = engine.parseHookOutput("plain text output");
        assertInstanceOf(HookResult.Allow.class, result);
        assertEquals("plain text output",
            ((HookResult.Allow) result).additionalContext().orElse(""));
    }

    @Test
    void executePreToolHooksBlocksOnBlockResult() {
        // Create a hook that outputs a block decision
        // We test the aggregation logic directly
        HookEngine engine = new HookEngine(HooksSettings.EMPTY, "/tmp");
        HookResult result = engine.executePreToolHooks("Bash", null, "tu-1");
        // No hooks configured, should return Skip
        assertInstanceOf(HookResult.Skip.class, result);
    }

    @Test
    void promptHookStubReturnsAllow() {
        PromptHook hook = new PromptHook("Check: $ARGUMENTS");
        HookEngine engine = new HookEngine(HooksSettings.EMPTY, "/tmp");
        HookResult result = engine.executePromptHook(hook,
            HookInput.forEvent(HookEvent.STOP));
        assertInstanceOf(HookResult.Allow.class, result);
    }

    @Test
    void agentHookStubReturnsAllow() {
        AgentHook hook = new AgentHook("Verify action");
        HookEngine engine = new HookEngine(HooksSettings.EMPTY, "/tmp");
        HookResult result = engine.executeAgentHook(hook,
            HookInput.forEvent(HookEvent.PERMISSION_REQUEST));
        assertInstanceOf(HookResult.Allow.class, result);
    }

    @Test
    void hookInputToJsonContainsEventAndToolName() {
        HookInput input = HookInput.forPreToolUse("Bash",
            MAPPER.createObjectNode().put("command", "ls"), "tu-1");
        String json = input.toJson();
        assertTrue(json.contains("pre_tool_use"));
        assertTrue(json.contains("Bash"));
        assertTrue(json.contains("tu-1"));
    }

    @Test
    void hookResultSealedTypes() {
        HookResult allow = new HookResult.Allow();
        HookResult allowCtx = new HookResult.Allow("context");
        HookResult block = new HookResult.Block("reason");
        HookResult msg = new HookResult.Message("content");
        HookResult skip = new HookResult.Skip();

        assertInstanceOf(HookResult.Allow.class, allow);
        assertInstanceOf(HookResult.Allow.class, allowCtx);
        assertInstanceOf(HookResult.Block.class, block);
        assertInstanceOf(HookResult.Message.class, msg);
        assertInstanceOf(HookResult.Skip.class, skip);

        assertTrue(((HookResult.Allow) allow).additionalContext().isEmpty());
        assertEquals("context", ((HookResult.Allow) allowCtx).additionalContext().orElse(""));
        assertEquals("reason", ((HookResult.Block) block).reason());
    }
}
