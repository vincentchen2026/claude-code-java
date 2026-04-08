package com.claudecode.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Server-Sent Events (SSE) parser.
 * Parses an SSE stream into typed event/data pairs.
 */
public class SseParser implements Iterator<SseParser.SseEvent>, AutoCloseable {

    private final BlockingQueue<SseEvent> eventQueue = new LinkedBlockingQueue<>();
    private final Thread readerThread;
    private volatile boolean done = false;
    private SseEvent nextEvent = null;

    /** Sentinel value to signal end of stream. */
    private static final SseEvent SENTINEL = new SseEvent("__sentinel__", "");

    public record SseEvent(String event, String data) {}

    public SseParser(InputStream inputStream) {
        this.readerThread = Thread.ofVirtual().name("sse-parser").start(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                parseStream(reader);
            } catch (IOException e) {
                if (!done) {
                    eventQueue.offer(new SseEvent("error", e.getMessage()));
                }
            } finally {
                done = true;
                eventQueue.offer(SENTINEL);
            }
        });
    }

    private void parseStream(BufferedReader reader) throws IOException {
        String currentEvent = null;
        StringBuilder currentData = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            if (done) break;

            if (line.isEmpty()) {
                // Empty line = end of event
                if (currentData.length() > 0) {
                    String eventType = currentEvent != null ? currentEvent : "message";
                    eventQueue.offer(new SseEvent(eventType, currentData.toString().trim()));
                    currentEvent = null;
                    currentData.setLength(0);
                }
                continue;
            }

            if (line.startsWith("event:")) {
                currentEvent = line.substring(6).trim();
            } else if (line.startsWith("data:")) {
                if (currentData.length() > 0) {
                    currentData.append('\n');
                }
                currentData.append(line.substring(5).trim());
            }
            // Ignore comments (lines starting with ':') and other fields
        }

        // Flush any remaining event
        if (currentData.length() > 0) {
            String eventType = currentEvent != null ? currentEvent : "message";
            eventQueue.offer(new SseEvent(eventType, currentData.toString().trim()));
        }
    }

    @Override
    public boolean hasNext() {
        if (nextEvent != null) return true;
        try {
            nextEvent = eventQueue.take();
            return nextEvent != SENTINEL;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public SseEvent next() {
        if (!hasNext()) throw new NoSuchElementException();
        SseEvent event = nextEvent;
        nextEvent = null;
        return event;
    }

    @Override
    public void close() {
        done = true;
        readerThread.interrupt();
    }
}
