package com.claudecode.services.tasks;

import com.claudecode.core.engine.AbortController;
import com.claudecode.core.engine.ToolExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskSystemTest {

    // --- TaskIdGenerator ---

    @Test
    void generatedIdStartsWithTypePrefix() {
        String id = TaskIdGenerator.generate(TaskType.LOCAL_BASH);
        assertTrue(id.startsWith("b"));
        assertEquals(9, id.length()); // 1 prefix + 8 random
    }

    @Test
    void generatedIdsAreUnique() {
        String id1 = TaskIdGenerator.generate(TaskType.LOCAL_AGENT);
        String id2 = TaskIdGenerator.generate(TaskType.LOCAL_AGENT);
        assertNotEquals(id1, id2);
    }

    @Test
    void extractTypeFromId() {
        assertEquals(TaskType.LOCAL_BASH, TaskIdGenerator.extractType("b12345678"));
        assertEquals(TaskType.LOCAL_AGENT, TaskIdGenerator.extractType("a12345678"));
        assertEquals(TaskType.IN_PROCESS_TEAMMATE, TaskIdGenerator.extractType("t12345678"));
    }

    @Test
    void extractTypeFromInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> TaskIdGenerator.extractType("z12345678"));
        assertThrows(IllegalArgumentException.class, () -> TaskIdGenerator.extractType(null));
    }

    // --- TaskStateMachine ---

    @Test
    void validTransitions() {
        assertTrue(TaskStateMachine.isValidTransition(TaskStatus.PENDING, TaskStatus.RUNNING));
        assertTrue(TaskStateMachine.isValidTransition(TaskStatus.PENDING, TaskStatus.KILLED));
        assertTrue(TaskStateMachine.isValidTransition(TaskStatus.RUNNING, TaskStatus.COMPLETED));
        assertTrue(TaskStateMachine.isValidTransition(TaskStatus.RUNNING, TaskStatus.FAILED));
        assertTrue(TaskStateMachine.isValidTransition(TaskStatus.RUNNING, TaskStatus.KILLED));
    }

    @Test
    void invalidTransitions() {
        assertFalse(TaskStateMachine.isValidTransition(TaskStatus.COMPLETED, TaskStatus.RUNNING));
        assertFalse(TaskStateMachine.isValidTransition(TaskStatus.FAILED, TaskStatus.RUNNING));
        assertFalse(TaskStateMachine.isValidTransition(TaskStatus.KILLED, TaskStatus.RUNNING));
        assertFalse(TaskStateMachine.isValidTransition(TaskStatus.PENDING, TaskStatus.COMPLETED));
    }

    @Test
    void validateTransitionThrowsOnInvalid() {
        assertThrows(IllegalStateException.class,
            () -> TaskStateMachine.validateTransition(TaskStatus.COMPLETED, TaskStatus.RUNNING));
    }

    // --- TaskState ---

    @Test
    void taskStateCreation() {
        TaskState task = TaskState.create(TaskType.LOCAL_BASH, "Run tests");
        assertEquals(TaskStatus.PENDING, task.status());
        assertEquals(TaskType.LOCAL_BASH, task.type());
        assertEquals("Run tests", task.description());
        assertTrue(task.id().startsWith("b"));
    }

    @Test
    void taskStateTransition() {
        TaskState task = TaskState.create(TaskType.LOCAL_BASH, "Run tests");
        TaskState running = task.withStatus(TaskStatus.RUNNING);
        assertEquals(TaskStatus.RUNNING, running.status());

        TaskState completed = running.withStatus(TaskStatus.COMPLETED);
        assertEquals(TaskStatus.COMPLETED, completed.status());
        assertTrue(completed.endTime().isPresent());
    }

    @Test
    void taskStateInvalidTransitionThrows() {
        TaskState task = TaskState.create(TaskType.LOCAL_BASH, "Run tests");
        assertThrows(IllegalStateException.class, () -> task.withStatus(TaskStatus.COMPLETED));
    }

    // --- TaskStore ---

    @Test
    void taskStoreCrud() {
        TaskStore store = new TaskStore();
        TaskState task = store.create(TaskType.LOCAL_BASH, "Build project");
        assertEquals(1, store.size());

        assertTrue(store.get(task.id()).isPresent());
        assertEquals(1, store.list().size());

        store.updateStatus(task.id(), TaskStatus.RUNNING);
        assertEquals(TaskStatus.RUNNING, store.get(task.id()).get().status());
    }

    @Test
    void taskStoreCompletionNotification() {
        TaskStore store = new TaskStore();
        List<TaskState> completed = new ArrayList<>();
        store.onCompletion(completed::add);

        TaskState task = store.create(TaskType.LOCAL_BASH, "Build");
        store.updateStatus(task.id(), TaskStatus.RUNNING);
        assertTrue(completed.isEmpty());

        store.updateStatus(task.id(), TaskStatus.COMPLETED);
        assertEquals(1, completed.size());
        assertEquals(TaskStatus.COMPLETED, completed.get(0).status());
    }

    @Test
    void taskStoreListByStatus() {
        TaskStore store = new TaskStore();
        store.create(TaskType.LOCAL_BASH, "Task 1");
        TaskState t2 = store.create(TaskType.LOCAL_AGENT, "Task 2");
        store.updateStatus(t2.id(), TaskStatus.RUNNING);

        assertEquals(1, store.listByStatus(TaskStatus.PENDING).size());
        assertEquals(1, store.listByStatus(TaskStatus.RUNNING).size());
    }

    // --- Task CRUD Tools ---

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolExecutionContext testContext() {
        return ToolExecutionContext.of(new AbortController(), "test-session");
    }

    @Test
    void taskCreateToolCreatesTask() {
        TaskStore store = new TaskStore();
        var tool = new TaskCreateTool(store);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("type", "LOCAL_BASH");
        input.put("description", "Run tests");

        String result = tool.call(input, testContext());
        assertTrue(result.startsWith("Created task"));
        assertEquals(1, store.size());
    }

    @Test
    void taskListToolListsTasks() {
        TaskStore store = new TaskStore();
        store.create(TaskType.LOCAL_BASH, "Task 1");
        var tool = new TaskListTool(store);

        String result = tool.call(MAPPER.createObjectNode(), testContext());
        assertTrue(result.contains("Task 1"));
    }

    @Test
    void taskGetToolGetsTask() {
        TaskStore store = new TaskStore();
        TaskState task = store.create(TaskType.LOCAL_BASH, "Task 1");
        var tool = new TaskGetTool(store);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("task_id", task.id());

        String result = tool.call(input, testContext());
        assertTrue(result.contains(task.id()));
    }

    @Test
    void taskStopToolKillsTask() {
        TaskStore store = new TaskStore();
        TaskState task = store.create(TaskType.LOCAL_BASH, "Task 1");
        store.updateStatus(task.id(), TaskStatus.RUNNING);
        var tool = new TaskStopTool(store);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("task_id", task.id());

        String result = tool.call(input, testContext());
        assertTrue(result.contains("stopped"));
        assertEquals(TaskStatus.KILLED, store.get(task.id()).get().status());
    }

    @Test
    void taskUpdateToolUpdatesDescription() {
        TaskStore store = new TaskStore();
        TaskState task = store.create(TaskType.LOCAL_BASH, "Old desc");
        var tool = new TaskUpdateTool(store);

        ObjectNode input = MAPPER.createObjectNode();
        input.put("task_id", task.id());
        input.put("description", "New desc");

        String result = tool.call(input, testContext());
        assertTrue(result.contains("New desc"));
    }

    // --- LocalAgentTask ---

    @Test
    void localAgentTaskProgress() {
        TaskState task = TaskState.create(TaskType.LOCAL_AGENT, "Agent task");
        var agent = new LocalAgentTask(task);
        assertEquals(0.0, agent.getProgress());

        agent.updateProgress(0.5, "processing");
        assertEquals(0.5, agent.getProgress());
        assertEquals("processing", agent.getCurrentStep());

        agent.complete("done");
        assertEquals(1.0, agent.getProgress());
    }

    // --- InProcessTeammateTask ---

    @Test
    void inProcessTeammateStartStop() {
        TaskState task = TaskState.create(TaskType.IN_PROCESS_TEAMMATE, "Teammate");
        var teammate = new InProcessTeammateTask(task);
        assertFalse(teammate.isActive());

        teammate.start();
        assertTrue(teammate.isActive());

        teammate.stop();
        assertFalse(teammate.isActive());
    }
}
