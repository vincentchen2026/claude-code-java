package com.claudecode.services.tasks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * In-memory task store with CRUD operations and notification support.
 */
public class TaskStore {

    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();
    private final List<Consumer<TaskState>> completionListeners = new ArrayList<>();

    public TaskState create(TaskType type, String description) {
        TaskState task = TaskState.create(type, description);
        tasks.put(task.id(), task);
        return task;
    }

    public Optional<TaskState> get(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public List<TaskState> list() {
        return List.copyOf(tasks.values());
    }

    public List<TaskState> listByStatus(TaskStatus status) {
        return tasks.values().stream()
            .filter(t -> t.status() == status)
            .toList();
    }

    public TaskState updateStatus(String taskId, TaskStatus newStatus) {
        TaskState current = tasks.get(taskId);
        if (current == null) {
            throw new NoSuchElementException("Task not found: " + taskId);
        }
        TaskState updated = current.withStatus(newStatus);
        tasks.put(taskId, updated);
        if (newStatus.isTerminal()) {
            notifyCompletion(updated);
        }
        return updated;
    }

    public TaskState updateDescription(String taskId, String description) {
        TaskState current = tasks.get(taskId);
        if (current == null) {
            throw new NoSuchElementException("Task not found: " + taskId);
        }
        TaskState updated = current.withDescription(description);
        tasks.put(taskId, updated);
        return updated;
    }

    public void onCompletion(Consumer<TaskState> listener) {
        completionListeners.add(listener);
    }

    private void notifyCompletion(TaskState task) {
        for (Consumer<TaskState> listener : completionListeners) {
            try {
                listener.accept(task);
            } catch (Exception ignored) {
                // Don't let listener failures propagate
            }
        }
    }

    public int size() {
        return tasks.size();
    }
}
