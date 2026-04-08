package com.claudecode.commands.impl;

import com.claudecode.commands.*;
import com.claudecode.mcp.McpClientManager;
import com.claudecode.mcp.McpConfig;
import com.claudecode.mcp.McpConfigLoader;
import com.claudecode.mcp.McpServerConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * /mcp — manage MCP server configuration.
 * Shows MCP servers, their status, and allows toggling/enabling/disabling.
 */
public class McpCommand implements Command {

    private final McpClientManager mcpClientManager;
    private final Path projectDir;

    /**
     * Creates McpCommand without MCP client manager (read-only mode).
     */
    public McpCommand() {
        this(null, null);
    }

    /**
     * Creates McpCommand with MCP client manager.
     *
     * @param mcpClientManager the MCP client manager for connections
     * @param projectDir the project directory for workspace-level config
     */
    public McpCommand(McpClientManager mcpClientManager, Path projectDir) {
        this.mcpClientManager = mcpClientManager;
        this.projectDir = projectDir;
    }

    @Override
    public String name() { return "mcp"; }

    @Override
    public String description() { return "Manage MCP server configuration"; }

    @Override
    public List<String> aliases() { return List.of("mcps"); }

    @Override
    public CommandResult execute(CommandContext context, String args) {
        String action = args != null ? args.trim().toLowerCase() : "";
        McpConfig config = McpConfigLoader.loadConfig(projectDir);

        if (action.isEmpty() || action.equals("list") || action.equals("show")) {
            return listServers(config);
        }

        if (action.equals("reload")) {
            return reloadServers(config);
        }

        if (action.startsWith("enable ")) {
            String serverName = action.substring(7).trim();
            return enableServer(config, serverName);
        }

        if (action.startsWith("disable ")) {
            String serverName = action.substring(8).trim();
            return disableServer(config, serverName);
        }

        if (action.startsWith("connect ")) {
            String serverName = action.substring(8).trim();
            return connectServer(config, serverName);
        }

        if (action.startsWith("disconnect ")) {
            String serverName = action.substring(11).trim();
            return disconnectServer(serverName);
        }

        if (action.equals("status")) {
            return serverStatus(config);
        }

        return showHelp();
    }

    private CommandResult listServers(McpConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("MCP Servers\n");
        sb.append("==========\n\n");

        var servers = config.servers();
        if (servers.isEmpty()) {
            sb.append("No MCP servers configured.\n\n");
            sb.append("Add servers to ~/.claude/mcp.json or {project}/.claude/mcp.json\n");
            sb.append("\nExample mcp.json:\n");
            sb.append("{\n");
            sb.append("  \"mcpServers\": {\n");
            sb.append("    \"my-server\": {\n");
            sb.append("      \"command\": \"npx\",\n");
            sb.append("      \"args\": [\"-y\", \"@modelcontextprotocol/server-filesystem\", \".\"]\n");
            sb.append("    }\n");
            sb.append("  }\n");
            sb.append("}\n");
            return CommandResult.of(sb.toString());
        }

        for (McpServerConfig server : servers.values()) {
            String status = getServerStatus(server.name());
            String disabledStr = server.disabled() ? " [DISABLED]" : "";
            String connectedStr = isConnected(server.name()) ? " [CONNECTED]" : "";

            sb.append(String.format("  %s%s%s%n", server.name(), disabledStr, connectedStr));
            sb.append(String.format("    command: %s%n", server.command()));
            if (!server.args().isEmpty()) {
                sb.append(String.format("    args: %s%n", String.join(" ", server.args())));
            }
            if (!server.env().isEmpty()) {
                sb.append(String.format("    env: %s keys%n", server.env().size()));
            }
            if (!server.autoApprove().isEmpty()) {
                sb.append(String.format("    auto-approve: %s%n", String.join(", ", server.autoApprove())));
            }
            sb.append(String.format("    transport: %s%n", server.transportType()));
            if (!status.isEmpty()) {
                sb.append(String.format("    status: %s%n", status));
            }
            sb.append("\n");
        }

        sb.append("Commands:\n");
        sb.append("  /mcp list           - List all servers\n");
        sb.append("  /mcp status         - Show connection status\n");
        sb.append("  /mcp connect <name> - Connect to a server\n");
        sb.append("  /mcp disconnect <name> - Disconnect from a server\n");
        sb.append("  /mcp enable <name>  - Enable a server\n");
        sb.append("  /mcp disable <name> - Disable a server\n");
        sb.append("  /mcp reload         - Reload configuration\n");

        return CommandResult.of(sb.toString());
    }

    private CommandResult serverStatus(McpConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("MCP Server Status\n");
        sb.append("=================\n\n");

        var connectedIds = mcpClientManager.getConnectedServerIds();
        if (connectedIds.isEmpty()) {
            sb.append("No MCP servers connected.\n");
            sb.append("Use /mcp connect <name> to connect.\n");
            return CommandResult.of(sb.toString());
        }

        for (String serverId : connectedIds) {
            sb.append(String.format("  %s [CONNECTED]%n", serverId));
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult connectServer(McpConfig config, String serverName) {
        var servers = config.servers();
        McpServerConfig server = servers.get(serverName);

        if (server == null) {
            return CommandResult.of("Server not found: " + serverName + "\nUse /mcp list to see available servers.");
        }

        if (server.disabled()) {
            return CommandResult.of("Server '" + serverName + "' is disabled.\nUse /mcp enable " + serverName + " first.");
        }

        if (isConnected(serverName)) {
            return CommandResult.of("Server '" + serverName + "' is already connected.");
        }

        try {
            mcpClientManager.connect(server);
            return CommandResult.of("Connected to MCP server: " + serverName);
        } catch (Exception e) {
            return CommandResult.of("Failed to connect to '" + serverName + "': " + e.getMessage());
        }
    }

    private CommandResult disconnectServer(String serverName) {
        if (!isConnected(serverName)) {
            return CommandResult.of("Server '" + serverName + "' is not connected.");
        }

        try {
            mcpClientManager.disconnect(serverName);
            return CommandResult.of("Disconnected from MCP server: " + serverName);
        } catch (Exception e) {
            return CommandResult.of("Failed to disconnect from '" + serverName + "': " + e.getMessage());
        }
    }

    private CommandResult enableServer(McpConfig config, String serverName) {
        var servers = config.servers();
        if (!servers.containsKey(serverName)) {
            return CommandResult.of("Server not found: " + serverName);
        }
        return CommandResult.of("Note: To enable a server, edit ~/.claude/mcp.json and set \"disabled\": false.\n" +
            "Then run /mcp reload.");
    }

    private CommandResult disableServer(McpConfig config, String serverName) {
        var servers = config.servers();
        if (!servers.containsKey(serverName)) {
            return CommandResult.of("Server not found: " + serverName);
        }
        return CommandResult.of("Note: To disable a server, edit ~/.claude/mcp.json and set \"disabled\": true.\n" +
            "Then run /mcp reload.");
    }

    private CommandResult reloadServers(McpConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reloading MCP configuration...\n\n");

        // Disconnect all current connections
        for (String serverId : mcpClientManager.getConnectedServerIds()) {
            try {
                mcpClientManager.disconnect(serverId);
            } catch (Exception e) {
                sb.append("Warning: Failed to disconnect " + serverId + ": " + e.getMessage() + "\n");
            }
        }

        // Connect to enabled servers
        for (McpServerConfig server : config.enabledServers()) {
            try {
                mcpClientManager.connect(server);
                sb.append("Connected: " + server.name() + "\n");
            } catch (Exception e) {
                sb.append("Failed: " + server.name() + " - " + e.getMessage() + "\n");
            }
        }

        return CommandResult.of(sb.toString());
    }

    private CommandResult showHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("MCP Command\n");
        sb.append("===========\n\n");
        sb.append("Manage Model Context Protocol (MCP) servers.\n\n");
        sb.append("Usage:\n");
        sb.append("  /mcp                - List all servers\n");
        sb.append("  /mcp list           - List all servers\n");
        sb.append("  /mcp status         - Show connection status\n");
        sb.append("  /mcp connect <name> - Connect to a server\n");
        sb.append("  /mcp disconnect <name> - Disconnect from a server\n");
        sb.append("  /mcp enable <name>  - Instructions to enable a server\n");
        sb.append("  /mcp disable <name> - Instructions to disable a server\n");
        sb.append("  /mcp reload         - Reload config and reconnect\n\n");
        sb.append("Configuration files:\n");
        sb.append("  ~/.claude/mcp.json           (user-level)\n");
        sb.append("  {project}/.claude/mcp.json   (workspace-level)\n");
        return CommandResult.of(sb.toString());
    }

    private boolean isConnected(String serverName) {
        return mcpClientManager.getConnection(serverName).isPresent();
    }

    private String getServerStatus(String serverName) {
        return isConnected(serverName) ? "connected" : "";
    }

    @Override
    public boolean isBridgeSafe() { return true; }
}
