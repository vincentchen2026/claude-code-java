package com.claudecode.services.coordinator;

import com.claudecode.core.engine.StreamingClient;
import com.claudecode.core.engine.ToolExecutor;
import com.claudecode.core.engine.ToolResult;
import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.TextBlock;
import com.claudecode.core.message.Usage;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CoordinatorServiceTest {

    @TempDir
    Path tempDir;

    private CoordinatorService service;
    private ScratchpadManager scratchpad;
    private MockStreamingClient mockClient;
    private ToolExecutor mockToolExecutor;

    @BeforeEach
    void setUp() {
        scratchpad = new ScratchpadManager(tempDir.resolve("scratchpad"));
        mockClient = new MockStreamingClient();
        mockToolExecutor = new MockToolExecutor();
        service = new CoordinatorService(scratchpad, mockClient, mockToolExecutor);
    }

    // --- CoordinatorService prompt generation ---

    @Test
    void buildCoordinatorPromptContainsRoleSection() {
        var ctx = new CoordinatorContext(
            List.of("Bash", "FileRead"), List.of(), tempDir, 4, Map.of()
        );
        String prompt = service.buildCoordinatorPrompt(ctx);
        assertTrue(prompt.contains("coordinator managing multiple worker agents"));
        assertTrue(prompt.contains("## Your Role"));
        assertTrue(prompt.contains("## Worker Management"));
    }

    @Test
    void buildCoordinatorPromptIncludesTools() {
        var ctx = new CoordinatorContext(
            List.of("Bash", "FileRead", "FileWrite"), List.of(), null, 4, Map.of()
        );
        String prompt = service.buildCoordinatorPrompt(ctx);
        assertTrue(prompt.contains("- Bash"));
        assertTrue(prompt.contains("- FileRead"));
        assertTrue(prompt.contains("- FileWrite"));
    }

    @Test
    void buildCoordinatorPromptIncludesMcpServers() {
        var ctx = new CoordinatorContext(
            List.of(), List.of("github-mcp", "slack-mcp"), null, 4, Map.of()
        );
        String prompt = service.buildCoordinatorPrompt(ctx);
        assertTrue(prompt.contains("## MCP Servers"));
        assertTrue(prompt.contains("- github-mcp"));
        assertTrue(prompt.contains("- slack-mcp"));
    }

    @Test
    void buildCoordinatorPromptOmitsMcpSectionWhenEmpty() {
        var ctx = new CoordinatorContext(List.of(), List.of(), null, 4, Map.of());
        String prompt = service.buildCoordinatorPrompt(ctx);
        assertFalse(prompt.contains("## MCP Servers"));
    }

    @Test
    void buildCoordinatorPromptIncludesScratchpadDir() {
        var ctx = new CoordinatorContext(
            List.of(), List.of(), tempDir.resolve("scratch"), 4, Map.of()
        );
        String prompt = service.buildCoordinatorPrompt(ctx);
        assertTrue(prompt.contains("## Scratchpad"));
        assertTrue(prompt.contains("scratch"));
    }

    @Test
    void buildCoordinatorPromptShowsMaxWorkers() {
        var ctx = new CoordinatorContext(List.of(), List.of(), null, 8, Map.of());
        String prompt = service.buildCoordinatorPrompt(ctx);
        assertTrue(prompt.contains("## Max Workers: 8"));
    }

    // --- Worker execution ---

    @Test
    void executeWorkerReturnsCompletedResult() {
        mockClient.setResponse("Task completed successfully");

        var config = new WorkerConfig(
            "w1", "test task", WorkerConfig.WorkerMode.SIMPLE,
            Optional.empty(), List.of("Bash"), 10, 1.0
        );
        WorkerResult result = service.executeWorker(config);
        assertEquals(WorkerResult.WorkerStatus.COMPLETED, result.status());
        assertEquals("w1", result.workerId());
        assertTrue(result.output().isPresent());
    }

    @Test
    void activeWorkerCountIsZeroAfterExecution() {
        mockClient.setResponse("Done");

        var config = new WorkerConfig(
            "w1", "task", WorkerConfig.WorkerMode.FULL,
            Optional.empty(), List.of(), 10, 1.0
        );
        service.executeWorker(config);
        assertEquals(0, service.activeWorkerCount());
    }

    // --- Worker renew vs regenerate ---

    @Test
    void shouldContinueWorkerWhenHighProgressAndContext() {
        var state = new WorkerState("w1", WorkerState.Status.RUNNING, 5, 50_000, 0.8);
        assertTrue(service.shouldContinueWorker(state,
            CoordinatorService.TaskComplexity.LOW));
    }

    @Test
    void shouldNotContinueWorkerWhenLowProgress() {
        var state = new WorkerState("w1", WorkerState.Status.RUNNING, 1, 50_000, 0.2);
        assertFalse(service.shouldContinueWorker(state,
            CoordinatorService.TaskComplexity.LOW));
    }

    @Test
    void shouldNotContinueWorkerWhenLowContext() {
        var state = new WorkerState("w1", WorkerState.Status.RUNNING, 5, 5_000, 0.9);
        assertFalse(service.shouldContinueWorker(state,
            CoordinatorService.TaskComplexity.HIGH));
    }

    // --- Scratchpad ---

    @Test
    void scratchpadWriteAndRead() {
        scratchpad.writeFile("notes.txt", "hello world");
        assertEquals("hello world", scratchpad.readFile("notes.txt"));
    }

    @Test
    void scratchpadListFiles() {
        scratchpad.writeFile("a.txt", "a");
        scratchpad.writeFile("b.txt", "b");
        List<String> files = scratchpad.listFiles();
        assertEquals(List.of("a.txt", "b.txt"), files);
    }

    @Test
    void scratchpadFileExists() {
        assertFalse(scratchpad.fileExists("missing.txt"));
        scratchpad.writeFile("exists.txt", "data");
        assertTrue(scratchpad.fileExists("exists.txt"));
    }

    @Test
    void scratchpadDeleteFile() {
        scratchpad.writeFile("temp.txt", "data");
        assertTrue(scratchpad.deleteFile("temp.txt"));
        assertFalse(scratchpad.fileExists("temp.txt"));
    }

    @Test
    void scratchpadListFilesEmptyDir() {
        assertEquals(List.of(), scratchpad.listFiles());
    }

    // --- Session mode detection ---

    @Test
    void detectSessionModeCoordinator() {
        assertEquals(CoordinatorService.SessionMode.COORDINATOR,
            CoordinatorService.detectSessionMode(Optional.of("coordinator")));
        assertEquals(CoordinatorService.SessionMode.COORDINATOR,
            CoordinatorService.detectSessionMode(Optional.of("coord")));
    }

    @Test
    void detectSessionModeAgent() {
        assertEquals(CoordinatorService.SessionMode.AGENT,
            CoordinatorService.detectSessionMode(Optional.of("agent")));
    }

    @Test
    void detectSessionModeStandard() {
        assertEquals(CoordinatorService.SessionMode.STANDARD,
            CoordinatorService.detectSessionMode(Optional.empty()));
        assertEquals(CoordinatorService.SessionMode.STANDARD,
            CoordinatorService.detectSessionMode(Optional.of("unknown")));
    }

    @Test
    void isCoordinatorMode() {
        assertTrue(CoordinatorService.isCoordinatorMode(
            CoordinatorService.SessionMode.COORDINATOR));
        assertFalse(CoordinatorService.isCoordinatorMode(
            CoordinatorService.SessionMode.STANDARD));
        assertFalse(CoordinatorService.isCoordinatorMode(
            CoordinatorService.SessionMode.AGENT));
    }

    // --- WorkerAgentImpl ---

    @Test
    void workerAgentLifecycle() {
        mockClient.setResponse("Task completed");

        var agent = new WorkerAgentImpl("w1", mockClient, mockToolExecutor);
        assertEquals(WorkerState.Status.IDLE, agent.getState().status());

        var config = new WorkerConfig(
            "w1", "task", WorkerConfig.WorkerMode.SIMPLE,
            Optional.empty(), List.of(), 10, 1.0
        );
        WorkerResult result = agent.execute(config);
        assertEquals(WorkerResult.WorkerStatus.COMPLETED, result.status());
        assertEquals(WorkerState.Status.COMPLETED, agent.getState().status());
    }

    @Test
    void workerAgentCancel() {
        var agent = new WorkerAgentImpl("w1", mockClient, mockToolExecutor);
        agent.cancel();
        assertEquals(WorkerState.Status.CANCELLED, agent.getState().status());
    }

    // --- Mock implementations ---

    private static class MockStreamingClient implements StreamingClient {
        private String response = "Mock response";

        void setResponse(String response) {
            this.response = response;
        }

        @Override
        public Iterator<StreamingEvent> createStream(StreamRequest request) {
            List<StreamingEvent> events = List.of(
                new StreamingEvent.MessageStartEvent(
                    "msg-1", request.model(), List.of(new TextBlock(response)), Usage.EMPTY),
                new StreamingEvent.ContentBlockStartEvent(0, "text", "cb-1", null),
                new StreamingEvent.ContentBlockDeltaEvent(0, "text_delta", response),
                new StreamingEvent.ContentBlockStopEvent(0),
                new StreamingEvent.MessageDeltaEvent("end_turn", Usage.EMPTY),
                new StreamingEvent.MessageStopEvent()
            );
            return events.iterator();
        }

        @Override
        public String getModel() {
            return "mock-model";
        }
    }

    private static class MockToolExecutor implements ToolExecutor {
        @Override
        public ToolResult execute(String toolName, JsonNode input, com.claudecode.core.engine.ToolExecutionContext context) {
            return ToolResult.success("Tool '" + toolName + "' executed");
        }

        @Override
        public List<StreamingClient.StreamRequest.ToolDef> getToolDefinitions() {
            return List.of(
                new StreamingClient.StreamRequest.ToolDef("Bash", "Execute bash command", null),
                new StreamingClient.StreamRequest.ToolDef("FileRead", "Read file contents", null)
            );
        }
    }
}
