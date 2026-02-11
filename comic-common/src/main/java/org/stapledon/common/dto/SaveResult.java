package org.stapledon.common.dto;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.time.LocalDate;

/**
 * Data Transfer Object representing the result of a comic strip save operation.
 * Provides detailed outcome information beyond simple success/failure.
 */
@Value
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class SaveResult {

    /**
     * Enum representing the specific outcome of a save operation.
     */
    public enum Outcome {
        /**
         * Image was successfully saved to storage.
         */
        SAVED,

        /**
         * Save was skipped because an identical image already exists for a different date.
         */
        DUPLICATE_SKIPPED,

        /**
         * Save failed due to image validation errors (invalid format, dimensions, etc.).
         */
        VALIDATION_FAILED,

        /**
         * Save failed due to I/O errors (disk full, permissions, index update failure, etc.).
         */
        IO_ERROR
    }

    /**
     * The specific outcome of this save operation.
     */
    @ToString.Include
    Outcome outcome;

    /**
     * Optional message providing additional details about the outcome.
     * Typically contains error details for failed operations.
     */
    @ToString.Include
    String message;

    /**
     * If the outcome is DUPLICATE_SKIPPED, this contains the date of the existing duplicate image.
     * Null for all other outcomes.
     */
    @ToString.Include
    LocalDate duplicateDate;

    /**
     * Checks if the save operation was successful (either saved or duplicate was properly detected).
     *
     * @return true if outcome is SAVED or DUPLICATE_SKIPPED, false otherwise
     */
    public boolean isSuccess() {
        return outcome == Outcome.SAVED || outcome == Outcome.DUPLICATE_SKIPPED;
    }

    /**
     * Checks if the image was actually written to disk (not skipped as duplicate).
     *
     * @return true only if outcome is SAVED
     */
    public boolean wasSaved() {
        return outcome == Outcome.SAVED;
    }

    /**
     * Factory method to create a successful save result.
     *
     * @return A SaveResult indicating the image was successfully saved
     */
    public static SaveResult saved() {
        return SaveResult.builder()
                .outcome(Outcome.SAVED)
                .build();
    }

    /**
     * Factory method to create a result indicating the save was skipped due to duplicate detection.
     *
     * @param existingDate The date of the existing duplicate image
     * @return A SaveResult indicating a duplicate was found
     */
    public static SaveResult duplicateSkipped(LocalDate existingDate) {
        return SaveResult.builder()
                .outcome(Outcome.DUPLICATE_SKIPPED)
                .duplicateDate(existingDate)
                .message("Image already exists for date: " + existingDate)
                .build();
    }

    /**
     * Factory method to create a result indicating validation failure.
     *
     * @param errorMessage Description of the validation error
     * @return A SaveResult indicating validation failure
     */
    public static SaveResult validationFailed(String errorMessage) {
        return SaveResult.builder()
                .outcome(Outcome.VALIDATION_FAILED)
                .message(errorMessage)
                .build();
    }

    /**
     * Factory method to create a result indicating I/O error.
     *
     * @param errorMessage Description of the I/O error
     * @return A SaveResult indicating I/O failure
     */
    public static SaveResult ioError(String errorMessage) {
        return SaveResult.builder()
                .outcome(Outcome.IO_ERROR)
                .message(errorMessage)
                .build();
    }
}
