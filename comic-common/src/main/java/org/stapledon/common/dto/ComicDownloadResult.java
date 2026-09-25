package org.stapledon.common.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * Data Transfer Object representing the result of a comic download operation.
 * Contains the downloaded image data and status information about the download.
 */
@Data
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
public class ComicDownloadResult {

    /**
     * Why a download failed, so callers can tell a missing strip from a transient problem.
     */
    public enum FailureKind {
        /** Anything else: network errors, exceptions, unknown problems. */
        ERROR,
        /** The source answered HTTP 429 (Too Many Requests). */
        RATE_LIMITED,
        /** The source had nothing usable for the date (no image, empty or invalid image data, HTTP 404 or 410). */
        UNAVAILABLE
    }

    /**
     * The request that initiated this download.
     */
    @ToString.Include
    private final ComicDownloadRequest request;

    /**
     * The binary image data of the downloaded comic.
     */
    private final byte[] imageData;

    /**
     * Flag indicating whether the download was successful.
     */
    @ToString.Include
    private final boolean successful;

    /**
     * Error message in case the download failed.
     */
    private final String errorMessage;

    /**
     * The actual publication date discovered from the page (for indexed comics).
     * Null for date-based comics where the date is already known from the request.
     */
    @ToString.Include
    private final LocalDate actualDate;

    /**
     * The strip number (for indexed comics). Null for date-based comics.
     */
    @ToString.Include
    private final Integer stripNumber;

    /**
     * Transcript text extracted from the comic page (nullable).
     */
    private final String transcript;

    /**
     * Why the download failed. Null for successful results, and for failures that were not classified.
     */
    @ToString.Include
    private final FailureKind failureKind;

    /**
     * What happened when the downloaded image was saved (for example a duplicate of another date). Null when the result was not saved.
     */
    @ToString.Include
    private final SaveResult.Outcome saveOutcome;

    /**
     * Factory method to create a successful result.
     */
    public static ComicDownloadResult success(ComicDownloadRequest request, byte[] imageData) {
        return ComicDownloadResult.builder()
                .request(request)
                .imageData(imageData)
                .successful(true)
                .build();
    }

    /**
     * Factory method to create a successful result with metadata from indexed comics.
     */
    public static ComicDownloadResult successWithMetadata(ComicDownloadRequest request, byte[] imageData,
            LocalDate actualDate, Integer stripNumber, String transcript) {
        return ComicDownloadResult.builder()
                .request(request)
                .imageData(imageData)
                .successful(true)
                .actualDate(actualDate)
                .stripNumber(stripNumber)
                .transcript(transcript)
                .build();
    }

    /**
     * Factory method to create a failed result.
     */
    public static ComicDownloadResult failure(ComicDownloadRequest request, String errorMessage) {
        return failure(request, errorMessage, null);
    }

    /**
     * Factory method to create a failed result with the reason it failed.
     */
    public static ComicDownloadResult failure(ComicDownloadRequest request, String errorMessage, FailureKind failureKind) {
        return ComicDownloadResult.builder()
                .request(request)
                .successful(false)
                .errorMessage(errorMessage)
                .failureKind(failureKind)
                .build();
    }

    /**
     * True when the source rate-limited this download.
     */
    public boolean isRateLimited() {
        return failureKind == FailureKind.RATE_LIMITED;
    }
}