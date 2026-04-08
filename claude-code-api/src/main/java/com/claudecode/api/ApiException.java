package com.claudecode.api;

/**
 * Exception thrown by API client operations.
 * Carries HTTP status code for retry logic.
 */
public class ApiException extends RuntimeException {

    private final int statusCode;
    private final String errorType;

    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = null;
    }

    public ApiException(String message, int statusCode, String errorType) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    public ApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorType = null;
    }

    public int statusCode() {
        return statusCode;
    }

    public String errorType() {
        return errorType;
    }
}
