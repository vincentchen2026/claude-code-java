package com.claudecode.services.tasks;

import com.claudecode.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for task management tools.
 * Uses TaskStore to provide persistent task management.
 */
public class TaskToolProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TaskToolProvider.class);

    private final TaskStore taskStore;
    private boolean initialized = false;

    public TaskToolProvider() {
        this.taskStore = new TaskStore();
    }

    public TaskToolProvider(TaskStore taskStore) {
        this.taskStore = taskStore;
    }

    /**
     * Registers all task-related tools into the registry.
     *
     * @param registry the tool registry to register tools into
     */
    public void initialize(ToolRegistry registry) {
        if (initialized) {
            LOG.warn("TaskToolProvider already initialized");
            return;
        }

        // Register task management tools with TaskStore
        registry.register(new TaskCreateTool(taskStore));
        registry.register(new TaskGetTool(taskStore));
        registry.register(new TaskListTool(taskStore));
        registry.register(new TaskStopTool(taskStore));
        registry.register(new TaskUpdateTool(taskStore));

        initialized = true;
        LOG.info("Task tools initialized with TaskStore");
    }

    public TaskStore getTaskStore() {
        return taskStore;
    }
}
