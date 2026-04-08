package com.claudecode.services.tasks;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CP-6: Task lifecycle correctness property-based tests.
 * Validates: Requirements 29.2
 */
class TaskLifecycleProperties {

    @Provide
    Arbitrary<TaskType> taskTypes() {
        return Arbitraries.of(TaskType.values());
    }

    @Provide
    Arbitrary<String> descriptions() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
    }

    /**
     * Property: Generated task IDs always start with the correct type prefix.
     */
    @Property(tries = 100)
    void generatedIdAlwaysHasCorrectPrefix(@ForAll("taskTypes") TaskType type) {
        String id = TaskIdGenerator.generate(type);
        assertTrue(id.startsWith(type.prefix()),
            "ID " + id + " should start with prefix " + type.prefix());
    }

    /**
     * Property: Generated task IDs are always the expected length (1 prefix + 8 random).
     */
    @Property(tries = 100)
    void generatedIdAlwaysHasCorrectLength(@ForAll("taskTypes") TaskType type) {
        String id = TaskIdGenerator.generate(type);
        assertEquals(9, id.length());
    }

    /**
     * Property: Task type can always be extracted from a generated ID.
     */
    @Property(tries = 100)
    void typeCanBeExtractedFromGeneratedId(@ForAll("taskTypes") TaskType type) {
        String id = TaskIdGenerator.generate(type);
        assertEquals(type, TaskIdGenerator.extractType(id));
    }

    /**
     * Property: New tasks always start in PENDING status.
     */
    @Property(tries = 50)
    void newTasksAlwaysStartPending(
            @ForAll("taskTypes") TaskType type,
            @ForAll("descriptions") String desc) {
        TaskState task = TaskState.create(type, desc);
        assertEquals(TaskStatus.PENDING, task.status());
        assertTrue(task.endTime().isEmpty());
    }

    /**
     * Property: Terminal states have no valid outgoing transitions.
     */
    @Property(tries = 50)
    void terminalStatesHaveNoOutgoingTransitions(
            @ForAll("taskTypes") TaskType type) {
        for (TaskStatus terminal : new TaskStatus[]{TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.KILLED}) {
            for (TaskStatus target : TaskStatus.values()) {
                assertFalse(TaskStateMachine.isValidTransition(terminal, target),
                    terminal + " should not transition to " + target);
            }
        }
    }

    /**
     * Property: Completing a task always sets an end time.
     */
    @Property(tries = 50)
    void completingTaskSetsEndTime(
            @ForAll("taskTypes") TaskType type,
            @ForAll("descriptions") String desc) {
        TaskState task = TaskState.create(type, desc);
        TaskState running = task.withStatus(TaskStatus.RUNNING);
        TaskState completed = running.withStatus(TaskStatus.COMPLETED);
        assertTrue(completed.endTime().isPresent());
    }

    /**
     * Property: Task IDs within a store are always unique.
     */
    @Property(tries = 20)
    void taskStoreIdsAreUnique(@ForAll("taskTypes") TaskType type) {
        TaskStore store = new TaskStore();
        TaskState t1 = store.create(type, "task 1");
        TaskState t2 = store.create(type, "task 2");
        assertNotEquals(t1.id(), t2.id());
    }
}
