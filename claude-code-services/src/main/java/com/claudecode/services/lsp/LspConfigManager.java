package com.claudecode.services.lsp;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LspConfigManager {

    private final Path configDir;
    private final Map<String, LspConfig> configs;

    public LspConfigManager(Path configDir) {
        this.configDir = configDir;
        this.configs = new ConcurrentHashMap<>();
    }

    public void setConfig(String serverId, LspConfig config) {
        configs.put(serverId, config);
    }

    public LspConfig getConfig(String serverId) {
        return configs.getOrDefault(serverId, LspConfig.DEFAULT);
    }

    public void removeConfig(String serverId) {
        configs.remove(serverId);
    }

    public Map<String, LspConfig> getAllConfigs() {
        return Map.copyOf(configs);
    }

    public void enableServer(String serverId) {
        LspConfig existing = configs.get(serverId);
        if (existing != null) {
            configs.put(serverId, new LspConfig(
                existing.command(),
                existing.args(),
                existing.workspaceRoot(),
                existing.env(),
                true,
                existing.traceLevel(),
                existing.diagnosticsEnabled()
            ));
        }
    }

    public void disableServer(String serverId) {
        LspConfig existing = configs.get(serverId);
        if (existing != null) {
            configs.put(serverId, new LspConfig(
                existing.command(),
                existing.args(),
                existing.workspaceRoot(),
                existing.env(),
                false,
                existing.traceLevel(),
                existing.diagnosticsEnabled()
            ));
        }
    }

    public record LspConfig(
        String command,
        String[] args,
        Path workspaceRoot,
        Map<String, String> env,
        boolean enabled,
        TraceLevel traceLevel,
        boolean diagnosticsEnabled
    ) {
        public static final LspConfig DEFAULT = new LspConfig(
            null, new String[0], null, Map.of(), true, TraceLevel.OFF, true
        );
    }

    public enum TraceLevel {
        OFF, MESSAGES, VERBOSE
    }
}