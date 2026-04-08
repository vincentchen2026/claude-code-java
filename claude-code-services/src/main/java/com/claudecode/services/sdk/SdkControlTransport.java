package com.claudecode.services.sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class SdkControlTransport {

    private static final Logger log = LoggerFactory.getLogger(SdkControlTransport.class);

    private final Map<String, ControlChannel> channels = new ConcurrentHashMap<>();
    private final SubmissionPublisher<ControlMessage> publisher = new SubmissionPublisher<>();
    private volatile boolean connected = false;

    public void registerChannel(String channelId, ControlChannel channel) {
        channels.put(channelId, channel);
        log.info("Registered control channel: {}", channelId);
    }

    public void unregisterChannel(String channelId) {
        channels.remove(channelId);
        log.info("Unregistered control channel: {}", channelId);
    }

    public void sendMessage(String channelId, ControlMessage message) {
        ControlChannel channel = channels.get(channelId);
        if (channel != null) {
            channel.onMessage(message);
            log.debug("Sent control message to channel {}: {}", channelId, message.type());
        }
    }

    public void broadcast(ControlMessage message) {
        for (ControlChannel channel : channels.values()) {
            channel.onMessage(message);
        }
        log.debug("Broadcast control message to {} channels: {}", channels.size(), message.type());
    }

    public void subscribe(Flow.Subscriber<ControlMessage> subscriber) {
        publisher.subscribe(subscriber);
    }

    public void publish(ControlMessage message) {
        publisher.submit(message);
    }

    public void connect() {
        this.connected = true;
        log.info("SDK control transport connected");
    }

    public void disconnect() {
        this.connected = false;
        channels.clear();
        log.info("SDK control transport disconnected");
    }

    public boolean isConnected() {
        return connected;
    }

    public int getChannelCount() {
        return channels.size();
    }

    public ControlChannel getChannel(String channelId) {
        return channels.get(channelId);
    }

    public interface ControlChannel {
        void onMessage(ControlMessage message);
        void onConnect();
        void onDisconnect();
    }

    public record ControlMessage(
        String messageId,
        MessageType type,
        Map<String, Object> payload,
        Instant timestamp
    ) {}

    public enum MessageType {
        PING,
        PONG,
        CHANNEL_OPEN,
        CHANNEL_CLOSE,
        COMMAND,
        EVENT,
        ERROR
    }

    public static abstract class AbstractControlChannel implements ControlChannel {
        protected volatile boolean open = false;

        @Override
        public void onConnect() {
            this.open = true;
        }

        @Override
        public void onDisconnect() {
            this.open = false;
        }

        public boolean isOpen() {
            return open;
        }
    }
}