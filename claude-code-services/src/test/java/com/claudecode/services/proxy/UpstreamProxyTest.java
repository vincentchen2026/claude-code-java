package com.claudecode.services.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UpstreamProxyTest {

    @TempDir
    Path tempDir;

    // --- UpstreamProxy ---

    @Test
    void startWithNoTokenFile() {
        var proxy = new UpstreamProxy(tempDir.resolve("nonexistent"), 8080);
        proxy.start();
        assertTrue(proxy.isStarted());
    }

    @Test
    void startUnlinksTokenFile() throws IOException {
        Path tokenFile = tempDir.resolve("token.txt");
        Files.writeString(tokenFile, "secret-token");
        assertTrue(Files.exists(tokenFile));

        var proxy = new UpstreamProxy(tokenFile, 8080);
        proxy.start();

        assertTrue(proxy.isStarted());
        assertFalse(Files.exists(tokenFile), "Token file should be deleted");
    }

    @Test
    void startWithNullTokenFile() {
        var proxy = new UpstreamProxy(null, 9090);
        proxy.start();
        assertTrue(proxy.isStarted());
        assertEquals(9090, proxy.getProxyPort());
    }

    @Test
    void caCertManagerInitialized() {
        var proxy = new UpstreamProxy(null, 8080);
        proxy.start();
        assertTrue(proxy.getCaCertManager().isInitialized());
    }

    // --- AntiDumpProtection ---

    @Test
    void antiDumpCreateReturnsNonNull() {
        var protection = AntiDumpProtection.create();
        assertNotNull(protection);
        assertNotNull(protection.platform());
    }

    @Test
    void antiDumpSetNonDumpableSucceeds() {
        var protection = AntiDumpProtection.create();
        assertTrue(protection.setNonDumpable());
    }

    @Test
    void noOpAntiDumpAlwaysSucceeds() {
        var noop = new AntiDumpProtection.NoOpAntiDump();
        assertTrue(noop.setNonDumpable());
        assertEquals("unsupported", noop.platform());
    }

    // --- CaCertManager ---

    @Test
    void caCertManagerInitTrustStore() {
        var mgr = new CaCertManager();
        assertFalse(mgr.isInitialized());
        mgr.initTrustStore();
        assertTrue(mgr.isInitialized());
        assertNotNull(mgr.getTrustStore());
    }

    @Test
    void caCertManagerCreateTrustManagerFactory() {
        var mgr = new CaCertManager();
        mgr.initTrustStore();
        var tmf = mgr.createTrustManagerFactory();
        assertNotNull(tmf);
    }

    @Test
    void caCertManagerNotInitializedReturnsNull() {
        var mgr = new CaCertManager();
        assertNull(mgr.createTrustManagerFactory());
    }

    @Test
    void caCertManagerApplyAsDefaultWhenNotInitialized() {
        var mgr = new CaCertManager();
        assertFalse(mgr.applyAsDefault());
    }

    // --- UpstreamProxySelector ---

    @Test
    void proxySelectorRoutesTrafficThroughProxy() {
        var selector = new UpstreamProxySelector(8080);
        List<Proxy> proxies = selector.select(URI.create("https://example.com"));
        assertEquals(1, proxies.size());
        assertEquals(Proxy.Type.HTTP, proxies.get(0).type());
    }

    @Test
    void proxySelectorBypassesLocalhost() {
        var selector = new UpstreamProxySelector(8080);
        List<Proxy> proxies = selector.select(URI.create("https://localhost:3000"));
        assertEquals(List.of(Proxy.NO_PROXY), proxies);
    }

    @Test
    void proxySelectorBypassesLoopback() {
        var selector = new UpstreamProxySelector(8080);
        List<Proxy> proxies = selector.select(URI.create("https://127.0.0.1:3000"));
        assertEquals(List.of(Proxy.NO_PROXY), proxies);
    }

    @Test
    void proxySelectorBypassesGithub() {
        var selector = new UpstreamProxySelector(8080);
        List<Proxy> proxies = selector.select(URI.create("https://github.com/repo"));
        assertEquals(List.of(Proxy.NO_PROXY), proxies);
    }

    @Test
    void proxySelectorBypassesAnthropicApi() {
        var selector = new UpstreamProxySelector(8080);
        List<Proxy> proxies = selector.select(URI.create("https://api.anthropic.com/v1"));
        assertEquals(List.of(Proxy.NO_PROXY), proxies);
    }

    @Test
    void proxySelectorBypassesNullUri() {
        var selector = new UpstreamProxySelector(8080);
        List<Proxy> proxies = selector.select(null);
        assertEquals(List.of(Proxy.NO_PROXY), proxies);
    }

    @Test
    void proxySelectorCustomNoProxy() {
        var selector = new UpstreamProxySelector(8080, Set.of("internal.corp"));
        List<Proxy> proxies = selector.select(URI.create("https://internal.corp/api"));
        assertEquals(List.of(Proxy.NO_PROXY), proxies);

        // external should go through proxy
        proxies = selector.select(URI.create("https://example.com"));
        assertEquals(Proxy.Type.HTTP, proxies.get(0).type());
    }

    @Test
    void proxySelectorBuildNoProxyString() {
        var selector = new UpstreamProxySelector(8080, Set.of("a.com", "b.com"));
        String noProxy = selector.buildNoProxyString();
        assertTrue(noProxy.contains("a.com"));
        assertTrue(noProxy.contains("b.com"));
    }

    @Test
    void proxySelectorConnectFailedDoesNotThrow() {
        var selector = new UpstreamProxySelector(8080);
        // Should not throw (fail-open)
        selector.connectFailed(URI.create("https://example.com"), null, new IOException("test"));
    }

    @Test
    void defaultNoProxyContainsExpectedEntries() {
        assertTrue(UpstreamProxySelector.DEFAULT_NO_PROXY.contains("localhost"));
        assertTrue(UpstreamProxySelector.DEFAULT_NO_PROXY.contains("127.0.0.1"));
        assertTrue(UpstreamProxySelector.DEFAULT_NO_PROXY.contains("169.254.169.254"));
        assertTrue(UpstreamProxySelector.DEFAULT_NO_PROXY.contains("github.com"));
        assertTrue(UpstreamProxySelector.DEFAULT_NO_PROXY.contains("api.anthropic.com"));
    }
}
