package org.stapledon.common.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * Records information about a comic retrieval attempt.
 */
@Data
@Builder(toBuilder = true)
public class ComicRetrievalRecord {
    /**
     * Unique identifier for this record: "{comicId}_{yyyy-MM-dd}", or "{comicName}_{yyyy-MM-dd}" for records written before the
     * comic id was kept. Treat it as opaque.
     */
    private final String id;

    /**
     * The comic's id (null on records written before it was kept)
     */
    private final Integer comicId;

    /**
     * The comic name
     */
    private final String comicName;

    /**
     * The date for which the comic was retrieved
     */
    private final LocalDate comicDate;

    /**
     * The source of the comic (e.g., "gocomics", "comicskingdom")
     */
    private final String source;

    /**
     * The retrieval status
     */
    private final ComicRetrievalStatus status;

    /**
     * Error message if retrieval failed
     */
    private final String errorMessage;

    /**
     * Duration of the retrieval operation in milliseconds
     */
    private final long retrievalDurationMs;

    /**
     * Size of the retrieved image in bytes (if successful)
     */
    private final Long imageSize;

    /**
     * HTTP status code from the comic source (if applicable)
     */
    private final Integer httpStatusCode;

    /**
     * When the attempt was made, in UTC (null on records written before it was kept). The repository stamps it on save.
     */
    private final OffsetDateTime attemptedAt;

    /**
     * Factory method to create a successful record
     */
    public static ComicRetrievalRecord success(
            Integer comicId, String comicName, LocalDate comicDate,
            String source, long retrievalDurationMs, Long imageSize) {
        return ComicRetrievalRecord.builder()
                .id(generateId(comicId, comicName, comicDate))
                .comicId(comicId)
                .comicName(comicName)
                .comicDate(comicDate)
                .source(source)
                .status(ComicRetrievalStatus.SUCCESS)
                .retrievalDurationMs(retrievalDurationMs)
                .imageSize(imageSize)
                .build();
    }

    /**
     * Factory method to create a failed record
     */
    public static ComicRetrievalRecord failure(
            Integer comicId, String comicName, LocalDate comicDate,
            String source, ComicRetrievalStatus status, String errorMessage,
            long retrievalDurationMs, Integer httpStatusCode) {
        return ComicRetrievalRecord.builder()
                .id(generateId(comicId, comicName, comicDate))
                .comicId(comicId)
                .comicName(comicName)
                .comicDate(comicDate)
                .source(source)
                .status(status)
                .errorMessage(errorMessage)
                .retrievalDurationMs(retrievalDurationMs)
                .httpStatusCode(httpStatusCode)
                .build();
    }

    /**
     * Generates a unique ID for this record
     */
    private static String generateId(Integer comicId, String comicName, LocalDate comicDate) {
        return (comicId != null ? comicId.toString() : comicName) + "_" + comicDate;
    }
}
