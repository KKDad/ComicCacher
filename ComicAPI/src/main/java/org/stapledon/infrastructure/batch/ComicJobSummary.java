package org.stapledon.infrastructure.batch;

import lombok.Builder;
import lombok.Data;

/**
 * Summary statistics for comic retrieval batch jobs
 */
@Data
@Builder
public class ComicJobSummary {
    private long totalExecutions;
    private long successfulExecutions;
    private long failedExecutions;
    private double successRate;
    private double averageDurationMinutes;
    private String dateRange;
}