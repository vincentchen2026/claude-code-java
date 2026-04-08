package com.claudecode.services.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AgentSummaryService.class);

    private final Map<String, List<AgentActivity>> activities = new ConcurrentHashMap<>();

    public void recordActivity(String agentId, AgentActivity activity) {
        List<AgentActivity> agentActivities = activities.computeIfAbsent(agentId, k -> new ArrayList<>());
        agentActivities.add(activity);
        log.debug("Recorded activity for agent {}: {}", agentId, activity.type());
    }

    public AgentSummary generateSummary(String agentId) {
        List<AgentActivity> agentActivities = activities.getOrDefault(agentId, List.of());

        if (agentActivities.isEmpty()) {
            return new AgentSummary(agentId, List.of(), Instant.now(), "No activity recorded");
        }

        int totalTasks = 0;
        int completedTasks = 0;
        int failedTasks = 0;
        long totalDuration = 0;

        for (AgentActivity activity : agentActivities) {
            totalTasks++;
            switch (activity.type()) {
                case TASK_COMPLETED -> completedTasks++;
                case TASK_FAILED -> failedTasks++;
            }
            if (activity.endTime() != null && activity.startTime() != null) {
                totalDuration += ChronoUnit.MILLIS.between(activity.startTime(), activity.endTime());
            }
        }

        String summaryText = String.format(
            "Agent %s completed %d/%d tasks (failed: %d), total runtime: %dms",
            agentId, completedTasks, totalTasks, failedTasks, totalDuration
        );

        return new AgentSummary(
            agentId,
            agentActivities,
            Instant.now(),
            summaryText
        );
    }

    public List<AgentActivity> getRecentActivities(String agentId, int limit) {
        List<AgentActivity> agentActivities = activities.getOrDefault(agentId, List.of());
        return agentActivities.stream()
            .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
            .limit(limit)
            .toList();
    }

    public void clearActivities(String agentId) {
        activities.remove(agentId);
        log.info("Cleared activities for agent: {}", agentId);
    }

    public record AgentSummary(
        String agentId,
        List<AgentActivity> activities,
        Instant generatedAt,
        String summary
    ) {}

    public record AgentActivity(
        String activityId,
        ActivityType type,
        String description,
        Instant timestamp,
        Instant startTime,
        Instant endTime,
        Map<String, Object> metadata
    ) {}

    public enum ActivityType {
        TASK_STARTED,
        TASK_COMPLETED,
        TASK_FAILED,
        TOOL_USED,
        MESSAGE_SENT,
        MESSAGE_RECEIVED,
        ERROR
    }
}