package com.claudecode.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP transport over subprocess stdin/stdout using JSON-RPC 2.0.
 * Starts the MCP server as a child process and communicates via stdio.
 */
public class StdioTransport implements McpTransport {

    private static final Logger LOG = LoggerFactory.getLogger(StdioTransport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final McpServerConfig config;
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final AtomicInteger requestId = new AtomicInteger(0);

    public StdioTransport(McpServerConfig config) {
        this.config = config;
    }

    /**
     * Starts the MCP server subprocess.
     */
    public void start() {
        try {
            List<String> command = new ArrayList<>();
            command.add(config.command());
            command.addAll(config.args());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);

            // Merge environment variables
            Map<String, String> processEnv = pb.environment();
            processEnv.putAll(config.env());

            process = pb.start();
            writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            LOG.debug("Started MCP server '{}': {} {}", config.name(), config.command(), config.args());
        } catch (IOException e) {
            throw new McpException("Failed to start MCP server '" + config.name() + "'", e);
        }
    }

    @Override
    public JsonNode sendRequest(String method, JsonNode params) {
        if (!isConnected()) {
            throw new McpException("Transport not connected for server '" + config.name() + "'");
        }

        int id = requestId.incrementAndGet();
        try {
            // Build JSON-RPC 2.0 request
            ObjectNode request = MAPPER.createObjectNode();
            request.put("jsonrpc", "2.0");
            request.put("id", id);
            request.put("method", method);
            if (params != null) {
                request.set("params", params);
            }

            String jsonLine = MAPPER.writeValueAsString(request);
            synchronized (this) {
                writer.write(jsonLine);
                writer.newLine();
                writer.flush();

                // Read response line
                String responseLine = reader.readLine();
                if (responseLine == null) {
                    throw new McpException("MCP server '" + config.name() + "' closed connection");
                }

                JsonNode response = MAPPER.readTree(responseLine);

                // Check for JSON-RPC error
                if (response.has("error")) {
                    JsonNode error = response.get("error");
                    String message = error.has("message") ? error.get("message").asText() : "Unknown error";
                    throw new McpException("MCP server error: " + message);
                }

                return response.has("result") ? response.get("result") : MAPPER.nullNode();
            }
        } catch (McpException e) {
            throw e;
        } catch (IOException e) {
            throw new McpException("Communication error with MCP server '" + config.name() + "'", e);
        }
    }

    @Override
    public boolean isConnected() {
        return process != null && process.isAlive();
    }

    @Override
    public void close() throws Exception {
        if (writer != null) {
            try { writer.close(); } catch (IOException ignored) {}
        }
        if (reader != null) {
            try { reader.close(); } catch (IOException ignored) {}
        }
        if (process != null) {
            process.destroyForcibly();
            process.waitFor();
        }
        LOG.debug("Closed MCP server '{}'", config.name());
    }
}
