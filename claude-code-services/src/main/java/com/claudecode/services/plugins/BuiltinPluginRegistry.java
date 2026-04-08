package com.claudecode.services.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Registry of built-in plugins with name@builtin IDs.
 * Manages plugin enable/disable state via settings.
 */
public class BuiltinPluginRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(BuiltinPluginRegistry.class);

    private final Map<String, BuiltinPlugin> plugins;
    private final Set<String> enabledPlugins;

    public BuiltinPluginRegistry() {
        this.plugins = new LinkedHashMap<>();
        this.enabledPlugins = new LinkedHashSet<>();
    }

    /**
     * Register a built-in plugin.
     *
     * @param plugin the plugin to register
     */
    public void register(BuiltinPlugin plugin) {
        plugins.put(plugin.id(), plugin);
        if (plugin.enabledByDefault()) {
            enabledPlugins.add(plugin.id());
        }
        LOG.debug("Registered plugin: {}", plugin.id());
    }

    /**
     * Enable a plugin by ID.
     *
     * @param pluginId the plugin ID (name@builtin)
     * @return true if the plugin was found and enabled
     */
    public boolean enable(String pluginId) {
        if (!plugins.containsKey(pluginId)) {
            return false;
        }
        enabledPlugins.add(pluginId);
        LOG.info("Enabled plugin: {}", pluginId);
        return true;
    }

    /**
     * Disable a plugin by ID.
     *
     * @param pluginId the plugin ID (name@builtin)
     * @return true if the plugin was found and disabled
     */
    public boolean disable(String pluginId) {
        if (!plugins.containsKey(pluginId)) {
            return false;
        }
        enabledPlugins.remove(pluginId);
        LOG.info("Disabled plugin: {}", pluginId);
        return true;
    }

    /**
     * Check if a plugin is enabled.
     */
    public boolean isEnabled(String pluginId) {
        return enabledPlugins.contains(pluginId);
    }

    /**
     * Get a plugin by ID.
     */
    public Optional<BuiltinPlugin> getPlugin(String pluginId) {
        return Optional.ofNullable(plugins.get(pluginId));
    }

    /**
     * Get all registered plugins.
     */
    public List<BuiltinPlugin> getAllPlugins() {
        return List.copyOf(plugins.values());
    }

    /**
     * Get all enabled plugins.
     */
    public List<BuiltinPlugin> getEnabledPlugins() {
        return plugins.values().stream()
                .filter(p -> enabledPlugins.contains(p.id()))
                .toList();
    }

    /**
     * Get all disabled plugins.
     */
    public List<BuiltinPlugin> getDisabledPlugins() {
        return plugins.values().stream()
                .filter(p -> !enabledPlugins.contains(p.id()))
                .toList();
    }

    /**
     * Handle /plugin command (stub).
     * Returns a formatted list of plugins and their status.
     *
     * @param args command arguments (e.g., "enable name@builtin", "disable name@builtin", "list")
     * @return command output
     */
    public String handlePluginCommand(String args) {
        if (args == null || args.isBlank() || "list".equals(args.trim())) {
            return formatPluginList();
        }

        String[] parts = args.trim().split("\\s+", 2);
        if (parts.length < 2) {
            return "Usage: /plugin <enable|disable|list> [plugin-id]";
        }

        String action = parts[0];
        String pluginId = parts[1];

        return switch (action) {
            case "enable" -> enable(pluginId)
                    ? "Enabled: " + pluginId
                    : "Plugin not found: " + pluginId;
            case "disable" -> disable(pluginId)
                    ? "Disabled: " + pluginId
                    : "Plugin not found: " + pluginId;
            default -> "Unknown action: " + action + ". Use enable, disable, or list.";
        };
    }

    private String formatPluginList() {
        if (plugins.isEmpty()) {
            return "No plugins registered.";
        }

        StringBuilder sb = new StringBuilder("Plugins:\n");
        for (BuiltinPlugin plugin : plugins.values()) {
            String status = enabledPlugins.contains(plugin.id()) ? "enabled" : "disabled";
            sb.append("  ").append(plugin.id())
              .append(" - ").append(plugin.description())
              .append(" [").append(status).append("]\n");
        }
        return sb.toString().trim();
    }
}
