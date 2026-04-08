package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Abstract base class for all tools.
 * Each tool is a self-contained module with name, schema, execution logic,
 * and permission checks.
 *
 * @param <I> the input type
 * @param <O> the output type
 */
public abstract class Tool<I, O> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Returns the unique tool name. */
    public abstract String name();

    /** Returns a human-readable description of the tool. */
    public abstract String description();

    /** Returns the JSON Schema describing valid inputs. */
    public abstract JsonNode inputSchema();

    /** Executes the tool with the given input and context. */
    public abstract O call(I input, ToolExecutionContext context);

    /**
     * Checks whether the tool is allowed to execute with the given input.
     * Default implementation returns ASK.
     */
    public PermissionDecision checkPermissions(I input, ToolPermissionContext permCtx) {
        return PermissionDecision.ASK;
    }

    /** Whether this tool is safe for concurrent execution. Default false. */
    public boolean isConcurrencySafe() {
        return false;
    }

    /** Whether this tool only reads data (no side effects). Default false. */
    public boolean isReadOnly() {
        return false;
    }

    /** Whether this tool is currently enabled. Default true. */
    public boolean isEnabled() {
        return true;
    }

    /**
     * Helper to create a simple JSON Schema object node.
     */
    protected static ObjectNode createObjectSchema() {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", MAPPER.createObjectNode());
        return schema;
    }

    /**
     * Helper to get the shared ObjectMapper.
     */
    protected static ObjectMapper mapper() {
        return MAPPER;
    }
}
