package org.stapledon.engine.batch.dto;

import java.time.OffsetDateTime;

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
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}
