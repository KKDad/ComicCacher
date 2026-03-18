package org.stapledon.engine.batch.dto;

import java.time.LocalDateTime;

/**
 * Summary of a single batch step execution, persisted to JSON.
 */
public record BatchStepSummary(
        String stepName,
        String status,
        int readCount,
        int writeCount,
        int filterCount,
        int skipCount,
        int commitCount,
        int rollbackCount,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
