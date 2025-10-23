package org.stapledon.common.service;

import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing comic retrieval status records.
 */
public interface RetrievalStatusService {
    /**
     * Records a comic retrieval result
     */
    void recordRetrievalResult(ComicRetrievalRecord record);
    
    /**
     * Gets a specific retrieval record by ID
     */
    Optional<ComicRetrievalRecord> getRetrievalRecord(String id);
    
    /**
     * Gets retrieval records with optional filtering
     */
    List<ComicRetrievalRecord> getRetrievalRecords(
            String comicName, 
            ComicRetrievalStatus status, 
            LocalDate fromDate, 
            LocalDate toDate, 
            int limit);
    
    /**
     * Gets retrieval summary statistics
     */
    Map<String, Object> getRetrievalSummary(LocalDate fromDate, LocalDate toDate);
    
    /**
     * Deletes a specific retrieval record
     */
    boolean deleteRetrievalRecord(String id);
    
    /**
     * Purges retrieval records older than specified days
     */
    int purgeOldRecords(int daysToKeep);
}