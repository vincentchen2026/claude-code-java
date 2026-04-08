package com.claudecode.services.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sub-agent progress tracking stub.
 * Tracks the progress of a locally running agent task.
 */
public class LocalAgentTask {

    private static final Logger log = LoggerFactory.getLogger(LocalAgentTask.class);

    private final TaskState taskState;
    private volatile double progress;
    private volatile String currentStep;

    public LocalAgentTask(TaskState taskState) {
        this.taskState = taskState;
        this.progress = 0.0;
        this.currentStep = "initializing";
    }

    public String getTaskId() {
        return taskState.id();
    }

    public double getProgress() {
        return progress;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    /**
     * Updates the progress of the agent task.
     */
    public void updateProgress(double progress, String step) {
        if (progress < 0 || progress > 1.0) {
            throw new IllegalArgumentException("Progress must be between 0 and 1.0");
        }
        this.progress = progress;
        this.currentStep = step;
        log.debug("Agent task {} progress: {:.0%} - {}", taskState.id(), progress, step);
    }

    /**
     * Marks the agent task as complete.
     */
    public void complete(String result) {
        this.progress = 1.0;
        this.currentStep = "completed";
        log.info("Agent task {} completed: {}", taskState.id(), result);
    }
}
