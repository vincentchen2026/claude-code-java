package com.claudecode.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Describes a tool discovered from an MCP server.
 *
 * @param serverId    the MCP server that provides this tool
 * @param name        tool name
 * @param description human-readable description
 * @param inputSchema JSON Schema for the tool's input
 */
public record McpToolInfo(
    String serverId,
    String name,
    String description,
    JsonNode inputSchema
) {}
