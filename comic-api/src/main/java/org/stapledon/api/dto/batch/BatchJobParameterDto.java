package org.stapledon.api.dto.batch;

import java.util.List;

/**
 * DTO describing an optional parameter accepted by a batch job.
 */
public record BatchJobParameterDto(
    String name,
    String label,
    BatchJobParameterType type,
    boolean required,
    String defaultValue,
    List<BatchJobParameterOptionDto> options
) {
}
