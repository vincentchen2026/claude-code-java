package com.claudecode.services.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Custom ProxySelector that routes traffic through the upstream proxy,
 * except for addresses in the NO_PROXY list.
 */
public class UpstreamProxySelector extends ProxySelector {

    /** Default NO_PROXY entries covering loopback, RFC1918, IMDS, common services */
    public static final Set<String> DEFAULT_NO_PROXY = Set.of(
        "127.0.0.1",
        "localhost",
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "169.254.169.254",       // IMDS
        "registry.npmjs.org",    // package registries
        "github.com",
        "api.anthropic.com"
    );

    private final int proxyPort;
    private final Set<String> noProxyHosts;

    public UpstreamProxySelector(int proxyPort, Set<String> noProxyHosts) {
        this.proxyPort = proxyPort;
        this.noProxyHosts = noProxyHosts != null ? noProxyHosts : DEFAULT_NO_PROXY;
    }

    public UpstreamProxySelector(int proxyPort) {
        this(proxyPort, DEFAULT_NO_PROXY);
    }

    @Override
    public List<Proxy> select(URI uri) {
        if (uri == null || shouldBypass(uri.getHost())) {
            return List.of(Proxy.NO_PROXY);
        }
        return List.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", proxyPort)));
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        // Fail-open: log but don't block
    }

    /**
     * Checks if the given host should bypass the proxy.
     */
    boolean shouldBypass(String host) {
        if (host == null || host.isEmpty()) return true;
        for (String entry : noProxyHosts) {
            if (entry.contains("/")) {
                // CIDR notation — simplified: just check prefix
                String prefix = entry.substring(0, entry.indexOf('.'));
                if (host.startsWith(prefix + ".")) return true;
            } else if (host.equalsIgnoreCase(entry)) {
                return true;
            }
        }
        return false;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public Set<String> getNoProxyHosts() {
        return noProxyHosts;
    }

    /**
     * Builds the NO_PROXY string for environment variable export.
     */
    public String buildNoProxyString() {
        return String.join(",", noProxyHosts);
    }
}
