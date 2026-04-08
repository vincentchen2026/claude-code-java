package com.claudecode.services.hooks;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HookCommandTest {

    @Test
    void bashCommandHookConvenienceConstructor() {
        BashCommandHook hook = new BashCommandHook("echo hello");
        assertEquals("echo hello", hook.command());
        assertTrue(hook.ifCondition().isEmpty());
        assertFalse(hook.once());
        assertFalse(hook.async());
        assertFalse(hook.asyncRewake());
        assertEquals("bash", hook.effectiveShell());
    }

    @Test
    void bashCommandHookCustomShell() {
        BashCommandHook hook = new BashCommandHook(
            "echo hello", Optional.empty(), Optional.of("zsh"),
            Optional.empty(), Optional.empty(), false, false, false);
        assertEquals("zsh", hook.effectiveShell());
    }

    @Test
    void promptHookConvenienceConstructor() {
        PromptHook hook = new PromptHook("Check this: $ARGUMENTS");
        assertEquals("Check this: $ARGUMENTS", hook.prompt());
        assertTrue(hook.ifCondition().isEmpty());
        assertTrue(hook.model().isEmpty());
        assertFalse(hook.once());
    }

    @Test
    void httpHookConvenienceConstructor() {
        HttpHook hook = new HttpHook("https://example.com/hook");
        assertEquals("https://example.com/hook", hook.url());
        assertTrue(hook.headers().isEmpty());
        assertTrue(hook.allowedEnvVars().isEmpty());
        assertFalse(hook.once());
    }

    @Test
    void httpHookResolvedHeadersWithNoEnvVars() {
        HttpHook hook = new HttpHook(
            "https://example.com", Optional.empty(), Optional.empty(),
            Map.of("X-Token", "$MY_TOKEN"), List.of(),
            Optional.empty(), false);
        // No allowed env vars, so no interpolation
        assertEquals("$MY_TOKEN", hook.resolvedHeaders().get("X-Token"));
    }

    @Test
    void agentHookConvenienceConstructor() {
        AgentHook hook = new AgentHook("Verify this action");
        assertEquals("Verify this action", hook.prompt());
        assertTrue(hook.model().isEmpty());
        assertFalse(hook.once());
    }

    @Test
    void sealedInterfacePermitsAllTypes() {
        // Verify all four types implement HookCommand
        HookCommand bash = new BashCommandHook("cmd");
        HookCommand prompt = new PromptHook("prompt");
        HookCommand http = new HttpHook("url");
        HookCommand agent = new AgentHook("prompt");

        assertInstanceOf(BashCommandHook.class, bash);
        assertInstanceOf(PromptHook.class, prompt);
        assertInstanceOf(HttpHook.class, http);
        assertInstanceOf(AgentHook.class, agent);
    }
}
