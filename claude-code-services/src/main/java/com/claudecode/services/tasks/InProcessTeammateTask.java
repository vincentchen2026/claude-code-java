package com.claudecode.services.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-process teammate task stub.
 * Manages a teammate running in the same JVM process.
 */
public class InProcessTeammateTask {

    private static final Logger log = LoggerFactory.getLogger(InProcessTeammateTask.class);

    private final TaskState taskState;
    private volatile boolean active;

    public InProcessTeammateTask(TaskState taskState) {
        this.taskState = taskState;
        this.active = false;
    }

    public String getTaskId() {
        return taskState.id();
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Starts the in-process teammate.
     */
    public void start() {
        active = true;
        log.info("In-process teammate task {} started", taskState.id());
    }

    /**
     * Stops the in-process teammate.
     */
    public void stop() {
        active = false;
        log.info("In-process teammate task {} stopped", taskState.id());
    }
}
