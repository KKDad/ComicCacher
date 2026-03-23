package org.stapledon.api.dto.batch;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for batch scheduler info exposed via GraphQL.
 */
public record BatchSchedulerInfoDto(
    String jobName,
    String cronExpression,
    String description,
    String timezone,
    OffsetDateTime nextRunTime,
    boolean enabled,
    boolean paused,
    OffsetDateTime lastToggled,
    String toggledBy,
    List<BatchJobParameterDto> availableParameters
) {
}
