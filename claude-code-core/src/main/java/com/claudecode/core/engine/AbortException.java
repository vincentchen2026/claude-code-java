package com.claudecode.core.engine;

/**
 * Thrown when an operation is aborted via {@link AbortController}.
 */
public class AbortException extends RuntimeException {

    public AbortException(String message) {
        super(message);
    }
}
