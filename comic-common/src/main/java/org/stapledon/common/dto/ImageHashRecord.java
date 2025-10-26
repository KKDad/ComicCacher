package org.stapledon.common.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * Data Transfer Object representing a stored image hash record for duplicate detection.
 * Each record tracks a unique image hash along with metadata about when and where it was saved.
 */
@Data
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class ImageHashRecord {
    /**
     * The calculated hash value of the image.
     */
    @ToString.Include
    private final String hash;

    /**
     * The date this comic strip was published/saved.
     */
    @ToString.Include
    private final LocalDate date;

    /**
     * The file path where this image is stored.
     */
    @ToString.Include
    private final String filePath;

    /**
     * The hash algorithm used to generate this hash.
     */
    @ToString.Include
    private final HashAlgorithm algorithm;
}
