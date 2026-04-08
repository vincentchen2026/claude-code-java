package com.claudecode.services.telemetry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory ring buffer for diagnostic log entries.
 * Keeps the most recent N entries, discarding oldest when full.
 */
public class DiagnosticLog {

    private final LogEntry[] buffer;
    private int head;
    private int size;

    public DiagnosticLog(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.buffer = new LogEntry[capacity];
        this.head = 0;
        this.size = 0;
    }

    /**
     * Adds a log entry to the ring buffer.
     */
    public synchronized void add(String level, String message) {
        buffer[head] = new LogEntry(level, message, Instant.now());
        head = (head + 1) % buffer.length;
        if (size < buffer.length) size++;
    }

    /**
     * Returns all entries in chronological order.
     */
    public synchronized List<LogEntry> getEntries() {
        List<LogEntry> entries = new ArrayList<>(size);
        int start = size < buffer.length ? 0 : head;
        for (int i = 0; i < size; i++) {
            entries.add(buffer[(start + i) % buffer.length]);
        }
        return entries;
    }

    /**
     * Returns the number of entries currently stored.
     */
    public synchronized int size() {
        return size;
    }

    /**
     * Returns the capacity of the ring buffer.
     */
    public int capacity() {
        return buffer.length;
    }

    /**
     * Clears all entries.
     */
    public synchronized void clear() {
        for (int i = 0; i < buffer.length; i++) buffer[i] = null;
        head = 0;
        size = 0;
    }

    /**
     * A single diagnostic log entry.
     */
    public record LogEntry(String level, String message, Instant timestamp) {}
}
