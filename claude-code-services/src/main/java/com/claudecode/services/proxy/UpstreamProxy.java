package com.claudecode.services.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CCR container-side upstream proxy stub.
 * Handles CONNECT→WebSocket relay, anti-dump, CA certs, token unlinking.
 * Implements fail-open policy: each step's failure does not block the flow.
 */
public class UpstreamProxy {

    private static final Logger log = LoggerFactory.getLogger(UpstreamProxy.class);

    private final Path sessionTokenFile;
    private final int proxyPort;
    private final AntiDumpProtection antiDump;
    private final CaCertManager caCertManager;
    private boolean started;

    public UpstreamProxy(Path sessionTokenFile, int proxyPort) {
        this.sessionTokenFile = sessionTokenFile;
        this.proxyPort = proxyPort;
        this.antiDump = AntiDumpProtection.create();
        this.caCertManager = new CaCertManager();
        this.started = false;
    }

    /**
     * Starts the upstream proxy with fail-open policy.
     * Each step logs warnings on failure but does not throw.
     */
    public void start() {
        // 1. Read session token (fail-open)
        String token = readTokenFailOpen();

        // 2. Set process non-dumpable (fail-open)
        try {
            antiDump.setNonDumpable();
        } catch (Exception e) {
            log.warn("Anti-dump protection failed (fail-open): {}", e.getMessage());
        }

        // 3. Initialize CA cert trust store (fail-open)
        try {
            caCertManager.initTrustStore();
        } catch (Exception e) {
            log.warn("CA cert initialization failed (fail-open): {}", e.getMessage());
        }

        // 4. Start CONNECT→WebSocket relay (stub — no actual server)
        log.info("Upstream proxy relay stub on port {} (not actually listening)", proxyPort);

        // 5. Unlink token file (fail-open)
        unlinkTokenFile();

        // 6. Set proxy selector (fail-open)
        try {
            var selector = new UpstreamProxySelector(proxyPort);
            // In production: ProxySelector.setDefault(selector);
            log.debug("Proxy selector prepared for port {}", proxyPort);
        } catch (Exception e) {
            log.warn("Proxy selector setup failed (fail-open): {}", e.getMessage());
        }

        started = true;
        log.info("Upstream proxy started (stub mode)");
    }

    /**
     * Reads the session token file, returning empty string on failure (fail-open).
     */
    private String readTokenFailOpen() {
        if (sessionTokenFile == null || !Files.isRegularFile(sessionTokenFile)) {
            log.debug("No session token file found");
            return "";
        }
        try {
            return Files.readString(sessionTokenFile);
        } catch (IOException e) {
            log.warn("Failed to read session token (fail-open): {}", e.getMessage());
            return "";
        }
    }

    /**
     * Deletes the token file after reading (fail-open).
     */
    private void unlinkTokenFile() {
        if (sessionTokenFile == null) return;
        try {
            boolean deleted = Files.deleteIfExists(sessionTokenFile);
            if (deleted) {
                log.debug("Token file unlinked: {}", sessionTokenFile);
            }
        } catch (IOException e) {
            log.warn("Failed to unlink token file (fail-open): {}", e.getMessage());
        }
    }

    public boolean isStarted() {
        return started;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public AntiDumpProtection getAntiDump() {
        return antiDump;
    }

    public CaCertManager getCaCertManager() {
        return caCertManager;
    }
}
