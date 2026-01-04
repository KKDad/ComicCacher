package org.stapledon.engine.batch.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Data structure for batch execution summary
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class BatchExecutionSummary {
    @ToString.Include
    Long lastExecutionId;
    @ToString.Include
    LocalDateTime lastExecutionTime;
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
}
