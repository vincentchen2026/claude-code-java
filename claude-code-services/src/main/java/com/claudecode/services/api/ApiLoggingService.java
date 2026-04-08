package com.claudecode.services.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class ApiLoggingService {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingService.class);

    private final ConcurrentLinkedQueue<ApiLogEntry> entries = new ConcurrentLinkedQueue<>();
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private volatile LogLevel minLevel;

    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    public ApiLoggingService() {
        this(LogLevel.INFO);
    }

    public ApiLoggingService(LogLevel minLevel) {
        this.minLevel = minLevel != null ? minLevel : LogLevel.INFO;
    }

    public void logRequest(ApiLogEntry entry) {
        entries.add(entry);
        requestCount.incrementAndGet();

        if (entry.level() == LogLevel.ERROR) {
            errorCount.incrementAndGet();
        }

        if (shouldLog(entry.level())) {
            log.debug("API Request: {} {} - {}ms", entry.method(), entry.endpoint(), entry.durationMs());
        }
    }

    public void logResponse(String requestId, int statusCode, long durationMs, String responseBody) {
        var entry = new ApiLogEntry(
            requestId,
            "RESPONSE",
            "/",
            "GET",
            statusCode,
            durationMs,
            Instant.now(),
            LogLevel.INFO,
            null
        );
        entries.add(entry);
    }

    private boolean shouldLog(LogLevel level) {
        return level.ordinal() >= minLevel.ordinal();
    }

    public List<ApiLogEntry> getEntries(int limit) {
        List<ApiLogEntry> result = new ArrayList<>();
        int count = 0;
        for (ApiLogEntry entry : entries) {
            if (limit > 0 && count >= limit) break;
            result.add(entry);
            count++;
        }
        return result;
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public void clear() {
        entries.clear();
    }

    public record ApiLogEntry(
        String requestId,
        String method,
        String endpoint,
        String path,
        int statusCode,
        long durationMs,
        Instant timestamp,
        LogLevel level,
        String errorMessage
    ) {}
}