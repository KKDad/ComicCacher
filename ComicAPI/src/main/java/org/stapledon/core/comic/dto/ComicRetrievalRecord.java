package org.stapledon.core.comic.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Records information about a comic retrieval attempt.
 */
@Data
@Builder
public class ComicRetrievalRecord {
    /**
     * Unique identifier for this record in the format "ComicName_YYYY-MM-DD"
     */
    private final String id;
    
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
     * Factory method to create a successful record
     */
    public static ComicRetrievalRecord success(
            String comicName, LocalDate comicDate, 
            String source, long retrievalDurationMs, Long imageSize) {
        return ComicRetrievalRecord.builder()
                .id(generateId(comicName, comicDate))
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
            String comicName, LocalDate comicDate, 
            String source, ComicRetrievalStatus status, String errorMessage, 
            long retrievalDurationMs, Integer httpStatusCode) {
        return ComicRetrievalRecord.builder()
                .id(generateId(comicName, comicDate))
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
    private static String generateId(String comicName, LocalDate comicDate) {
        return comicName + "_" + comicDate;
    }
}