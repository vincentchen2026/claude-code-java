package com.claudecode.services.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TeamFileWatcher {

    private static final Logger log = LoggerFactory.getLogger(TeamFileWatcher.class);

    private final Path rootPath;
    private final WatchService watchService;
    private final ExecutorService executor;
    private final Map<WatchKey, Path> keys;
    private final CopyOnWriteArrayList<FileChangeListener> listeners;
    private volatile boolean running;

    public TeamFileWatcher(Path rootPath) throws IOException {
        this.rootPath = rootPath;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.executor = Executors.newSingleThreadExecutor();
        this.keys = new HashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.running = false;
    }

    public void start() {
        if (running) return;
        running = true;
        registerRecursive(rootPath);
        executor.submit(this::processEvents);
        log.info("Started file watcher on: {}", rootPath);
    }

    public void stop() {
        running = false;
        executor.shutdown();
        try {
            watchService.close();
        } catch (IOException e) {
            log.error("Error closing watch service", e);
        }
        log.info("Stopped file watcher");
    }

    public void addListener(FileChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FileChangeListener listener) {
        listeners.remove(listener);
    }

    private void registerRecursive(Path dir) {
        try {
            Files.walk(dir)
                .filter(Files::isDirectory)
                .forEach(this::registerDirectory);
        } catch (IOException e) {
            log.error("Error registering directories", e);
        }
    }

    private void registerDirectory(Path dir) {
        try {
            WatchKey key = dir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
            keys.put(key, dir);
            log.debug("Registered directory for watching: {}", dir);
        } catch (IOException e) {
            log.warn("Could not register directory: {}", dir, e);
        }
    }

    private void processEvents() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) continue;

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path fileName = pathEvent.context();
                Path fullPath = dir.resolve(fileName);

                FileChange change = new FileChange(
                    fullPath,
                    fileName.toString(),
                    kind,
                    System.currentTimeMillis()
                );

                for (FileChangeListener listener : listeners) {
                    try {
                        listener.onFileChange(change);
                    } catch (Exception e) {
                        log.warn("File change listener threw exception", e);
                    }
                }
            }

            key.reset();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public int getWatchedDirectoryCount() {
        return keys.size();
    }

    public record FileChange(
        Path fullPath,
        String fileName,
        WatchEvent.Kind<?> kind,
        long timestamp
    ) {}

    @FunctionalInterface
    public interface FileChangeListener {
        void onFileChange(FileChange change);
    }
}