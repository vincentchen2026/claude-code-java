package com.claudecode.services.tasks;

import java.security.SecureRandom;

/**
 * Generates unique task IDs with type prefix + random alphanumeric suffix.
 */
public final class TaskIdGenerator {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int SUFFIX_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TaskIdGenerator() {}

    /**
     * Generates a task ID like "b3k9x2m1" for LOCAL_BASH.
     */
    public static String generate(TaskType type) {
        StringBuilder sb = new StringBuilder(type.prefix());
        byte[] bytes = new byte[SUFFIX_LENGTH];
        RANDOM.nextBytes(bytes);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(ALPHABET.charAt(Byte.toUnsignedInt(bytes[i]) % ALPHABET.length()));
        }
        return sb.toString();
    }

    /**
     * Extracts the task type from a task ID by its prefix.
     */
    public static TaskType extractType(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be null or empty");
        }
        String prefix = taskId.substring(0, 1);
        for (TaskType type : TaskType.values()) {
            if (type.prefix().equals(prefix)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown task type prefix: " + prefix);
    }
}
