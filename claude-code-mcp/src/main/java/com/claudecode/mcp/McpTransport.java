package com.claudecode.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Transport layer for MCP server communication.
 * Implementations handle the actual wire protocol (stdio, SSE, etc.).
 */
public interface McpTransport extends AutoCloseable {

    /**
     * Sends a JSON-RPC request and returns the result.
     *
     * @param method the JSON-RPC method name
     * @param params the parameters as a JSON node
     * @return the result JSON node from the response
     * @throws McpException if the request fails or the server returns an error
     */
    JsonNode sendRequest(String method, JsonNode params);

    /**
     * Returns true if the transport is currently connected and usable.
     */
    boolean isConnected();
}
