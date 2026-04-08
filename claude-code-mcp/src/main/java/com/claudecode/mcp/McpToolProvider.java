package com.claudecode.mcp;

import com.claudecode.tools.Tool;
import com.claudecode.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Discovers and registers MCP tools into the tool registry.
 * Loads MCP configuration, connects to servers, and registers discovered tools.
 */
public class McpToolProvider {

    private static final Logger LOG = LoggerFactory.getLogger(McpToolProvider.class);

    private final McpClientManager clientManager;
    private boolean initialized = false;

    public McpToolProvider() {
        this.clientManager = new McpClientManager();
    }

    public McpToolProvider(McpClientManager clientManager) {
        this.clientManager = clientManager;
    }

    /**
     * Initializes MCP connections and registers tools.
     * Should be called once during startup.
     *
     * @param projectDir the workspace directory for loading config
     * @param registry   the tool registry to register tools into
     */
    public void initialize(Path projectDir, ToolRegistry registry) {
        if (initialized) {
            LOG.warn("McpToolProvider already initialized");
            return;
        }

        try {
            // Load MCP configuration
            McpConfig config = McpConfigLoader.loadConfig(projectDir);

            // Register static tools
            registry.register(new McpAuthTool());

            // Connect to servers and register discovered tools
            for (McpServerConfig serverConfig : config.servers().values()) {
                try {
                    connectAndRegister(serverConfig, registry);
                } catch (Exception e) {
                    LOG.warn("Failed to connect to MCP server '{}': {}",
                        serverConfig.name(), e.getMessage());
                }
            }

            // Register resource tools (they use lazy connection lookup)
            registry.register(new ListMcpResourcesTool(clientManager));
            registry.register(new ReadMcpResourceTool(clientManager));

            initialized = true;
            LOG.info("MCP tools initialized: {} servers connected",
                clientManager.getConnectedServerIds().size());
        } catch (Exception e) {
            LOG.error("Failed to initialize MCP tools", e);
        }
    }

    private void connectAndRegister(McpServerConfig config, ToolRegistry registry) {
        if (config.disabled()) {
            LOG.debug("MCP server '{}' is disabled", config.name());
            return;
        }

        McpConnection connection = clientManager.connect(config);
        // listTools() connects and discovers tools
        List<McpToolInfo> tools = clientManager.listToolsForServer(config.name());

        for (McpToolInfo toolInfo : tools) {
            MCPTool tool = new MCPTool(toolInfo, clientManager);
            registry.register(tool);
            LOG.debug("Registered MCP tool: {}", tool.name());
        }

        LOG.info("Connected to MCP server '{}' with {} tools",
            config.name(), tools.size());
    }

    /**
     * Returns the client manager for direct access if needed.
     */
    public McpClientManager getClientManager() {
        return clientManager;
    }

    /**
     * Shuts down all MCP connections.
     */
    public void shutdown() {
        try {
            clientManager.close();
            initialized = false;
        } catch (Exception e) {
            LOG.warn("Error shutting down MCP client manager", e);
        }
    }
}
