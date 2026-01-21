package org.stapledon.common.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * Data Transfer Object representing a stored image hash record for duplicate
 * detection.
 * Each record tracks a unique image hash along with metadata about when and
 * where it was saved.
 */
@Data
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class ImageHashRecord {
    @ToString.Include
    private final String hash;

    @ToString.Include
    private final LocalDate date;

    @ToString.Include
    private final String filePath;

    @ToString.Include
    private final HashAlgorithm algorithm;
}
