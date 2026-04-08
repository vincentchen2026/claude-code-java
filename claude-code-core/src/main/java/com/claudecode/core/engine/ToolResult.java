package com.claudecode.core.engine;

import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.TextBlock;

import java.util.List;

/**
 * Result of executing a tool.
 */
public record ToolResult(
    List<ContentBlock> content,
    boolean isError
) {

    /**
     * Creates a successful text result.
     */
    public static ToolResult success(String text) {
        return new ToolResult(List.of(new TextBlock(text)), false);
    }

    /**
     * Creates an error result.
     */
    public static ToolResult error(String errorMessage) {
        return new ToolResult(List.of(new TextBlock(errorMessage)), true);
    }
}
