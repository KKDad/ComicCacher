package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.service.RetrievalStatusService;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for Retrieval Status operations.
 * Provides queries for retrieval records and summary.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RetrievalStatusResolver {

    private final RetrievalStatusService retrievalStatusService;

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get retrieval records with optional filtering.
     */
    @QueryMapping
    public List<ComicRetrievalRecord> retrievalRecords(
            @Argument String comicName,
            @Argument String status,
            @Argument LocalDate fromDate,
            @Argument LocalDate toDate,
            @Argument Integer limit) {

        log.debug("Getting retrieval records: comicName={}, status={}, fromDate={}, toDate={}, limit={}",
                comicName, status, fromDate, toDate, limit);

        ComicRetrievalStatus statusEnum = null;
        if (status != null) {
            try {
                statusEnum = ComicRetrievalStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status value: {}", status);
            }
        }

        int recordLimit = limit != null ? limit : 100;
        return retrievalStatusService.getRetrievalRecords(comicName, statusEnum, fromDate, toDate, recordLimit);
    }

    /**
     * Get a specific retrieval record by ID.
     */
    @QueryMapping
    public ComicRetrievalRecord retrievalRecord(@Argument String id) {
        log.debug("Getting retrieval record: {}", id);
        return retrievalStatusService.getRetrievalRecord(id).orElse(null);
    }

    /**
     * Get summary statistics of retrieval operations.
     */
    @QueryMapping
    public Object retrievalSummary(
            @Argument LocalDate fromDate,
            @Argument LocalDate toDate) {
        log.debug("Getting retrieval summary: fromDate={}, toDate={}", fromDate, toDate);
        return retrievalStatusService.getRetrievalSummary(fromDate, toDate);
    }

    /**
     * Get retrieval records for a specific comic.
     */
    @QueryMapping
    public List<ComicRetrievalRecord> retrievalRecordsForComic(
            @Argument String comicName,
            @Argument Integer limit) {
        log.debug("Getting retrieval records for comic: {}, limit={}", comicName, limit);
        int recordLimit = limit != null ? limit : 20;
        return retrievalStatusService.getRetrievalRecords(comicName, null, null, null, recordLimit);
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Delete a specific retrieval record.
     */
    @MutationMapping
    public boolean deleteRetrievalRecord(@Argument String id) {
        log.info("Deleting retrieval record: {}", id);
        return retrievalStatusService.deleteRetrievalRecord(id);
    }

    /**
     * Purge old retrieval records.
     */
    @MutationMapping
    public int purgeOldRetrievalRecords(@Argument Integer daysToKeep) {
        int days = daysToKeep != null ? daysToKeep : 7;
        log.info("Purging retrieval records older than {} days", days);
        return retrievalStatusService.purgeOldRecords(days);
    }
}
