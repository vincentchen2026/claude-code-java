package com.claudecode.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link McpConfigLoader}.
 */
class McpConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadConfigFromEmptyDir() {
        McpConfig config = McpConfigLoader.loadConfig(tempDir);
        assertNotNull(config);
        assertTrue(config.servers().isEmpty());
    }

    @Test
    void loadConfigFromWorkspaceLevel() throws IOException {
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("mcp.json"), """
            {
              "mcpServers": {
                "my-server": {
                  "command": "node",
                  "args": ["server.js"],
                  "env": {"PORT": "3000"},
                  "type": "stdio"
                }
              }
            }
            """);

        McpConfig config = McpConfigLoader.loadConfig(tempDir);
        assertEquals(1, config.servers().size());

        McpServerConfig server = config.servers().get("my-server");
        assertNotNull(server);
        assertEquals("my-server", server.name());
        assertEquals("node", server.command());
        assertEquals(1, server.args().size());
        assertEquals("server.js", server.args().get(0));
        assertEquals("3000", server.env().get("PORT"));
        assertEquals("stdio", server.transportType());
        assertFalse(server.disabled());
    }

    @Test
    void loadConfigWithDisabledServer() throws IOException {
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("mcp.json"), """
            {
              "mcpServers": {
                "disabled-srv": {
                  "command": "python",
                  "disabled": true
                }
              }
            }
            """);

        McpConfig config = McpConfigLoader.loadConfig(tempDir);
        McpServerConfig server = config.servers().get("disabled-srv");
        assertNotNull(server);
        assertTrue(server.disabled());
        assertTrue(config.enabledServers().isEmpty());
    }

    @Test
    void loadConfigWithAutoApprove() throws IOException {
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("mcp.json"), """
            {
              "mcpServers": {
                "srv": {
                  "command": "cmd",
                  "autoApprove": ["tool1", "tool2"]
                }
              }
            }
            """);

        McpConfig config = McpConfigLoader.loadConfig(tempDir);
        McpServerConfig server = config.servers().get("srv");
        assertEquals(2, server.autoApprove().size());
        assertEquals("tool1", server.autoApprove().get(0));
        assertEquals("tool2", server.autoApprove().get(1));
    }

    @Test
    void mergeFromOverridesExistingEntries() {
        Map<String, McpServerConfig> target = new LinkedHashMap<>();
        target.put("srv", new McpServerConfig("srv", "old-cmd", null, null, false, null, "stdio"));

        // Create a config file that overrides "srv"
        try {
            Path claudeDir = tempDir.resolve(".claude");
            Files.createDirectories(claudeDir);
            Path configPath = claudeDir.resolve("mcp.json");
            Files.writeString(configPath, """
                {
                  "mcpServers": {
                    "srv": {
                      "command": "new-cmd"
                    }
                  }
                }
                """);

            McpConfigLoader.mergeFrom(configPath, target);
            assertEquals("new-cmd", target.get("srv").command());
        } catch (IOException e) {
            fail("Unexpected IOException", e);
        }
    }

    @Test
    void parseServerConfigDefaults() {
        // Test that missing fields get sensible defaults
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var node = mapper.createObjectNode();
        node.put("command", "test-cmd");

        McpServerConfig config = McpConfigLoader.parseServerConfig("test", node);
        assertEquals("test", config.name());
        assertEquals("test-cmd", config.command());
        assertTrue(config.args().isEmpty());
        assertTrue(config.env().isEmpty());
        assertFalse(config.disabled());
        assertTrue(config.autoApprove().isEmpty());
        assertEquals("stdio", config.transportType());
    }

    @Test
    void loadConfigWithSseTransportType() throws IOException {
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("mcp.json"), """
            {
              "mcpServers": {
                "sse-srv": {
                  "command": "http://localhost:8080",
                  "type": "sse"
                }
              }
            }
            """);

        McpConfig config = McpConfigLoader.loadConfig(tempDir);
        assertEquals("sse", config.servers().get("sse-srv").transportType());
    }

    @Test
    void loadConfigNullProjectDir() {
        // Should not throw, just skip workspace-level
        McpConfig config = McpConfigLoader.loadConfig(null);
        assertNotNull(config);
    }

    @Test
    void loadConfigWithFlatFormat() throws IOException {
        // Some mcp.json files use flat format (no "mcpServers" wrapper)
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("mcp.json"), """
            {
              "flat-srv": {
                "command": "flat-cmd"
              }
            }
            """);

        McpConfig config = McpConfigLoader.loadConfig(tempDir);
        McpServerConfig server = config.servers().get("flat-srv");
        assertNotNull(server);
        assertEquals("flat-cmd", server.command());
    }
}
