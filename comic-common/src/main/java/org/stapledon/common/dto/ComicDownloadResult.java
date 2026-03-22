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
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class ComicDownloadResult {
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
        return ComicDownloadResult.builder()
                .request(request)
                .successful(false)
                .errorMessage(errorMessage)
                .build();
    }
}