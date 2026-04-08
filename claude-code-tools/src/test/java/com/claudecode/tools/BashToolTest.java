package com.claudecode.tools;

import com.claudecode.core.engine.AbortController;
import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BashToolTest {

    private final BashTool tool = new BashTool();
    private final ObjectMapper mapper = new ObjectMapper();

    private ToolExecutionContext ctx() {
        return ToolExecutionContext.of(new AbortController(), "test-session");
    }

    @Test
    void nameIsBash() {
        assertEquals("Bash", tool.name());
    }

    @Test
    void schemaRequiresCommand() {
        assertTrue(tool.inputSchema().has("required"));
        assertEquals("command", tool.inputSchema().get("required").get(0).asText());
    }

    @Test
    void executeSimpleEcho() {
        ObjectNode input = mapper.createObjectNode();
        input.put("command", "echo hello");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("hello"));
    }

    @Test
    void emptyCommandReturnsError() {
        ObjectNode input = mapper.createObjectNode();
        input.put("command", "");

        String result = tool.call(input, ctx());
        assertTrue(result.contains("Error"));
    }

    @Test
    void permissionDeniesEmptyCommand() {
        ObjectNode input = mapper.createObjectNode();
        input.put("command", "");

        PermissionDecision decision = tool.checkPermissions(input,
                ToolPermissionContext.of(Path.of(".")));
        assertEquals(PermissionDecision.DENY, decision);
    }

    @Test
    void permissionAllowsReadOnlyCommand() {
        ObjectNode input = mapper.createObjectNode();
        input.put("command", "ls -la");

        PermissionDecision decision = tool.checkPermissions(input,
                ToolPermissionContext.of(Path.of(".")));
        assertEquals(PermissionDecision.ALLOW, decision);
    }

    @Test
    void permissionAsksForWriteCommand() {
        ObjectNode input = mapper.createObjectNode();
        input.put("command", "rm -rf /tmp/test");

        PermissionDecision decision = tool.checkPermissions(input,
                ToolPermissionContext.of(Path.of(".")));
        assertEquals(PermissionDecision.ASK, decision);
    }

    @Test
    void permissionDeniesIncompleteCommand() {
        ObjectNode input = mapper.createObjectNode();
        input.put("command", "echo hello |");

        PermissionDecision decision = tool.checkPermissions(input,
                ToolPermissionContext.of(Path.of(".")));
        assertEquals(PermissionDecision.DENY, decision);
    }

    // isSearchOrReadCommand tests
    @Test
    void searchOrReadDetectsGrep() {
        assertTrue(BashTool.isSearchOrReadCommand("grep -r pattern ."));
    }

    @Test
    void searchOrReadDetectsFind() {
        assertTrue(BashTool.isSearchOrReadCommand("find . -name '*.java'"));
    }

    @Test
    void searchOrReadDetectsLs() {
        assertTrue(BashTool.isSearchOrReadCommand("ls -la"));
    }

    @Test
    void searchOrReadDetectsCat() {
        assertTrue(BashTool.isSearchOrReadCommand("cat file.txt"));
    }

    @Test
    void searchOrReadDetectsHead() {
        assertTrue(BashTool.isSearchOrReadCommand("head -n 10 file.txt"));
    }

    @Test
    void searchOrReadDetectsTail() {
        assertTrue(BashTool.isSearchOrReadCommand("tail -f log.txt"));
    }

    @Test
    void searchOrReadDetectsWc() {
        assertTrue(BashTool.isSearchOrReadCommand("wc -l file.txt"));
    }

    @Test
    void searchOrReadDetectsGitLog() {
        assertTrue(BashTool.isSearchOrReadCommand("git log --oneline"));
    }

    @Test
    void searchOrReadDetectsGitStatus() {
        assertTrue(BashTool.isSearchOrReadCommand("git status"));
    }

    @Test
    void searchOrReadRejectsRm() {
        assertFalse(BashTool.isSearchOrReadCommand("rm file.txt"));
    }

    @Test
    void searchOrReadRejectsMv() {
        assertFalse(BashTool.isSearchOrReadCommand("mv a b"));
    }

    @Test
    void searchOrReadRejectsNull() {
        assertFalse(BashTool.isSearchOrReadCommand(null));
    }

    @Test
    void searchOrReadRejectsBlank() {
        assertFalse(BashTool.isSearchOrReadCommand(""));
    }

    @Test
    void searchOrReadDetectsPipedReadCommands() {
        assertTrue(BashTool.isSearchOrReadCommand("cat file.txt | grep pattern"));
    }

    @Test
    void searchOrReadRejectsPipedWriteCommand() {
        assertFalse(BashTool.isSearchOrReadCommand("cat file.txt | tee output.txt"));
    }

    // Incomplete command tests
    @Test
    void incompleteCommandTrailingPipe() {
        assertTrue(BashTool.isIncompleteCommand("echo hello |"));
    }

    @Test
    void incompleteCommandTrailingAnd() {
        assertTrue(BashTool.isIncompleteCommand("echo hello &&"));
    }

    @Test
    void incompleteCommandTrailingOr() {
        assertTrue(BashTool.isIncompleteCommand("echo hello ||"));
    }

    @Test
    void completeCommandIsNotIncomplete() {
        assertFalse(BashTool.isIncompleteCommand("echo hello"));
    }

    @Test
    void nullIsNotIncomplete() {
        assertFalse(BashTool.isIncompleteCommand(null));
    }
}
