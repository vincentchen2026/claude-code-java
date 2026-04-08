package com.claudecode.services.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PluginInstallationManager {

    private static final Logger log = LoggerFactory.getLogger(PluginInstallationManager.class);

    private final Path pluginsDir;
    private final HttpClient httpClient;
    private final Map<String, PluginMetadata> installedPlugins;

    public PluginInstallationManager(Path pluginsDir) {
        this.pluginsDir = pluginsDir;
        this.httpClient = HttpClient.newHttpClient();
        this.installedPlugins = new ConcurrentHashMap<>();
        initializePluginsDir();
    }

    private void initializePluginsDir() {
        try {
            if (!Files.exists(pluginsDir)) {
                Files.createDirectories(pluginsDir);
                log.info("Created plugins directory: {}", pluginsDir);
            }
        } catch (IOException e) {
            log.error("Failed to create plugins directory", e);
        }
    }

    public CompletableFuture<InstallationResult> installFromUrl(String pluginId, URI url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Installing plugin {} from {}", pluginId, url);

                HttpRequest request = HttpRequest.newBuilder(url).GET().build();
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() != 200) {
                    return new InstallationResult(pluginId, false, "HTTP " + response.statusCode());
                }

                Path targetPath = pluginsDir.resolve(pluginId + ".jar");
                Files.write(targetPath, response.body(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                PluginMetadata metadata = loadMetadata(targetPath);
                installedPlugins.put(pluginId, metadata);

                log.info("Plugin {} installed successfully to {}", pluginId, targetPath);
                return new InstallationResult(pluginId, true, targetPath.toString());

            } catch (Exception e) {
                log.error("Failed to install plugin {}", pluginId, e);
                return new InstallationResult(pluginId, false, e.getMessage());
            }
        });
    }

    public boolean uninstall(String pluginId) {
        Path pluginPath = pluginsDir.resolve(pluginId + ".jar");
        try {
            boolean deleted = Files.deleteIfExists(pluginPath);
            if (deleted) {
                installedPlugins.remove(pluginId);
                log.info("Plugin {} uninstalled", pluginId);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to uninstall plugin {}", pluginId, e);
            return false;
        }
    }

    public boolean isInstalled(String pluginId) {
        return installedPlugins.containsKey(pluginId) ||
               Files.exists(pluginsDir.resolve(pluginId + ".jar"));
    }

    public PluginMetadata getMetadata(String pluginId) {
        return installedPlugins.get(pluginId);
    }

    public Path getPluginPath(String pluginId) {
        return pluginsDir.resolve(pluginId + ".jar");
    }

    private PluginMetadata loadMetadata(Path jarPath) {
        try (var jarFs = FileSystems.newFileSystem(jarPath, (ClassLoader) null)) {
            Path metaInf = jarFs.getPath("/META-INF/plugin.json");
            if (Files.exists(metaInf)) {
                String json = Files.readString(metaInf);
                return parseMetadata(json);
            }
        } catch (Exception e) {
            log.warn("Could not load plugin metadata from {}", jarPath, e);
        }
        return new PluginMetadata(jarPath.getFileName().toString().replace(".jar", ""), "Unknown", "1.0.0", null);
    }

    private PluginMetadata parseMetadata(String json) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, PluginMetadata.class);
        } catch (Exception e) {
            log.warn("Failed to parse plugin metadata JSON", e);
            return null;
        }
    }

    public record InstallationResult(
        String pluginId,
        boolean success,
        String message
    ) {}

    public record PluginMetadata(
        String id,
        String name,
        String version,
        String description
    ) {}
}