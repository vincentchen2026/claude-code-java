package com.claudecode.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages channel-level permissions for MCP servers.
 * Controls which MCP servers are allowed to be used via an allowlist.
 */
public class McpChannelPermissions {

    private static final Logger LOG = LoggerFactory.getLogger(McpChannelPermissions.class);

    private final Set<String> allowedServers = ConcurrentHashMap.newKeySet();
    private volatile boolean allowAll = true;

    /**
     * Sets the allowlist of server IDs. When set, only these servers can be used.
     * Pass null or empty to allow all servers.
     */
    public void setAllowedServers(Collection<String> serverIds) {
        allowedServers.clear();
        if (serverIds != null && !serverIds.isEmpty()) {
            allowedServers.addAll(serverIds);
            allowAll = false;
        } else {
            allowAll = true;
        }
        LOG.debug("MCP channel permissions updated: allowAll={}, allowed={}", allowAll, allowedServers);
    }

    /**
     * Checks whether the given server is allowed.
     */
    public boolean isServerAllowed(String serverId) {
        if (allowAll) return true;
        return allowedServers.contains(serverId);
    }

    /**
     * Adds a server to the allowlist.
     */
    public void allowServer(String serverId) {
        allowedServers.add(serverId);
        allowAll = false;
    }

    /**
     * Removes a server from the allowlist.
     */
    public void denyServer(String serverId) {
        allowedServers.remove(serverId);
    }

    /**
     * Returns the current set of allowed server IDs.
     * Returns empty if all servers are allowed.
     */
    public Set<String> getAllowedServers() {
        if (allowAll) return Set.of();
        return Collections.unmodifiableSet(new HashSet<>(allowedServers));
    }

    /**
     * Returns true if all servers are allowed (no restrictions).
     */
    public boolean isAllowAll() {
        return allowAll;
    }
}
