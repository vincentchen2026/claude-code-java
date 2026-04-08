package com.claudecode.services.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ChannelNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ChannelNotificationService.class);

    private final Map<String, List<ChannelSubscriber>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, ChannelState> channelStates = new ConcurrentHashMap<>();

    public void subscribe(String channelId, ChannelSubscriber subscriber) {
        List<ChannelSubscriber> channelSubs = subscribers.computeIfAbsent(channelId, k -> new CopyOnWriteArrayList<>());
        if (!channelSubs.contains(subscriber)) {
            channelSubs.add(subscriber);
            log.debug("Subscribed to channel: {}", channelId);
        }
    }

    public void unsubscribe(String channelId, ChannelSubscriber subscriber) {
        List<ChannelSubscriber> channelSubs = subscribers.get(channelId);
        if (channelSubs != null) {
            channelSubs.remove(subscriber);
            log.debug("Unsubscribed from channel: {}", channelId);
        }
    }

    public void publish(String channelId, ChannelNotification notification) {
        List<ChannelSubscriber> channelSubs = subscribers.get(channelId);
        if (channelSubs == null || channelSubs.isEmpty()) {
            log.debug("No subscribers for channel: {}", channelId);
            return;
        }

        updateChannelState(channelId, notification);

        for (ChannelSubscriber subscriber : channelSubs) {
            try {
                subscriber.onNotification(notification);
            } catch (Exception e) {
                log.error("Error delivering notification to subscriber on channel {}: {}", channelId, e.getMessage());
            }
        }

        log.debug("Published notification to channel {}: {}", channelId, notification.type());
    }

    public void publishToAll(ChannelNotification notification) {
        for (String channelId : subscribers.keySet()) {
            publish(channelId, notification);
        }
    }

    private void updateChannelState(String channelId, ChannelNotification notification) {
        ChannelState state = channelStates.get(channelId);
        int count = state != null ? state.notificationCount() + 1 : 1;
        ChannelState updated = new ChannelState(
            channelId,
            notification,
            Instant.now(),
            count
        );
        channelStates.put(channelId, updated);
    }

    public ChannelState getChannelState(String channelId) {
        return channelStates.get(channelId);
    }

    public record ChannelNotification(
        String notificationId,
        String channelId,
        NotificationType type,
        Object payload,
        Instant timestamp
    ) {}

    public record ChannelState(
        String channelId,
        ChannelNotification lastNotification,
        Instant lastUpdated,
        int notificationCount
    ) {}

    public interface ChannelSubscriber {
        void onNotification(ChannelNotification notification);
    }

    public enum NotificationType {
        MESSAGE,
        EVENT,
        ERROR,
        STATE_CHANGE,
        HEARTBEAT
    }
}