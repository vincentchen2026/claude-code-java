package com.claudecode.services.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelAllowlistManager {

    private static final Logger log = LoggerFactory.getLogger(ChannelAllowlistManager.class);

    private final Set<String> allowedChannels = ConcurrentHashMap.newKeySet();
    private final Set<String> blockedChannels = ConcurrentHashMap.newKeySet();
    private volatile boolean allowAllByDefault = true;

    public ChannelAllowlistManager() {
        this(true);
    }

    public ChannelAllowlistManager(boolean allowAllByDefault) {
        this.allowAllByDefault = allowAllByDefault;
    }

    public void addAllowed(String channelId) {
        allowedChannels.add(channelId);
        blockedChannels.remove(channelId);
        log.info("Added channel to allowlist: {}", channelId);
    }

    public void addBlocked(String channelId) {
        blockedChannels.add(channelId);
        allowedChannels.remove(channelId);
        log.info("Added channel to blocklist: {}", channelId);
    }

    public void remove(String channelId) {
        allowedChannels.remove(channelId);
        blockedChannels.remove(channelId);
        log.debug("Removed channel from lists: {}", channelId);
    }

    public boolean isAllowed(String channelId) {
        if (blockedChannels.contains(channelId)) {
            return false;
        }

        if (allowedChannels.contains(channelId)) {
            return true;
        }

        return allowAllByDefault;
    }

    public void setAllowAllByDefault(boolean allowAll) {
        this.allowAllByDefault = allowAll;
        log.info("Set allowAllByDefault to: {}", allowAll);
    }

    public void clearAllowlist() {
        allowedChannels.clear();
        log.info("Cleared allowlist");
    }

    public void clearBlocklist() {
        blockedChannels.clear();
        log.info("Cleared blocklist");
    }

    public void clearAll() {
        clearAllowlist();
        clearBlocklist();
    }

    public Set<String> getAllowedChannels() {
        return Set.copyOf(allowedChannels);
    }

    public Set<String> getBlockedChannels() {
        return Set.copyOf(blockedChannels);
    }

    public boolean isAllowAllByDefault() {
        return allowAllByDefault;
    }
}