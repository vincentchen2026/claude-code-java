package com.claudecode.services.speculation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SpeculationService {

    private static final Logger log = LoggerFactory.getLogger(SpeculationService.class);

    private final Map<String, SpeculativeExecution> executions = new ConcurrentHashMap<>();
    private final AtomicInteger speculationCounter = new AtomicInteger(0);
    private volatile boolean speculationEnabled = true;

    public String startSpeculation(String input, SpeculationType type) {
        if (!speculationEnabled) {
            return null;
        }

        String speculationId = "spec_" + speculationCounter.incrementAndGet();

        SpeculativeExecution execution = new SpeculativeExecution(
            speculationId,
            input,
            type,
            Instant.now(),
            SpeculationStatus.RUNNING,
            null,
            new ArrayList<>()
        );

        executions.put(speculationId, execution);
        log.debug("Started speculation {} for input: {}", speculationId, input.substring(0, Math.min(50, input.length())));
        return speculationId;
    }

    public void addIntermediateResult(String speculationId, String result) {
        SpeculativeExecution execution = executions.get(speculationId);
        if (execution != null && execution.status() == SpeculationStatus.RUNNING) {
            List<String> results = new ArrayList<>(execution.results());
            results.add(result);

            SpeculativeExecution updated = new SpeculativeExecution(
                execution.speculationId(),
                execution.input(),
                execution.type(),
                execution.startedAt(),
                execution.status(),
                execution.finalResult(),
                results
            );
            executions.put(speculationId, updated);
        }
    }

    public void completeSpeculation(String speculationId, String finalResult) {
        SpeculativeExecution execution = executions.get(speculationId);
        if (execution != null) {
            SpeculativeExecution updated = new SpeculativeExecution(
                execution.speculationId(),
                execution.input(),
                execution.type(),
                execution.startedAt(),
                SpeculationStatus.COMPLETED,
                finalResult,
                execution.results()
            );
            executions.put(speculationId, updated);
            log.debug("Completed speculation {}", speculationId);
        }
    }

    public void cancelSpeculation(String speculationId) {
        SpeculativeExecution execution = executions.get(speculationId);
        if (execution != null) {
            SpeculativeExecution updated = new SpeculativeExecution(
                execution.speculationId(),
                execution.input(),
                execution.type(),
                execution.startedAt(),
                SpeculationStatus.CANCELLED,
                null,
                execution.results()
            );
            executions.put(speculationId, updated);
            log.debug("Cancelled speculation {}", speculationId);
        }
    }

    public SpeculativeExecution getExecution(String speculationId) {
        return executions.get(speculationId);
    }

    public List<SpeculativeExecution> getRunningExecutions() {
        return executions.values().stream()
            .filter(e -> e.status() == SpeculationStatus.RUNNING)
            .toList();
    }

    public void enableSpeculation() {
        this.speculationEnabled = true;
    }

    public void disableSpeculation() {
        this.speculationEnabled = false;
    }

    public boolean isSpeculationEnabled() {
        return speculationEnabled;
    }

    public record SpeculativeExecution(
        String speculationId,
        String input,
        SpeculationType type,
        Instant startedAt,
        SpeculationStatus status,
        String finalResult,
        List<String> results
    ) {}

    public enum SpeculationType {
        TEXT_COMPLETION,
        CODE_COMPLETION,
        TOOL_PREDICTION,
        RESPONSE_PREDICTION
    }

    public enum SpeculationStatus {
        RUNNING,
        COMPLETED,
        CANCELLED,
        FAILED
    }
}