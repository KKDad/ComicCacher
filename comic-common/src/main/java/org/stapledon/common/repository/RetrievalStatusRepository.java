package org.stapledon.common.repository;

import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for comic retrieval status records.
 */
public interface RetrievalStatusRepository {
    /**
     * Save a new retrieval record
     */
    void saveRecord(ComicRetrievalRecord record);
    
    /**
     * Get a specific retrieval record by ID
     */
    Optional<ComicRetrievalRecord> getRecord(String id);
    
    /**
     * Get retrieval records with optional filtering
     * 
     * All parameters are optional:
     * - When comicName or status are null, no filtering is applied for that field
     * - When fromDate or toDate are null, no date filtering is applied
     * - Records are limited to the available 7-day window regardless of date parameters
     */
    List<ComicRetrievalRecord> getRecords(
            String comicName, 
            ComicRetrievalStatus status, 
            LocalDate fromDate, 
            LocalDate toDate, 
            int limit);
    
    /**
     * Delete a specific retrieval record
     */
    boolean deleteRecord(String id);
    
    /**
     * Delete all records older than specified days
     */
    int purgeOldRecords(int daysToKeep);
    
    /**
     * Get count of records by status
     */
    int getRecordCountByStatus(ComicRetrievalStatus status);
    
    /**
     * Reset all records (for testing purposes)
     */
    @com.google.common.annotations.VisibleForTesting
    void resetRecords();
}