package org.stapledon.api.dto.batch;

import java.util.List;

/**
 * DTO mapping aggregated batch job statistics to the GraphQL BatchJobSummary type.
 */
public record BatchJobSummaryDto(
        int daysIncluded,
        int totalExecutions,
        int successCount,
        int failureCount,
        int runningCount,
        Double averageDurationMs,
        Integer totalItemsProcessed,
        List<DailyJobStatsDto> dailyBreakdown
) {
}
