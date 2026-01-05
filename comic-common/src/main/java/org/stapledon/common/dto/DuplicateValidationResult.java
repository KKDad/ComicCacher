package org.stapledon.common.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * Data Transfer Object representing the result of a duplicate image validation check.
 * Indicates whether an image is a duplicate of an existing cached image.
 */
@Data
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class DuplicateValidationResult {
    /**
     * Flag indicating whether this image is a duplicate of an existing image.
     */
    @ToString.Include
    private final boolean duplicate;

    /**
     * The date of the existing duplicate image (null if not a duplicate).
     */
    @ToString.Include
    private final LocalDate duplicateDate;

    /**
     * The file path of the existing duplicate image (null if not a duplicate).
     */
    private final String duplicateFilePath;

    /**
     * The hash value that matched (null if not a duplicate).
     */
    @ToString.Include
    private final String hash;

    /**
     * Factory method to create a result indicating the image is unique (not a duplicate).
     *
     * @param hash The hash value of the unique image
     * @return A DuplicateValidationResult indicating uniqueness
     */
    public static DuplicateValidationResult unique(String hash) {
        return DuplicateValidationResult.builder()
                .duplicate(false)
                .hash(hash)
                .build();
    }

    /**
     * Factory method to create a result indicating the image is a duplicate.
     *
     * @param hash The matching hash value
     * @param duplicateDate The date of the existing duplicate
     * @param duplicateFilePath The file path of the existing duplicate
     * @return A DuplicateValidationResult indicating a duplicate was found
     */
    public static DuplicateValidationResult duplicate(String hash, LocalDate duplicateDate, String duplicateFilePath) {
        return DuplicateValidationResult.builder()
                .duplicate(true)
                .hash(hash)
                .duplicateDate(duplicateDate)
                .duplicateFilePath(duplicateFilePath)
                .build();
    }
}
