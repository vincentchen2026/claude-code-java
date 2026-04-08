package com.claudecode.services.bootstrap;

import com.claudecode.services.config.AppConfig;
import com.claudecode.services.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BootstrapService {

    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);

    public record BootstrapContext(
        AppConfig config,
        List<ServiceHandle<?>> services,
        long startupTimeMs
    ) {}

    public record ServiceHandle<T>(
        Class<T> serviceClass,
        T instance,
        String name,
        long initTimeMs
    ) {
        public String toString() {
            return "%s [%,dms]".formatted(name, initTimeMs);
        }
    }

    public record BootstrapOptions(
        Path projectDir,
        boolean skipPlugins,
        boolean skipMcp,
        boolean skipTelemetry
    ) {
        public static final BootstrapOptions DEFAULT = new BootstrapOptions(
            Path.of(System.getProperty("user.dir")),
            false,
            false,
            false
        );
    }

    public BootstrapResult bootstrap(BootstrapOptions options) {
        long startTime = System.currentTimeMillis();
        List<ServiceHandle<?>> services = new ArrayList<>();
        AppConfig config;

        try {
            config = loadConfiguration(options.projectDir());
            log.info("Configuration loaded");
        } catch (Exception e) {
            log.error("Failed to load configuration", e);
            return new BootstrapResult(null, null, List.of(), List.of("Configuration load failed: " + e.getMessage()), System.currentTimeMillis() - startTime);
        }

        services = initializeServices(config, options);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Bootstrap completed in {}ms with {} services", totalTime, services.size());

        return new BootstrapResult(config, services, List.copyOf(services), List.of(), totalTime);
    }

    private AppConfig loadConfiguration(Path projectDir) {
        ConfigService configService = new ConfigService(projectDir);
        return configService.loadConfig(null);
    }

    private List<ServiceHandle<?>> initializeServices(AppConfig config, BootstrapOptions options) {
        List<ServiceHandle<?>> handles = new ArrayList<>();

        handles.add(initService("ConfigService", () -> new ConfigService(Path.of(System.getProperty("user.dir")))));

        handles.add(initService("AnalyticsService", () -> createAnalyticsService(config)));

        if (!options.skipPlugins()) {
            handles.addAll(initPlugins());
        }

        if (!options.skipMcp()) {
            handles.addAll(initMcpServers());
        }

        return handles;
    }

    private <T> ServiceHandle<T> initService(String name, Supplier<T> factory) {
        long start = System.currentTimeMillis();
        try {
            T instance = factory.get();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("Initialized {} in {}ms", name, elapsed);
            return new ServiceHandle<>((Class<T>) factory.getClass(), instance, name, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("Failed to initialize {} after {}ms: {}", name, elapsed, e.getMessage());
            return new ServiceHandle<>((Class<T>) factory.getClass(), null, name, elapsed);
        }
    }

    private List<ServiceHandle<?>> initPlugins() {
        return List.of();
    }

    private List<ServiceHandle<?>> initMcpServers() {
        return List.of();
    }

    private Object createAnalyticsService(AppConfig config) {
        try {
            var sink = new com.claudecode.services.telemetry.AnalyticsSink();
            var privacyLevel = com.claudecode.services.telemetry.AnalyticsService.PrivacyLevel.STANDARD;
            return new com.claudecode.services.telemetry.AnalyticsService(sink, privacyLevel);
        } catch (Exception e) {
            log.debug("AnalyticsService not available: {}", e.getMessage());
            return null;
        }
    }

    public CompletableFuture<BootstrapResult> bootstrapAsync(BootstrapOptions options) {
        return CompletableFuture.supplyAsync(() -> bootstrap(options));
    }

    public record BootstrapResult(
        AppConfig config,
        Object queryEngine,
        List<ServiceHandle<?>> services,
        List<String> errors,
        long totalTimeMs
    ) {
        public boolean isSuccess() {
            return errors.isEmpty() && config != null;
        }
    }
}