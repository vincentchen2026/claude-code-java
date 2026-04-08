package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.permissions.PermissionDecision;
import com.claudecode.permissions.ToolPermissionContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.BiFunction;

/**
 * Builder for creating Tool instances from lambdas/functions.
 * Useful for simple tools that don't need a full class.
 *
 * @param <I> the input type
 * @param <O> the output type
 */
public class ToolBuilder<I, O> {

    private String name;
    private String description;
    private JsonNode inputSchema;
    private BiFunction<I, ToolExecutionContext, O> callFn;
    private BiFunction<I, ToolPermissionContext, PermissionDecision> permissionFn;
    private boolean concurrencySafe = false;
    private boolean readOnly = false;

    public ToolBuilder<I, O> name(String name) {
        this.name = name;
        return this;
    }

    public ToolBuilder<I, O> description(String description) {
        this.description = description;
        return this;
    }

    public ToolBuilder<I, O> inputSchema(JsonNode inputSchema) {
        this.inputSchema = inputSchema;
        return this;
    }

    public ToolBuilder<I, O> call(BiFunction<I, ToolExecutionContext, O> callFn) {
        this.callFn = callFn;
        return this;
    }

    public ToolBuilder<I, O> permissions(BiFunction<I, ToolPermissionContext, PermissionDecision> permissionFn) {
        this.permissionFn = permissionFn;
        return this;
    }

    public ToolBuilder<I, O> concurrencySafe(boolean concurrencySafe) {
        this.concurrencySafe = concurrencySafe;
        return this;
    }

    public ToolBuilder<I, O> readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public Tool<I, O> build() {
        if (name == null) throw new IllegalStateException("Tool name is required");
        if (callFn == null) throw new IllegalStateException("Tool call function is required");

        return new BuiltTool<>(name, description, inputSchema, callFn,
                permissionFn, concurrencySafe, readOnly);
    }

    /**
     * Internal Tool implementation backed by builder-provided functions.
     */
    private static final class BuiltTool<I, O> extends Tool<I, O> {
        private final String name;
        private final String description;
        private final JsonNode inputSchema;
        private final BiFunction<I, ToolExecutionContext, O> callFn;
        private final BiFunction<I, ToolPermissionContext, PermissionDecision> permissionFn;
        private final boolean concurrencySafe;
        private final boolean readOnly;

        BuiltTool(String name, String description, JsonNode inputSchema,
                  BiFunction<I, ToolExecutionContext, O> callFn,
                  BiFunction<I, ToolPermissionContext, PermissionDecision> permissionFn,
                  boolean concurrencySafe, boolean readOnly) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
            this.callFn = callFn;
            this.permissionFn = permissionFn;
            this.concurrencySafe = concurrencySafe;
            this.readOnly = readOnly;
        }

        @Override
        public String name() { return name; }

        @Override
        public String description() { return description != null ? description : ""; }

        @Override
        public JsonNode inputSchema() { return inputSchema; }

        @Override
        public O call(I input, ToolExecutionContext context) {
            return callFn.apply(input, context);
        }

        @Override
        public PermissionDecision checkPermissions(I input, ToolPermissionContext permCtx) {
            if (permissionFn != null) return permissionFn.apply(input, permCtx);
            return super.checkPermissions(input, permCtx);
        }

        @Override
        public boolean isConcurrencySafe() { return concurrencySafe; }

        @Override
        public boolean isReadOnly() { return readOnly; }
    }
}
