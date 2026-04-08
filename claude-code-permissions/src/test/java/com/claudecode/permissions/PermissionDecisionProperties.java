package com.claudecode.permissions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CP-3 Permission decision consistency property-based tests.
 * <p>
 * Validates: Requirements CP-3 (权限决策一致性)
 * <p>
 * Properties verified:
 * - Same rules + same input → same decision (determinism)
 * - Deny rules always take precedence over allow rules
 * - BYPASS_PERMISSIONS mode always returns ALLOW
 * - Rule CRUD operations preserve other rules
 * - Mode transitions are valid
 */
class PermissionDecisionProperties {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final PermissionEngine engine = new PermissionEngine();

    // ========== Property: Determinism — same rules + same input → same decision ==========

    /**
     * CP-3: Given the same rules and input, the engine always returns the same decision.
     * <p>
     * Validates: Requirements CP-3
     */
    @Property(tries = 200)
    void sameRulesAndInputProduceSameDecision(
        @ForAll("toolNames") String toolName,
        @ForAll("toolInputs") JsonNode input,
        @ForAll("permissionContexts") ToolPermissionContext context
    ) {
        PermissionDecision first = engine.evaluate(toolName, input, context);
        PermissionDecision second = engine.evaluate(toolName, input, context);

        assertEquals(first, second,
            "Same rules + same input must always produce the same decision");
    }

    // ========== Property: Deny rules take precedence over allow rules ==========

    /**
     * CP-3: When both deny and allow rules exist for the same tool, deny wins.
     * <p>
     * Validates: Requirements CP-3
     */
    @Property(tries = 200)
    void denyRulesTakePrecedenceOverAllowRules(
        @ForAll("toolNames") String toolName,
        @ForAll("toolInputs") JsonNode input,
        @ForAll("ruleSources") RuleSource denySource,
        @ForAll("ruleSources") RuleSource allowSource
    ) {
        List<PermissionRule> rules = List.of(
            PermissionRule.of(toolName, PermissionBehavior.DENY, denySource),
            PermissionRule.of(toolName, PermissionBehavior.ALLOW, allowSource)
        );

        ToolPermissionContext context = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .rules(rules)
            .build();

        PermissionDecision decision = engine.evaluate(toolName, input, context);

        assertEquals(PermissionDecision.DENY, decision,
            "Deny rules must always take precedence over allow rules");
    }

    // ========== Property: BYPASS_PERMISSIONS mode always returns ALLOW ==========

    /**
     * CP-3: In BYPASS_PERMISSIONS mode with no deny rules, the engine always returns ALLOW.
     * <p>
     * Validates: Requirements CP-3
     */
    @Property(tries = 200)
    void bypassPermissionsModeAlwaysAllows(
        @ForAll("toolNames") String toolName,
        @ForAll("toolInputs") JsonNode input,
        @ForAll("allowOnlyRules") List<PermissionRule> allowRules
    ) {
        ToolPermissionContext context = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.BYPASS_PERMISSIONS)
            .rules(allowRules)
            .build();

        PermissionDecision decision = engine.evaluate(toolName, input, context);

        assertEquals(PermissionDecision.ALLOW, decision,
            "BYPASS_PERMISSIONS mode must always return ALLOW when no deny rules exist");
    }

    // ========== Property: Rule CRUD operations preserve other rules ==========

    /**
     * CP-3: Adding rules preserves existing rules.
     * <p>
     * Validates: Requirements CP-3
     */
    @Property(tries = 200)
    void addRulesPreservesExistingRules(
        @ForAll("permissionRuleLists") List<PermissionRule> existingRules,
        @ForAll("permissionRuleLists") List<PermissionRule> newRules
    ) {
        ToolPermissionContext original = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .rules(existingRules)
            .build();

        ToolPermissionContext updated = original.addRules(newRules);

        // All existing rules should still be present
        for (PermissionRule existing : existingRules) {
            assertTrue(updated.rules().contains(existing),
                "Existing rule must be preserved after addRules: " + existing);
        }

        // All new rules should be present
        for (PermissionRule added : newRules) {
            assertTrue(updated.rules().contains(added),
                "New rule must be present after addRules: " + added);
        }

        // Total size should be sum
        assertEquals(existingRules.size() + newRules.size(), updated.rules().size(),
            "Total rules count must be sum of existing + new");
    }

    /**
     * CP-3: Removing rules does not affect non-matching rules.
     * <p>
     * Validates: Requirements CP-3
     */
    @Property(tries = 200)
    void removeRulesPreservesNonMatchingRules(
        @ForAll("permissionRuleLists") List<PermissionRule> rules,
        @ForAll("ruleSources") RuleSource sourceToRemove
    ) {
        ToolPermissionContext original = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .rules(rules)
            .build();

        ToolPermissionContext updated = original.removeRules(r -> r.source() == sourceToRemove);

        // Non-matching rules should be preserved
        List<PermissionRule> expectedRemaining = rules.stream()
            .filter(r -> r.source() != sourceToRemove)
            .toList();

        assertEquals(expectedRemaining, updated.rules(),
            "Non-matching rules must be preserved after removeRules");
    }

    /**
     * CP-3: replaceRules completely replaces all rules.
     * <p>
     * Validates: Requirements CP-3
     */
    @Property(tries = 200)
    void replaceRulesReplacesAllRules(
        @ForAll("permissionRuleLists") List<PermissionRule> originalRules,
        @ForAll("permissionRuleLists") List<PermissionRule> replacementRules
    ) {
        ToolPermissionContext original = ToolPermissionContext.builder()
            .workingDirectory(Path.of("."))
            .mode(PermissionMode.DEFAULT)
            .rules(originalRules)
            .build();

        ToolPermissionContext updated = original.replaceRules(replacementRules);

        assertEquals(replacementRules, updated.rules(),
            "replaceRules must completely replace all rules");
    }

    // ========== Property: Mode transitions preserve rules and directories ==========

    /**
     * CP-3: Changing mode preserves all rules and directories.
     * <p>
     * Validates: Requirements CP-3
     */
    @Property(tries = 200)
    void modeTransitionsPreserveRulesAndDirs(
        @ForAll("permissionContexts") ToolPermissionContext original,
        @ForAll("permissionModes") PermissionMode newMode
    ) {
        ToolPermissionContext updated = original.setMode(newMode);

        assertEquals(newMode, updated.mode(),
            "Mode must be updated");
        assertEquals(original.rules(), updated.rules(),
            "Rules must be preserved after mode change");
        assertEquals(original.additionalDirs(), updated.additionalDirs(),
            "Additional directories must be preserved after mode change");
        assertEquals(original.workingDirectory(), updated.workingDirectory(),
            "Working directory must be preserved after mode change");
    }

    // ========== Property: Directory CRUD preserves rules and mode ==========

    /**
     * CP-3: Adding directories preserves rules and mode.
     * <p>
     * Validates: Requirements CP-3
     */
    @Property(tries = 100)
    void addDirectoriesPreservesRulesAndMode(
        @ForAll("permissionContexts") ToolPermissionContext original,
        @ForAll("pathLists") List<Path> newDirs
    ) {
        ToolPermissionContext updated = original.addDirectories(newDirs);

        assertEquals(original.mode(), updated.mode(),
            "Mode must be preserved after adding directories");
        assertEquals(original.rules(), updated.rules(),
            "Rules must be preserved after adding directories");

        // All original dirs should still be present
        for (Path dir : original.additionalDirs()) {
            assertTrue(updated.additionalDirs().contains(dir),
                "Existing directory must be preserved: " + dir);
        }
    }

    // ========== Property: Immutability — original context is never modified ==========

    /**
     * CP-3: All mutation operations return new instances; original is unchanged.
     * <p>
     * Validates: Requirements CP-3
     */
    @Property(tries = 100)
    void mutationOperationsAreImmutable(
        @ForAll("permissionContexts") ToolPermissionContext original,
        @ForAll("permissionModes") PermissionMode newMode,
        @ForAll("permissionRuleLists") List<PermissionRule> newRules
    ) {
        // Capture original state
        PermissionMode origMode = original.mode();
        List<PermissionRule> origRules = List.copyOf(original.rules());
        List<Path> origDirs = List.copyOf(original.additionalDirs());

        // Perform mutations
        original.setMode(newMode);
        original.addRules(newRules);
        original.replaceRules(newRules);
        original.removeRules(r -> true);
        original.addDirectories(List.of(Path.of("/tmp/new")));
        original.removeDirectories(List.of(Path.of("/tmp/new")));

        // Original must be unchanged
        assertEquals(origMode, original.mode(), "Original mode must not change");
        assertEquals(origRules, original.rules(), "Original rules must not change");
        assertEquals(origDirs, original.additionalDirs(), "Original dirs must not change");
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> toolNames() {
        return Arbitraries.of("Bash", "FileWrite", "FileEdit", "FileRead", "GrepTool", "GlobTool", "NotebookEdit");
    }

    @Provide
    Arbitrary<JsonNode> toolInputs() {
        return Arbitraries.of(
            createInput("command", "ls -la"),
            createInput("command", "git status"),
            createInput("command", "rm -rf /"),
            createInput("file_path", "/tmp/test.txt"),
            createInput("path", "/home/user/file.java"),
            MAPPER.createObjectNode()
        );
    }

    @Provide
    Arbitrary<PermissionMode> permissionModes() {
        return Arbitraries.of(PermissionMode.values());
    }

    @Provide
    Arbitrary<RuleSource> ruleSources() {
        return Arbitraries.of(RuleSource.values());
    }

    @Provide
    Arbitrary<PermissionRule> permissionRules() {
        return Combinators.combine(
            toolNames(),
            Arbitraries.of(PermissionBehavior.ALLOW, PermissionBehavior.DENY, PermissionBehavior.ASK),
            ruleSources()
        ).as((name, behavior, source) -> PermissionRule.of(name, behavior, source));
    }

    @Provide
    Arbitrary<List<PermissionRule>> permissionRuleLists() {
        return permissionRules().list().ofMinSize(0).ofMaxSize(5);
    }

    @Provide
    Arbitrary<List<PermissionRule>> allowOnlyRules() {
        return Combinators.combine(
            toolNames(),
            ruleSources()
        ).as((name, source) -> PermissionRule.of(name, PermissionBehavior.ALLOW, source))
            .list().ofMinSize(0).ofMaxSize(3);
    }

    @Provide
    Arbitrary<List<Path>> pathLists() {
        return Arbitraries.of(
            Path.of("/tmp"), Path.of("/home"), Path.of("/var/log")
        ).list().ofMinSize(0).ofMaxSize(3);
    }

    @Provide
    Arbitrary<ToolPermissionContext> permissionContexts() {
        return Combinators.combine(
            permissionModes(),
            permissionRuleLists(),
            pathLists()
        ).as((mode, rules, dirs) ->
            ToolPermissionContext.builder()
                .workingDirectory(Path.of("."))
                .mode(mode)
                .rules(rules)
                .additionalDirs(dirs)
                .build()
        );
    }

    // ========== Helpers ==========

    private static JsonNode createInput(String key, String value) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put(key, value);
        return node;
    }
}
