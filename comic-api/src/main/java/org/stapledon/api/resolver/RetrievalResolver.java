package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.payload.MutationPayloads.DeleteRetrievalRecordPayload;
import org.stapledon.api.dto.payload.MutationPayloads.PurgeRetrievalRecordsPayload;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.service.RetrievalStatusService;
import org.stapledon.core.comic.service.RetrievalHealthService;
import org.stapledon.core.comic.service.RetrievalHealthService.RetrievalHealth;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for retrieval status queries and mutations.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RetrievalResolver {

    private final RetrievalStatusService retrievalStatusService;
    private final RetrievalHealthService retrievalHealthService;

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get retrieval records with optional filtering.
     */
    @QueryMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public List<RetrievalRecordDto> retrievalRecords(
            @Argument String comicName,
            @Argument ComicRetrievalStatus status,
            @Argument LocalDate fromDate,
            @Argument LocalDate toDate,
            @Argument Integer limit) {

        log.debug("Getting retrieval records: comicName={}, status={}, fromDate={}, toDate={}, limit={}",
                comicName, status, fromDate, toDate, limit);

        int effectiveLimit = Optional.ofNullable(limit).orElse(100);

        List<ComicRetrievalRecord> records = retrievalStatusService.getRetrievalRecords(
                comicName, status, fromDate, toDate, effectiveLimit);

        return records.stream()
                .map(RetrievalResolver::toDto)
                .toList();
    }

    /**
     * Get a specific retrieval record by ID.
     */
    @QueryMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public RetrievalRecordDto retrievalRecord(@Argument String id) {
        log.debug("Getting retrieval record: id={}", id);
        return retrievalStatusService.getRetrievalRecord(id)
                .map(RetrievalResolver::toDto)
                .orElse(null);
    }

    /**
     * Get retrieval records for a specific comic.
     */
    @QueryMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public List<RetrievalRecordDto> retrievalRecordsForComic(
            @Argument String comicName,
            @Argument Integer limit) {

        log.debug("Getting retrieval records for comic: comicName={}, limit={}", comicName, limit);

        int effectiveLimit = Optional.ofNullable(limit).orElse(20);

        List<ComicRetrievalRecord> records = retrievalStatusService.getRetrievalRecords(
                comicName, null, null, null, effectiveLimit);

        return records.stream()
                .map(RetrievalResolver::toDto)
                .toList();
    }

    /**
     * Get summary statistics of retrieval operations.
     */
    @QueryMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public RetrievalSummaryDto retrievalSummary(
            @Argument LocalDate fromDate,
            @Argument LocalDate toDate) {

        log.debug("Getting retrieval summary: fromDate={}, toDate={}", fromDate, toDate);

        Map<String, Object> raw = retrievalStatusService.getRetrievalSummary(fromDate, toDate);
        return buildSummary(raw);
    }

    /**
     * The retrieval-status page's data: the latest run, today's results and errors, and each comic's result per day.
     */
    @QueryMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public RetrievalHealth retrievalHealth(@Argument Integer days, @Argument Integer errorLimit) {
        log.debug("Getting retrieval health: days={}, errorLimit={}", days, errorLimit);
        return retrievalHealthService.getHealth(Optional.ofNullable(days).orElse(30), Optional.ofNullable(errorLimit).orElse(20));
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Delete a specific retrieval record.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DeleteRetrievalRecordPayload deleteRetrievalRecord(@Argument String id) {
        log.info("Deleting retrieval record: id={}", id);
        boolean deleted = retrievalStatusService.deleteRetrievalRecord(id);
        return new DeleteRetrievalRecordPayload(deleted, List.of());
    }

    /**
     * Purge retrieval records older than specified days.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PurgeRetrievalRecordsPayload purgeRetrievalRecords(@Argument Integer daysToKeep) {
        int days = Optional.ofNullable(daysToKeep).orElse(7);
        log.info("Purging retrieval records older than {} days", days);
        int purged = retrievalStatusService.purgeOldRecords(days);
        return new PurgeRetrievalRecordsPayload(purged, List.of());
    }

    // =========================================================================
    // Mapping helpers
    // =========================================================================

    private static RetrievalRecordDto toDto(ComicRetrievalRecord record) {
        return new RetrievalRecordDto(
                record.getId(),
                record.getComicName(),
                record.getComicDate(),
                record.getSource(),
                record.getStatus(),
                record.getErrorMessage(),
                (double) record.getRetrievalDurationMs(),
                record.getImageSize() != null ? record.getImageSize().doubleValue() : null,
                record.getHttpStatusCode(),
                record.getComicId(),
                record.getAttemptedAt());
    }

    @SuppressWarnings("unchecked")
    private static RetrievalSummaryDto buildSummary(Map<String, Object> raw) {
        int totalCount = (int) raw.getOrDefault("totalCount", 0);

        Map<ComicRetrievalStatus, Long> countsByStatus =
                (Map<ComicRetrievalStatus, Long>) raw.getOrDefault("countsByStatus", Map.of());

        long successCount = countsByStatus.getOrDefault(ComicRetrievalStatus.SUCCESS, 0L);
        long failureCount = countsByStatus.entrySet().stream()
                .filter(e -> e.getKey() != ComicRetrievalStatus.SUCCESS)
                .mapToLong(Map.Entry::getValue)
                .sum();
        long skippedCount = countsByStatus.getOrDefault(ComicRetrievalStatus.COMIC_UNAVAILABLE, 0L);

        double successRate = (double) raw.getOrDefault("successRate", 0.0);
        double avgDuration = (double) raw.getOrDefault("averageDurationMillis", 0.0);

        List<StatusCountDto> byStatus = countsByStatus.entrySet().stream()
                .map(e -> new StatusCountDto(e.getKey(), e.getValue().intValue()))
                .toList();

        Map<String, Map<ComicRetrievalStatus, Long>> countsByComic =
                (Map<String, Map<ComicRetrievalStatus, Long>>) raw.getOrDefault("countsByComic", Map.of());

        List<ComicRetrievalSummaryDto> byComic = countsByComic.entrySet().stream()
                .map(e -> {
                    Map<ComicRetrievalStatus, Long> counts = e.getValue();
                    long total = counts.values().stream().mapToLong(Long::longValue).sum();
                    long success = counts.getOrDefault(ComicRetrievalStatus.SUCCESS, 0L);
                    long unavailable = counts.getOrDefault(ComicRetrievalStatus.COMIC_UNAVAILABLE, 0L);
                    return new ComicRetrievalSummaryDto(e.getKey(), (int) total, (int) success, (int) (total - success - unavailable));
                })
                .toList();

        return new RetrievalSummaryDto(
                totalCount,
                (int) successCount,
                (int) (failureCount - skippedCount),
                (int) skippedCount,
                successRate * 100.0,
                avgDuration > 0 ? avgDuration : null,
                byComic,
                byStatus);
    }

    // =========================================================================
    // Record Types for GraphQL
    // =========================================================================

    public record RetrievalRecordDto(
            String id,
            String comicName,
            LocalDate comicDate,
            String source,
            ComicRetrievalStatus status,
            String errorMessage,
            Double retrievalDurationMs,
            Double imageSize,
            Integer httpStatusCode,
            Integer comicId,
            OffsetDateTime attemptedAt) {
    }

    public record RetrievalSummaryDto(
            int totalAttempts,
            int successCount,
            int failureCount,
            int skippedCount,
            double successRate,
            Double averageDurationMs,
            List<ComicRetrievalSummaryDto> byComic,
            List<StatusCountDto> byStatus) {
    }

    public record ComicRetrievalSummaryDto(String comicName, int totalAttempts, int successCount, int failureCount) {
    }

    public record StatusCountDto(ComicRetrievalStatus status, int count) {
    }
}
