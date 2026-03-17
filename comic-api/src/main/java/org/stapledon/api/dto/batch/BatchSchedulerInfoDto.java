package org.stapledon.api.dto.batch;

import java.time.OffsetDateTime;

/**
 * DTO for batch scheduler info exposed via GraphQL.
 */
public record BatchSchedulerInfoDto(
    String jobName,
    String cronExpression,
    String timezone,
    OffsetDateTime nextRunTime,
    boolean enabled,
    boolean paused,
    OffsetDateTime lastToggled,
    String toggledBy
) {
}
