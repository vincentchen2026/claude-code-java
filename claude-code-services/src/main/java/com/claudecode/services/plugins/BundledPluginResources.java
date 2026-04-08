package com.claudecode.services.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BundledPluginResources {

    private static final Logger LOG = LoggerFactory.getLogger(BundledPluginResources.class);
    private static final String PLUGIN_RESOURCES_PATH = "META-INF/resources/claude-code/plugins/";

    private final Map<String, Map<String, byte[]>> resourceCache;
    private final ClassLoader classLoader;

    public BundledPluginResources() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public BundledPluginResources(ClassLoader classLoader) {
        this.classLoader = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
        this.resourceCache = new ConcurrentHashMap<>();
    }

    public void loadPluginResources(String pluginId) {
        String basePath = PLUGIN_RESOURCES_PATH + pluginId + "/";
        Map<String, byte[]> resources = new HashMap<>();

        try {
            Enumeration<URL> resourcesEnum = classLoader.getResources(basePath);
            while (resourcesEnum.hasMoreElements()) {
                URL url = resourcesEnum.nextElement();
                LOG.debug("Found plugin resource: {}", url);
            }
        } catch (IOException e) {
            LOG.warn("Could not enumerate resources for plugin {}: {}", pluginId, e.getMessage());
        }

        try {
            Enumeration<URL> urls = classLoader.getResources(PLUGIN_RESOURCES_PATH + pluginId);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                loadResource(url, resources);
            }
        } catch (IOException e) {
            LOG.debug("No resources found for plugin {} at path {}", pluginId, PLUGIN_RESOURCES_PATH + pluginId);
        }

        if (!resources.isEmpty()) {
            resourceCache.put(pluginId, resources);
            LOG.info("Loaded {} resources for plugin {}", resources.size(), pluginId);
        }
    }

    public byte[] getResource(String pluginId, String resourcePath) {
        Map<String, byte[]> pluginResources = resourceCache.get(pluginId);
        if (pluginResources != null) {
            return pluginResources.get(resourcePath);
        }

        String fullPath = PLUGIN_RESOURCES_PATH + pluginId + "/" + resourcePath;
        try {
            URL resourceUrl = classLoader.getResource(fullPath);
            if (resourceUrl != null) {
                try (InputStream is = resourceUrl.openStream()) {
                    byte[] data = is.readAllBytes();
                    resourceCache.computeIfAbsent(pluginId, k -> new HashMap<>()).put(resourcePath, data);
                    return data;
                }
            }
        } catch (IOException e) {
            LOG.warn("Could not load resource {} for plugin {}: {}", resourcePath, pluginId, e.getMessage());
        }
        return null;
    }

    public String getResourceAsString(String pluginId, String resourcePath) {
        byte[] data = getResource(pluginId, resourcePath);
        if (data != null) {
            return new String(data, StandardCharsets.UTF_8);
        }
        return null;
    }

    public Map<String, byte[]> getAllResources(String pluginId) {
        if (!resourceCache.containsKey(pluginId)) {
            loadPluginResources(pluginId);
        }
        return Collections.unmodifiableMap(resourceCache.getOrDefault(pluginId, Collections.emptyMap()));
    }

    public boolean hasResources(String pluginId) {
        if (resourceCache.containsKey(pluginId)) {
            return !resourceCache.get(pluginId).isEmpty();
        }
        try {
            Enumeration<URL> urls = classLoader.getResources(PLUGIN_RESOURCES_PATH + pluginId);
            return urls.hasMoreElements();
        } catch (IOException e) {
            return false;
        }
    }

    public void clearCache() {
        resourceCache.clear();
        LOG.debug("Cleared plugin resource cache");
    }

    public void clearCache(String pluginId) {
        resourceCache.remove(pluginId);
        LOG.debug("Cleared resource cache for plugin {}", pluginId);
    }

    private void loadResource(URL url, Map<String, byte[]> resources) {
        try (InputStream is = url.openStream()) {
            String path = url.getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            resources.put(fileName, is.readAllBytes());
        } catch (IOException e) {
            LOG.warn("Could not load resource from {}: {}", url, e.getMessage());
        }
    }

    public static InputStream getResourceAsStream(String resourcePath) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    }

    public static InputStream getResourceAsStream(ClassLoader loader, String resourcePath) {
        if (loader != null) {
            return loader.getResourceAsStream(resourcePath);
        }
        return ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
    }

    public static boolean resourceExists(String resourcePath) {
        return Thread.currentThread().getContextClassLoader().getResource(resourcePath) != null;
    }

    public static URL getResource(String resourcePath) {
        return Thread.currentThread().getContextClassLoader().getResource(resourcePath);
    }

    public static String readResourceString(String resourcePath) {
        try (InputStream is = getResourceAsStream(resourcePath)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LOG.warn("Could not read resource {}: {}", resourcePath, e.getMessage());
        }
        return null;
    }
}
