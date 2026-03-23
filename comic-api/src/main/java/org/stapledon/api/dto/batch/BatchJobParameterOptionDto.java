package org.stapledon.api.dto.batch;

/**
 * DTO for a selectable option in an ENUM-type batch job parameter.
 */
public record BatchJobParameterOptionDto(
    String value,
    String label
) {
}
