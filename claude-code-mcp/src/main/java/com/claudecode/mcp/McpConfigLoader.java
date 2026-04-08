package com.claudecode.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads and merges MCP server configuration from multiple levels.
 * <p>
 * Merge order (later overrides earlier):
 * <ol>
 *   <li>User-level: ~/.claude/mcp.json</li>
 *   <li>Workspace-level: {projectDir}/.claude/mcp.json</li>
 * </ol>
 */
public class McpConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(McpConfigLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Loads and merges MCP configuration for the given project directory.
     *
     * @param projectDir the workspace root directory
     * @return merged MCP configuration
     */
    public static McpConfig loadConfig(Path projectDir) {
        Map<String, McpServerConfig> merged = new LinkedHashMap<>();

        // 1. User-level config
        Path userConfig = getUserConfigPath();
        mergeFrom(userConfig, merged);

        // 2. Workspace-level config
        if (projectDir != null) {
            Path workspaceConfig = projectDir.resolve(".claude").resolve("mcp.json");
            mergeFrom(workspaceConfig, merged);
        }

        return new McpConfig(Collections.unmodifiableMap(merged));
    }

    /**
     * Returns the user-level MCP config path (~/.claude/mcp.json).
     */
    static Path getUserConfigPath() {
        return Path.of(System.getProperty("user.home"), ".claude", "mcp.json");
    }

    /**
     * Parses a single mcp.json file and merges its servers into the target map.
     */
    static void mergeFrom(Path configPath, Map<String, McpServerConfig> target) {
        if (!Files.isRegularFile(configPath)) {
            return;
        }

        try {
            String content = Files.readString(configPath);
            JsonNode root = MAPPER.readTree(content);

            JsonNode mcpServers = root.has("mcpServers") ? root.get("mcpServers") : root;
            if (!mcpServers.isObject()) {
                return;
            }

            Iterator<Map.Entry<String, JsonNode>> fields = mcpServers.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String name = entry.getKey();
                JsonNode serverNode = entry.getValue();

                McpServerConfig config = parseServerConfig(name, serverNode);
                target.put(name, config);
            }

            LOG.debug("Loaded MCP config from {}: {} servers", configPath, mcpServers.size());
        } catch (IOException e) {
            LOG.warn("Failed to read MCP config from {}", configPath, e);
        }
    }

    /**
     * Parses a single server configuration node.
     */
    static McpServerConfig parseServerConfig(String name, JsonNode node) {
        String command = node.has("command") ? node.get("command").asText() : "";
        List<String> args = parseStringList(node.get("args"));
        Map<String, String> env = parseStringMap(node.get("env"));
        boolean disabled = node.has("disabled") && node.get("disabled").asBoolean();
        List<String> autoApprove = parseStringList(node.get("autoApprove"));
        String transportType = node.has("type") ? node.get("type").asText() : "stdio";

        return new McpServerConfig(name, command, args, env, disabled, autoApprove, transportType);
    }

    private static List<String> parseStringList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> list = new ArrayList<>();
        for (JsonNode item : node) {
            list.add(item.asText());
        }
        return List.copyOf(list);
    }

    private static Map<String, String> parseStringMap(JsonNode node) {
        if (node == null || !node.isObject()) return Map.of();
        Map<String, String> map = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            map.put(entry.getKey(), entry.getValue().asText());
        }
        return Map.copyOf(map);
    }
}
