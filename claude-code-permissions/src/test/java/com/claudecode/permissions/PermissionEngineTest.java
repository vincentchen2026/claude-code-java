package com.claudecode.permissions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PermissionEngine.
 */
class PermissionEngineTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private PermissionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PermissionEngine();
    }

    @Test
    void defaultModeReturnsAsk() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .build();

        PermissionDecision decision = engine.evaluate("Bash", createInput("command", "ls"), ctx);
        assertEquals(PermissionDecision.ASK, decision);
    }

    @Test
    void bypassModeReturnsAllow() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.BYPASS_PERMISSIONS)
            .build();

        PermissionDecision decision = engine.evaluate("Bash", createInput("command", "rm -rf /"), ctx);
        assertEquals(PermissionDecision.ALLOW, decision);
    }

    @Test
    void planModeDeniesWriteTools() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.PLAN)
            .build();

        assertEquals(PermissionDecision.DENY, engine.evaluate("Bash", createInput("command", "rm file"), ctx));
        assertEquals(PermissionDecision.DENY, engine.evaluate("FileWrite", createInput("path", "/tmp/f"), ctx));
        assertEquals(PermissionDecision.DENY, engine.evaluate("FileEdit", createInput("path", "/tmp/f"), ctx));
    }

    @Test
    void planModeAllowsReadTools() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.PLAN)
            .build();

        assertEquals(PermissionDecision.ALLOW, engine.evaluate("FileRead", createInput("path", "/tmp/f"), ctx));
        assertEquals(PermissionDecision.ALLOW, engine.evaluate("GrepTool", createInput("command", "grep"), ctx));
    }

    @Test
    void denyRuleOverridesAllowRule() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .rules(List.of(
                PermissionRule.of("Bash", PermissionBehavior.ALLOW, RuleSource.USER_SETTINGS),
                PermissionRule.of("Bash", PermissionBehavior.DENY, RuleSource.POLICY_SETTINGS)
            ))
            .build();

        PermissionDecision decision = engine.evaluate("Bash", createInput("command", "ls"), ctx);
        assertEquals(PermissionDecision.DENY, decision);
    }

    @Test
    void allowRuleOverridesDefaultAsk() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .rules(List.of(
                PermissionRule.of("Bash", PermissionBehavior.ALLOW, RuleSource.USER_SETTINGS)
            ))
            .build();

        PermissionDecision decision = engine.evaluate("Bash", createInput("command", "ls"), ctx);
        assertEquals(PermissionDecision.ALLOW, decision);
    }

    @Test
    void wildcardRuleMatchesAnyTool() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .rules(List.of(
                PermissionRule.of("*", PermissionBehavior.DENY, RuleSource.POLICY_SETTINGS)
            ))
            .build();

        assertEquals(PermissionDecision.DENY, engine.evaluate("Bash", createInput("command", "ls"), ctx));
        assertEquals(PermissionDecision.DENY, engine.evaluate("FileWrite", createInput("path", "/tmp"), ctx));
    }

    @Test
    void patternMatchingOnInput() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .rules(List.of(
                PermissionRule.withPattern("Bash", PermissionBehavior.ALLOW, RuleSource.USER_SETTINGS, "git *")
            ))
            .build();

        // "git status" matches "git *"
        assertEquals(PermissionDecision.ALLOW,
            engine.evaluate("Bash", createInput("command", "git status"), ctx));

        // "rm -rf /" does not match "git *", falls through to DEFAULT → ASK
        assertEquals(PermissionDecision.ASK,
            engine.evaluate("Bash", createInput("command", "rm -rf /"), ctx));
    }

    @Test
    void autoModeReturnsAllow() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.AUTO)
            .build();

        assertEquals(PermissionDecision.ALLOW, engine.evaluate("Bash", createInput("command", "ls"), ctx));
    }

    @Test
    void dontAskModeReturnsAllow() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DONT_ASK)
            .build();

        assertEquals(PermissionDecision.ALLOW, engine.evaluate("FileWrite", createInput("path", "/tmp"), ctx));
    }

    @Test
    void toolPermissionContextImmutability() {
        ToolPermissionContext original = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .rules(List.of(PermissionRule.of("Bash", PermissionBehavior.ALLOW, RuleSource.USER_SETTINGS)))
            .build();

        ToolPermissionContext updated = original.setMode(PermissionMode.PLAN);

        assertEquals(PermissionMode.DEFAULT, original.mode());
        assertEquals(PermissionMode.PLAN, updated.mode());
        assertEquals(original.rules(), updated.rules());
    }

    @Test
    void addAndRemoveRules() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .build();

        PermissionRule rule1 = PermissionRule.of("Bash", PermissionBehavior.ALLOW, RuleSource.USER_SETTINGS);
        PermissionRule rule2 = PermissionRule.of("FileWrite", PermissionBehavior.DENY, RuleSource.POLICY_SETTINGS);

        ToolPermissionContext withRules = ctx.addRules(List.of(rule1, rule2));
        assertEquals(2, withRules.rules().size());

        ToolPermissionContext afterRemove = withRules.removeRules(r -> r.toolName().equals("Bash"));
        assertEquals(1, afterRemove.rules().size());
        assertEquals("FileWrite", afterRemove.rules().get(0).toolName());
    }

    @Test
    void addAndRemoveDirectories() {
        ToolPermissionContext ctx = ToolPermissionContext.of(Path.of("."));

        ToolPermissionContext withDirs = ctx.addDirectories(List.of(Path.of("/tmp"), Path.of("/var")));
        assertEquals(2, withDirs.additionalDirs().size());

        ToolPermissionContext afterRemove = withDirs.removeDirectories(List.of(Path.of("/tmp")));
        assertEquals(1, afterRemove.additionalDirs().size());
        assertEquals(Path.of("/var"), afterRemove.additionalDirs().get(0));
    }

    @Test
    void builderPattern() {
        ToolPermissionContext ctx = ToolPermissionContext.builder()
            .workingDirectory(Path.of("/home/user"))
            .mode(PermissionMode.PLAN)
            .addRule(PermissionRule.of("Bash", PermissionBehavior.ALLOW, RuleSource.CLI_ARG))
            .addDir(Path.of("/tmp"))
            .build();

        assertEquals(Path.of("/home/user"), ctx.workingDirectory());
        assertEquals(PermissionMode.PLAN, ctx.mode());
        assertEquals(1, ctx.rules().size());
        assertEquals(1, ctx.additionalDirs().size());
    }

    @Test
    void toBuilderPreservesValues() {
        ToolPermissionContext original = ToolPermissionContext.builder()
            .workingDirectory(Path.of("/home"))
            .mode(PermissionMode.AUTO)
            .addRule(PermissionRule.of("Bash", PermissionBehavior.DENY, RuleSource.SESSION))
            .addDir(Path.of("/var"))
            .build();

        ToolPermissionContext copy = original.toBuilder().build();

        assertEquals(original, copy);
    }

    private static JsonNode createInput(String key, String value) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put(key, value);
        return node;
    }
}
