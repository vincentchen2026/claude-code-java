package com.claudecode.services.coordinator;

/**
 * Interface for worker agent execution and lifecycle management.
 */
public interface WorkerAgent {

    /**
     * Executes the worker with the given configuration.
     */
    WorkerResult execute(WorkerConfig config);

    /**
     * Cancels the worker if it is still running.
     */
    void cancel();

    /**
     * Returns the current state of the worker.
     */
    WorkerState getState();
}
