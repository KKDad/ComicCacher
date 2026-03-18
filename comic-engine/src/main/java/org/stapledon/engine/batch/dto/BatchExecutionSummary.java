package org.stapledon.engine.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data structure for batch execution summary, persisted to JSON.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class BatchExecutionSummary {
    @ToString.Include
    Long executionId;
    @ToString.Include
    String jobName;
    @ToString.Include
    LocalDateTime executionTime;
    @ToString.Include
    String status;
    @ToString.Include
    String exitCode;
    @ToString.Include
    String exitMessage;
    @ToString.Include
    LocalDateTime startTime;
    @ToString.Include
    LocalDateTime endTime;
    @ToString.Include
    String errorMessage;
    Map<String, Object> parameters;
    List<BatchStepSummary> steps;
}
