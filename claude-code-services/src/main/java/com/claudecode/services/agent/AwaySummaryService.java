package com.claudecode.services.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AwaySummaryService {

    private static final Logger log = LoggerFactory.getLogger(AwaySummaryService.class);

    private final Map<String, List<ActivityDuringAway>> awayActivities = new ConcurrentHashMap<>();

    public void recordActivity(String userId, ActivityDuringAway activity) {
        List<ActivityDuringAway> activities = awayActivities.computeIfAbsent(userId, k -> new ArrayList<>());
        activities.add(activity);
        log.debug("Recorded away activity for user {}: {}", userId, activity.activityType());
    }

    public AwaySummary generateSummary(String userId, Instant awayStart, Instant awayEnd) {
        List<ActivityDuringAway> activities = awayActivities.getOrDefault(userId, List.of());

        List<ActivityDuringAway> relevantActivities = new ArrayList<>();
        for (ActivityDuringAway activity : activities) {
            if (!activity.timestamp().isBefore(awayStart) && !activity.timestamp().isAfter(awayEnd)) {
                relevantActivities.add(activity);
            }
        }

        if (relevantActivities.isEmpty()) {
            return new AwaySummary(
                userId,
                awayStart,
                awayEnd,
                List.of(),
                "No activity during away period"
            );
        }

        long awayDurationMinutes = ChronoUnit.MINUTES.between(awayStart, awayEnd);
        int taskCount = (int) relevantActivities.stream()
            .filter(a -> a.activityType() == ActivityType.TASK_COMPLETED)
            .count();
        int messageCount = (int) relevantActivities.stream()
            .filter(a -> a.activityType() == ActivityType.MESSAGE_RECEIVED)
            .count();

        String summary = String.format(
            "While away (%d minutes): %d tasks completed, %d messages received",
            awayDurationMinutes, taskCount, messageCount
        );

        return new AwaySummary(
            userId,
            awayStart,
            awayEnd,
            relevantActivities,
            summary
        );
    }

    public List<ActivityDuringAway> getActivities(String userId) {
        return awayActivities.getOrDefault(userId, List.of());
    }

    public void clearActivities(String userId) {
        awayActivities.remove(userId);
        log.info("Cleared away activities for user: {}", userId);
    }

    public record AwaySummary(
        String userId,
        Instant awayStart,
        Instant awayEnd,
        List<ActivityDuringAway> activities,
        String summary
    ) {}

    public record ActivityDuringAway(
        String activityId,
        ActivityType activityType,
        String description,
        Instant timestamp,
        Map<String, Object> metadata
    ) {}

    public enum ActivityType {
        TASK_COMPLETED,
        TASK_STARTED,
        MESSAGE_RECEIVED,
        NOTIFICATION,
        ERROR,
        SYSTEM_EVENT
    }
}