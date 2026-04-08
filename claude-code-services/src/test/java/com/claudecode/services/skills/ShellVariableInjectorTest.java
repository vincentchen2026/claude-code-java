package com.claudecode.services.skills;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ShellVariableInjectorTest {

    @Test
    void injectSkillDir() {
        ShellVariableInjector injector = new ShellVariableInjector();
        injector.setSkillDir(Path.of("/home/user/.claude/skills"));

        String result = injector.inject("Dir: ${CLAUDE_SKILL_DIR}/templates");
        assertTrue(result.contains("/home/user/.claude/skills"));
        assertFalse(result.contains("${CLAUDE_SKILL_DIR}"));
    }

    @Test
    void injectSessionId() {
        ShellVariableInjector injector = new ShellVariableInjector();
        injector.setSessionId("session-123");

        String result = injector.inject("Session: ${CLAUDE_SESSION_ID}");
        assertEquals("Session: session-123", result);
    }

    @Test
    void injectCustomVariable() {
        ShellVariableInjector injector = new ShellVariableInjector();
        injector.setVariable("MY_VAR", "hello");

        String result = injector.inject("Value: ${MY_VAR}");
        assertEquals("Value: hello", result);
    }

    @Test
    void injectNullContent() {
        ShellVariableInjector injector = new ShellVariableInjector();
        assertNull(injector.inject(null));
    }

    @Test
    void injectEmptyContent() {
        ShellVariableInjector injector = new ShellVariableInjector();
        assertEquals("", injector.inject(""));
    }

    @Test
    void noVariablesUnchanged() {
        ShellVariableInjector injector = new ShellVariableInjector();
        String content = "No variables here.";
        assertEquals(content, injector.inject(content));
    }

    @Test
    void nullSessionIdBecomesEmpty() {
        ShellVariableInjector injector = new ShellVariableInjector();
        injector.setSessionId(null);

        String result = injector.inject("Session: ${CLAUDE_SESSION_ID}");
        assertEquals("Session: ", result);
    }
}
