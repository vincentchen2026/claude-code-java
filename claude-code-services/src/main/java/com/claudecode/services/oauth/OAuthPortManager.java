package com.claudecode.services.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OAuthPortManager {

    private static final Logger log = LoggerFactory.getLogger(OAuthPortManager.class);

    private static final int MIN_PORT = 8080;
    private static final int MAX_PORT = 65535;

    private final AtomicInteger nextPort;
    private final Map<Integer, PortAllocation> allocations = new ConcurrentHashMap<>();
    private final int basePort;

    public OAuthPortManager() {
        this(MIN_PORT);
    }

    public OAuthPortManager(int basePort) {
        this.basePort = basePort;
        this.nextPort = new AtomicInteger(basePort);
    }

    public int allocatePort(String callbackId) {
        int port = findAvailablePort();
        if (port == -1) {
            log.error("No available ports for OAuth callback");
            return -1;
        }

        PortAllocation allocation = new PortAllocation(
            port,
            callbackId,
            System.currentTimeMillis(),
            PortStatus.ALLOCATED
        );

        allocations.put(port, allocation);
        log.info("Allocated port {} for callback {}", port, callbackId);
        return port;
    }

    public boolean releasePort(int port) {
        PortAllocation allocation = allocations.remove(port);
        if (allocation != null) {
            log.info("Released port {}", port);
            return true;
        }
        return false;
    }

    public PortAllocation getAllocation(int port) {
        return allocations.get(port);
    }

    public PortAllocation getAllocationByCallbackId(String callbackId) {
        for (PortAllocation allocation : allocations.values()) {
            if (allocation.callbackId().equals(callbackId)) {
                return allocation;
            }
        }
        return null;
    }

    public void markPortInUse(int port) {
        PortAllocation existing = allocations.get(port);
        if (existing != null) {
            allocations.put(port, new PortAllocation(
                existing.port(),
                existing.callbackId(),
                existing.allocatedAt(),
                PortStatus.IN_USE
            ));
        }
    }

    public void markPortReady(int port) {
        PortAllocation existing = allocations.get(port);
        if (existing != null) {
            allocations.put(port, new PortAllocation(
                existing.port(),
                existing.callbackId(),
                existing.allocatedAt(),
                PortStatus.READY
            ));
        }
    }

    private int findAvailablePort() {
        int attempts = 0;
        int maxAttempts = MAX_PORT - MIN_PORT;

        while (attempts < maxAttempts) {
            int port = nextPort.getAndIncrement();
            if (port > MAX_PORT) {
                nextPort.set(basePort);
                port = basePort;
            }

            if (isPortAvailable(port)) {
                return port;
            }

            attempts++;
        }

        return -1;
    }

    private boolean isPortAvailable(int port) {
        if (allocations.containsKey(port)) {
            return false;
        }

        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getAllocatedPortCount() {
        return allocations.size();
    }

    public record PortAllocation(
        int port,
        String callbackId,
        long allocatedAt,
        PortStatus status
    ) {}

    public enum PortStatus {
        ALLOCATED,
        IN_USE,
        READY,
        RELEASED
    }
}