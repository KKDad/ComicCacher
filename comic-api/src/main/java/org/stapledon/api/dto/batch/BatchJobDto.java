package org.stapledon.api.dto.batch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO mapping Spring Batch JobExecution to the GraphQL BatchJob type.
 */
public record BatchJobDto(
        long executionId,
        String jobName,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Double durationMs,
        String exitCode,
        String exitDescription,
        Map<String, Object> parameters,
        List<BatchStepDto> steps
) {
}
