package com.claudecode.core.engine;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Options for submitting a message to the QueryEngine.
 */
public record SubmitOptions(
    boolean abortOnStopToken,
    String querySource,
    JsonNode jsonSchema,
    int maxStructuredOutputRetries
) {
    public static final SubmitOptions DEFAULT = new SubmitOptions(false, "user", null, 3);

    public static SubmitOptions of(String querySource) {
        return new SubmitOptions(false, querySource, null, 3);
    }

    /**
     * Creates options with structured output schema.
     */
    public static SubmitOptions withSchema(String querySource, JsonNode jsonSchema) {
        return new SubmitOptions(false, querySource, jsonSchema, 3);
    }

    /**
     * Returns true if structured output mode is enabled.
     */
    public boolean hasJsonSchema() {
        return jsonSchema != null;
    }
}
