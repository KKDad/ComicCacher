package org.stapledon.api.dto.batch;

import java.time.OffsetDateTime;

/**
 * DTO mapping Spring Batch StepExecution to the GraphQL BatchStep type.
 */
public record BatchStepDto(
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
