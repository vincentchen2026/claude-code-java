package com.claudecode.core.engine;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;

public class StreamingToolExecutor implements ToolExecutor {

    private final ConcurrentHashMap<String, StreamingToolHandler> handlers;
    private final ToolHookRegistry hookRegistry;

    public StreamingToolExecutor() {
        this.handlers = new ConcurrentHashMap<>();
        this.hookRegistry = new ToolHookRegistry();
    }

    @Override
    public ToolResult execute(String toolName, JsonNode input, ToolExecutionContext context) {
        StreamingToolHandler handler = handlers.get(toolName);
        if (handler == null) {
            return ToolResult.error("Unknown tool: " + toolName);
        }

        hookRegistry.invokePreExecute(toolName, input, context);

        try {
            Flow.Publisher<ToolProgressEvent> progressPublisher = handler.executeStreaming(input, context);
            
            if (progressPublisher == null) {
                return ToolResult.success("Tool execution completed");
            }

            return ToolResult.success("Tool execution started with progress streaming");
        } catch (Exception e) {
            hookRegistry.invokeOnError(toolName, input, context, e);
            return ToolResult.error(e.getMessage());
        }
    }

    public void registerHandler(String toolName, StreamingToolHandler handler) {
        handlers.put(toolName, handler);
    }

    public void unregisterHandler(String toolName) {
        handlers.remove(toolName);
    }

    public boolean hasHandler(String toolName) {
        return handlers.containsKey(toolName);
    }

    public ToolHookRegistry getHookRegistry() {
        return hookRegistry;
    }

    public List<String> getRegisteredTools() {
        return List.copyOf(handlers.keySet());
    }

    @FunctionalInterface
    public interface StreamingToolHandler {
        Flow.Publisher<ToolProgressEvent> executeStreaming(JsonNode input, ToolExecutionContext context);
    }

    public record ToolProgressEvent(
        String toolName,
        String message,
        double progress,
        long timestamp
    ) {}
}