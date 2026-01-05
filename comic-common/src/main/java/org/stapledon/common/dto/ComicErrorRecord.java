package org.stapledon.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Records information about a comic retrieval error.
 * Used to track the last N errors per comic for troubleshooting.
 */
@Data
@Builder
public class ComicErrorRecord {
    /**
     * The comic name that failed
     */
    private final String comicName;

    /**
     * The date for which the comic retrieval was attempted
     */
    private final LocalDate comicDate;

    /**
     * The source of the comic (e.g., "gocomics", "comicskingdom")
     */
    private final String source;

    /**
     * The retrieval status indicating the type of error
     */
    private final ComicRetrievalStatus status;

    /**
     * Detailed error message
     */
    private final String errorMessage;

    /**
     * HTTP status code if applicable
     */
    private final Integer httpStatusCode;

    /**
     * Timestamp when the error occurred
     */
    private final LocalDateTime timestamp;

    /**
     * Duration of the failed retrieval operation in milliseconds
     */
    private final long retrievalDurationMs;

    /**
     * Factory method to create an error record from a ComicRetrievalRecord
     */
    public static ComicErrorRecord fromRetrievalRecord(ComicRetrievalRecord record) {
        return ComicErrorRecord.builder()
                .comicName(record.getComicName())
                .comicDate(record.getComicDate())
                .source(record.getSource())
                .status(record.getStatus())
                .errorMessage(record.getErrorMessage())
                .httpStatusCode(record.getHttpStatusCode())
                .timestamp(LocalDateTime.now())
                .retrievalDurationMs(record.getRetrievalDurationMs())
                .build();
    }
}
