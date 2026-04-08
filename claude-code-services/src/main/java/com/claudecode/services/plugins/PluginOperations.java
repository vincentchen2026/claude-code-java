package com.claudecode.services.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PluginOperations {

    private static final Logger log = LoggerFactory.getLogger(PluginOperations.class);

    private final BuiltinPluginRegistry registry;
    private final PluginInstallationManager installationManager;
    private final Map<String, PluginInstance> activePlugins;
    private final List<PluginLifecycleListener> listeners;

    public PluginOperations(BuiltinPluginRegistry registry, PluginInstallationManager installationManager) {
        this.registry = registry;
        this.installationManager = installationManager;
        this.activePlugins = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void addListener(PluginLifecycleListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PluginLifecycleListener listener) {
        listeners.remove(listener);
    }

    public boolean enablePlugin(String pluginId) {
        boolean success = registry.enable(pluginId);
        if (success) {
            notifyListeners(pluginId, PluginEvent.ENABLED);
        }
        return success;
    }

    public boolean disablePlugin(String pluginId) {
        boolean success = registry.disable(pluginId);
        if (success) {
            unloadPlugin(pluginId);
            notifyListeners(pluginId, PluginEvent.DISABLED);
        }
        return success;
    }

    public void configurePlugin(String pluginId, Map<String, String> config) {
        PluginInstance instance = activePlugins.get(pluginId);
        if (instance != null) {
            instance.updateConfig(config);
            notifyListeners(pluginId, PluginEvent.RECONFIGURED);
        }
    }

    public PluginInfo getPluginInfo(String pluginId) {
        var builtin = registry.getPlugin(pluginId);
        if (builtin.isPresent()) {
            return new PluginInfo(
                pluginId,
                builtin.get().name(),
                builtin.get().description(),
                registry.isEnabled(pluginId),
                PluginSource.BUILTIN,
                null
            );
        }

        var metadata = installationManager.getMetadata(pluginId);
        if (metadata != null) {
            return new PluginInfo(
                pluginId,
                metadata.name(),
                metadata.description(),
                installationManager.isInstalled(pluginId),
                PluginSource.INSTALLED,
                installationManager.getPluginPath(pluginId)
            );
        }

        return null;
    }

    public List<PluginInfo> listAllPlugins() {
        List<PluginInfo> result = new CopyOnWriteArrayList<>();
        
        for (var plugin : registry.getAllPlugins()) {
            result.add(new PluginInfo(
                plugin.id(),
                plugin.name(),
                plugin.description(),
                registry.isEnabled(plugin.id()),
                PluginSource.BUILTIN,
                null
            ));
        }

        return result;
    }

    private void loadPlugin(String pluginId) {
        if (activePlugins.containsKey(pluginId)) {
            return;
        }

        try {
            var path = installationManager.getPluginPath(pluginId);
            var instance = new PluginInstance(pluginId, path);
            activePlugins.put(pluginId, instance);
            log.info("Loaded plugin: {}", pluginId);
            notifyListeners(pluginId, PluginEvent.LOADED);
        } catch (Exception e) {
            log.error("Failed to load plugin: {}", pluginId, e);
        }
    }

    private void unloadPlugin(String pluginId) {
        var instance = activePlugins.remove(pluginId);
        if (instance != null) {
            instance.shutdown();
            log.info("Unloaded plugin: {}", pluginId);
            notifyListeners(pluginId, PluginEvent.UNLOADED);
        }
    }

    private void notifyListeners(String pluginId, PluginEvent event) {
        for (var listener : listeners) {
            try {
                listener.onPluginEvent(pluginId, event);
            } catch (Exception e) {
                log.warn("Plugin lifecycle listener threw exception", e);
            }
        }
    }

    public enum PluginSource {
        BUILTIN, INSTALLED
    }

    public enum PluginEvent {
        ENABLED, DISABLED, LOADED, UNLOADED, RECONFIGURED
    }

    public record PluginInfo(
        String id,
        String name,
        String description,
        boolean enabled,
        PluginSource source,
        Path installPath
    ) {}

    private static class PluginInstance {
        private final String id;
        private final Path path;
        private final ClassLoader classLoader;
        private volatile Map<String, String> config;
        private volatile boolean loaded;

        PluginInstance(String id, Path path) throws Exception {
            this.id = id;
            this.path = path;
            this.classLoader = new PluginClassLoader(path);
            this.config = Map.of();
            this.loaded = true;
        }

        void updateConfig(Map<String, String> config) {
            this.config = config;
        }

        void shutdown() {
            this.loaded = false;
        }
    }

    private static class PluginClassLoader extends ClassLoader {
        PluginClassLoader(Path jarPath) {
        }
    }

    public interface PluginLifecycleListener {
        void onPluginEvent(String pluginId, PluginEvent event);
    }
}