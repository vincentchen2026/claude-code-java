package com.claudecode.tools;

import com.claudecode.core.engine.ToolExecutionContext;
import com.claudecode.core.engine.ToolExecutor;
import com.claudecode.core.engine.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Registry for managing tool instances.
 * Implements ToolExecutor to bridge with QueryEngine.
 */
public class ToolRegistry implements ToolExecutor {

    private final Map<String, Tool<?, ?>> tools = new ConcurrentHashMap<>();

    /** Registers a tool. Replaces any existing tool with the same name. */
    public void register(Tool<?, ?> tool) {
        Objects.requireNonNull(tool, "tool must not be null");
        tools.put(tool.name(), tool);
    }

    /** Gets a tool by name. */
    public Optional<Tool<?, ?>> get(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /** Returns all registered tools. */
    public Collection<Tool<?, ?>> getAll() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /** Returns tools matching the given predicate. */
    public List<Tool<?, ?>> filter(Predicate<Tool<?, ?>> predicate) {
        return tools.values().stream()
                .filter(predicate)
                .toList();
    }

    /** Returns the number of registered tools. */
    public int size() {
        return tools.size();
    }

    /**
     * Implements ToolExecutor to bridge with QueryEngine.
     * Looks up the tool by name, converts JsonNode input, and executes.
     */
    @Override
    public ToolResult execute(String toolName, JsonNode input, ToolExecutionContext context) {
        Tool<?, ?> tool = tools.get(toolName);
        if (tool == null) {
            return ToolResult.error("Unknown tool: " + toolName);
        }
        try {
            Object result = executeRaw(tool, input, context);
            return ToolResult.success(result != null ? result.toString() : "");
        } catch (Exception e) {
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    @Override
    public java.util.List<com.claudecode.core.engine.StreamingClient.StreamRequest.ToolDef> getToolDefinitions() {
        return tools.values().stream()
            .filter(Tool::isEnabled)
            .map(t -> new com.claudecode.core.engine.StreamingClient.StreamRequest.ToolDef(
                t.name(), t.description(), t.inputSchema()))
            .toList();
    }

    @SuppressWarnings("unchecked")
    private <I, O> O executeRaw(Tool<I, O> tool, JsonNode input, ToolExecutionContext context) {
        // Tools that accept JsonNode directly
        return tool.call((I) input, context);
    }
}
