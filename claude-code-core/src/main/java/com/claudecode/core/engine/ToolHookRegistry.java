package com.claudecode.core.engine;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ToolHookRegistry {

    private final List<ToolHook> preHooks;
    private final List<ToolHook> postHooks;
    private final List<ToolErrorHook> errorHooks;

    public ToolHookRegistry() {
        this.preHooks = new CopyOnWriteArrayList<>();
        this.postHooks = new CopyOnWriteArrayList<>();
        this.errorHooks = new CopyOnWriteArrayList<>();
    }

    public void addPreHook(ToolHook hook) {
        preHooks.add(hook);
    }

    public void addPostHook(ToolHook hook) {
        postHooks.add(hook);
    }

    public void addErrorHook(ToolErrorHook hook) {
        errorHooks.add(hook);
    }

    public void removePreHook(ToolHook hook) {
        preHooks.remove(hook);
    }

    public void removePostHook(ToolHook hook) {
        postHooks.remove(hook);
    }

    public void removeErrorHook(ToolErrorHook hook) {
        errorHooks.remove(hook);
    }

    public void invokePreExecute(String toolName, JsonNode input, ToolExecutionContext context) {
        for (ToolHook hook : preHooks) {
            try {
                hook.onPreExecute(toolName, input, context);
            } catch (Exception e) {
                // Log but don't fail
            }
        }
    }

    public void invokePostExecute(String toolName, JsonNode input, ToolExecutionContext context, ToolResult result) {
        for (ToolHook hook : postHooks) {
            try {
                hook.onPostExecute(toolName, input, context, result);
            } catch (Exception e) {
                // Log but don't fail
            }
        }
    }

    public void invokeOnError(String toolName, JsonNode input, ToolExecutionContext context, Throwable error) {
        for (ToolErrorHook hook : errorHooks) {
            try {
                hook.onError(toolName, input, context, error);
            } catch (Exception e) {
                // Log but don't fail
            }
        }
    }

    public int getPreHookCount() {
        return preHooks.size();
    }

    public int getPostHookCount() {
        return postHooks.size();
    }

    public int getErrorHookCount() {
        return errorHooks.size();
    }

    public void clear() {
        preHooks.clear();
        postHooks.clear();
        errorHooks.clear();
    }

    @FunctionalInterface
    public interface ToolHook {
        void onPreExecute(String toolName, JsonNode input, ToolExecutionContext context);
        default void onPostExecute(String toolName, JsonNode input, ToolExecutionContext context, ToolResult result) {}
    }

    @FunctionalInterface
    public interface ToolErrorHook {
        void onError(String toolName, JsonNode input, ToolExecutionContext context, Throwable error);
    }
}