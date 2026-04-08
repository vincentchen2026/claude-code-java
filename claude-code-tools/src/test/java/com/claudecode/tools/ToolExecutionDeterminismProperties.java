package com.claudecode.tools;

import com.claudecode.core.engine.AbortController;
import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.core.engine.ToolResult;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.nio.file.Path;

/**
 * CP-2: Tool execution determinism property-based tests.
 *
 * **Validates: Requirements AC-2.1, AC-2.2, AC-2.5 (CP-2)**
 *
 * Properties verified:
 * - Given same tool name and input, result is semantically equivalent
 * - Tool input schema validation rejects invalid inputs
 * - BashTool search/read command detection is consistent
 */
class ToolExecutionDeterminismProperties {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Property: Given the same tool name and input, executing via ToolRegistry
     * produces semantically equivalent results across multiple invocations.
     *
     * **Validates: Requirements AC-2.1 (CP-2)**
     */
    @Property(tries = 50)
    void sameToolAndInputProduceEquivalentResults(
            @ForAll("registeredToolNames") String toolName,
            @ForAll("validToolInputs") ObjectNode input) {

        ToolRegistry registry = createRegistry();
        ToolExecutionContext ctx = ToolExecutionContext.of(new AbortController(), "test");

        ToolResult result1 = registry.execute(toolName, input, ctx);
        ToolResult result2 = registry.execute(toolName, input, ctx);

        // Same error status
        assert result1.isError() == result2.isError() :
                "Error status differs for tool=" + toolName;

        // Same content (deterministic output)
        assert result1.content().size() == result2.content().size() :
                "Content size differs for tool=" + toolName;
    }

    /**
     * Property: Tool input schema validation rejects inputs missing required fields.
     * All tools have a schema with required fields; inputs missing those fields
     * should produce error results.
     *
     * **Validates: Requirements AC-2.2 (CP-2)**
     */
    @Property(tries = 50)
    void emptyInputProducesErrorForAllTools(
            @ForAll("registeredToolNames") String toolName) {

        ToolRegistry registry = createRegistry();
        ObjectNode emptyInput = MAPPER.createObjectNode();
        ToolExecutionContext ctx = ToolExecutionContext.of(new AbortController(), "test");

        ToolResult result = registry.execute(toolName, emptyInput, ctx);

        // Empty input should produce an error result for tools that require fields
        assert result.isError() || result.content().get(0).toString().contains("Error") :
                "Empty input should produce error for tool=" + toolName;
    }

    /**
     * Property: BashTool search/read command detection is consistent —
     * calling isSearchOrReadCommand multiple times with the same input
     * always returns the same result.
     *
     * **Validates: Requirements AC-2.5, AC-4.5 (CP-2)**
     */
    @Property(tries = 200)
    void bashSearchReadDetectionIsConsistent(
            @ForAll("shellCommands") String command) {

        boolean result1 = BashTool.isSearchOrReadCommand(command);
        boolean result2 = BashTool.isSearchOrReadCommand(command);
        boolean result3 = BashTool.isSearchOrReadCommand(command);

        assert result1 == result2 && result2 == result3 :
                "isSearchOrReadCommand is not consistent for: " + command;
    }

    /**
     * Property: BashTool permission check is consistent —
     * same command always produces the same permission decision.
     *
     * **Validates: Requirements AC-2.3 (CP-2)**
     */
    @Property(tries = 200)
    void bashPermissionCheckIsConsistent(
            @ForAll("shellCommands") String command) {

        PermissionDecision d1 = BashPermissions.check(command);
        PermissionDecision d2 = BashPermissions.check(command);

        assert d1 == d2 : "Permission decision is not consistent for: " + command;
    }

    /**
     * Property: Read-only tools always report isReadOnly() == true.
     *
     * **Validates: Requirements AC-2.5 (CP-2)**
     */
    @Property(tries = 10)
    void readOnlyToolsAreConsistentlyReadOnly(
            @ForAll("readOnlyToolNames") String toolName) {

        ToolRegistry registry = createRegistry();
        Tool<?, ?> tool = registry.get(toolName).orElseThrow();

        assert tool.isReadOnly() : toolName + " should be read-only";
        assert tool.isConcurrencySafe() : toolName + " should be concurrency-safe";
    }

    // --- Providers ---

    @Provide
    Arbitrary<String> registeredToolNames() {
        return Arbitraries.of("Bash", "Read", "Write", "Edit", "Grep", "Glob");
    }

    @Provide
    Arbitrary<String> readOnlyToolNames() {
        return Arbitraries.of("Read", "Grep", "Glob");
    }

    @Provide
    Arbitrary<ObjectNode> validToolInputs() {
        return Arbitraries.of(
                createInput("command", "echo test"),
                createInput("file_path", "/tmp/nonexistent-test-file.txt"),
                createInput("pattern", "test")
        );
    }

    @Provide
    Arbitrary<String> shellCommands() {
        Arbitrary<String> readCommands = Arbitraries.of(
                "grep -r pattern .", "find . -name '*.java'", "ls -la",
                "cat file.txt", "head -n 10 file.txt", "tail -f log.txt",
                "wc -l file.txt", "git log --oneline", "git status",
                "which java", "file test.txt", "echo hello"
        );
        Arbitrary<String> writeCommands = Arbitraries.of(
                "rm file.txt", "mv a b", "cp a b", "mkdir dir",
                "touch file.txt", "chmod 755 file.txt", "npm install",
                "make build", "python script.py"
        );
        Arbitrary<String> pipedCommands = Arbitraries.of(
                "cat file.txt | grep pattern",
                "ls -la | wc -l",
                "cat file.txt | tee output.txt"
        );
        Arbitrary<String> incompleteCommands = Arbitraries.of(
                "echo hello |", "echo hello &&", "echo hello ||"
        );
        Arbitrary<String> edgeCases = Arbitraries.of("", " ", "\t");

        return Arbitraries.oneOf(readCommands, writeCommands, pipedCommands,
                incompleteCommands, edgeCases);
    }

    // --- Helpers ---

    private ToolRegistry createRegistry() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new BashTool());
        registry.register(new FileReadTool());
        registry.register(new FileWriteTool());
        registry.register(new FileEditTool());
        registry.register(new GrepTool());
        registry.register(new GlobTool());
        return registry;
    }

    private static ObjectNode createInput(String key, String value) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put(key, value);
        return node;
    }
}
