package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.service.RetrievalStatusService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get retrieval records with optional filtering.
     */
    @QueryMapping
    public List<Map<String, Object>> retrievalRecords(
            @Argument String comicName,
            @Argument String status,
            @Argument LocalDate fromDate,
            @Argument LocalDate toDate,
            @Argument Integer limit) {

        log.debug("Getting retrieval records: comicName={}, status={}, fromDate={}, toDate={}, limit={}",
                comicName, status, fromDate, toDate, limit);

        ComicRetrievalStatus javaStatus = mapGraphqlStatusToJava(status);
        int effectiveLimit = Optional.ofNullable(limit).orElse(100);

        List<ComicRetrievalRecord> records = retrievalStatusService.getRetrievalRecords(
                comicName, javaStatus, fromDate, toDate, effectiveLimit);

        return records.stream()
                .map(this::mapRecordToGraphql)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific retrieval record by ID.
     */
    @QueryMapping
    public Map<String, Object> retrievalRecord(@Argument String id) {
        log.debug("Getting retrieval record: id={}", id);
        return retrievalStatusService.getRetrievalRecord(id)
                .map(this::mapRecordToGraphql)
                .orElse(null);
    }

    /**
     * Get retrieval records for a specific comic.
     */
    @QueryMapping
    public List<Map<String, Object>> retrievalRecordsForComic(
            @Argument String comicName,
            @Argument Integer limit) {

        log.debug("Getting retrieval records for comic: comicName={}, limit={}", comicName, limit);

        int effectiveLimit = Optional.ofNullable(limit).orElse(20);

        List<ComicRetrievalRecord> records = retrievalStatusService.getRetrievalRecords(
                comicName, null, null, null, effectiveLimit);

        return records.stream()
                .map(this::mapRecordToGraphql)
                .collect(Collectors.toList());
    }

    /**
     * Get summary statistics of retrieval operations.
     */
    @QueryMapping
    public Map<String, Object> retrievalSummary(
            @Argument LocalDate fromDate,
            @Argument LocalDate toDate) {

        log.debug("Getting retrieval summary: fromDate={}, toDate={}", fromDate, toDate);

        Map<String, Object> rawSummary = retrievalStatusService.getRetrievalSummary(fromDate, toDate);

        return buildGraphqlSummary(rawSummary);
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Delete a specific retrieval record.
     */
    @MutationMapping
    public boolean deleteRetrievalRecord(@Argument String id) {
        log.info("Deleting retrieval record: id={}", id);
        return retrievalStatusService.deleteRetrievalRecord(id);
    }

    /**
     * Purge retrieval records older than specified days.
     */
    @MutationMapping
    public int purgeRetrievalRecords(@Argument Integer daysToKeep) {
        int days = Optional.ofNullable(daysToKeep).orElse(7);
        log.info("Purging retrieval records older than {} days", days);
        return retrievalStatusService.purgeOldRecords(days);
    }

    // =========================================================================
    // Mapping helpers
    // =========================================================================

    private Map<String, Object> mapRecordToGraphql(ComicRetrievalRecord record) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", record.getId());
        map.put("comicName", record.getComicName());
        map.put("comicDate", record.getComicDate());
        map.put("source", record.getSource());
        map.put("status", mapJavaStatusToGraphql(record.getStatus()));
        map.put("errorMessage", record.getErrorMessage());
        map.put("retrievalDurationMs", (double) record.getRetrievalDurationMs());
        map.put("imageSize", record.getImageSize() != null ? record.getImageSize().doubleValue() : null);
        map.put("httpStatusCode", record.getHttpStatusCode());
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildGraphqlSummary(Map<String, Object> raw) {
        Map<String, Object> summary = new LinkedHashMap<>();

        int totalCount = (int) raw.getOrDefault("totalCount", 0);
        summary.put("totalAttempts", totalCount);

        Map<ComicRetrievalStatus, Long> countsByStatus =
                (Map<ComicRetrievalStatus, Long>) raw.getOrDefault("countsByStatus", Map.of());

        long successCount = countsByStatus.getOrDefault(ComicRetrievalStatus.SUCCESS, 0L);
        long failureCount = countsByStatus.entrySet().stream()
                .filter(e -> e.getKey() != ComicRetrievalStatus.SUCCESS)
                .mapToLong(Map.Entry::getValue)
                .sum();
        long skippedCount = countsByStatus.getOrDefault(ComicRetrievalStatus.COMIC_UNAVAILABLE, 0L);

        summary.put("successCount", (int) successCount);
        summary.put("failureCount", (int) (failureCount - skippedCount));
        summary.put("skippedCount", (int) skippedCount);

        double successRate = (double) raw.getOrDefault("successRate", 0.0);
        summary.put("successRate", successRate * 100.0);

        double avgDuration = (double) raw.getOrDefault("averageDurationMillis", 0.0);
        summary.put("averageDurationMs", avgDuration > 0 ? avgDuration : null);

        // byStatus breakdown
        List<Map<String, Object>> byStatus = new ArrayList<>();
        for (Map.Entry<ComicRetrievalStatus, Long> entry : countsByStatus.entrySet()) {
            Map<String, Object> statusCount = new LinkedHashMap<>();
            statusCount.put("status", mapJavaStatusToGraphql(entry.getKey()));
            statusCount.put("count", entry.getValue().intValue());
            byStatus.add(statusCount);
        }
        summary.put("byStatus", byStatus);

        // byComic breakdown
        Map<String, Long> failuresByComic =
                (Map<String, Long>) raw.getOrDefault("comicsWithMostFailures", Map.of());
        List<Map<String, Object>> byComic = new ArrayList<>();

        // We need per-comic totals from countsByStatus — but the raw summary only has failure counts per comic.
        // Reconstruct from available data: we have total failures by comic.
        for (Map.Entry<String, Long> entry : failuresByComic.entrySet()) {
            Map<String, Object> comicSummary = new LinkedHashMap<>();
            comicSummary.put("comicName", entry.getKey());
            comicSummary.put("failureCount", entry.getValue().intValue());
            comicSummary.put("successCount", 0);
            comicSummary.put("totalAttempts", entry.getValue().intValue());
            byComic.add(comicSummary);
        }
        summary.put("byComic", byComic);

        return summary;
    }

    private String mapJavaStatusToGraphql(ComicRetrievalStatus status) {
        return switch (status) {
            case SUCCESS -> "SUCCESS";
            case NETWORK_ERROR -> "ERROR";
            case PARSING_ERROR -> "FAILURE";
            case COMIC_UNAVAILABLE -> "NOT_FOUND";
            case AUTHENTICATION_ERROR -> "ERROR";
            case STORAGE_ERROR -> "FAILURE";
            case UNKNOWN_ERROR -> "ERROR";
        };
    }

    private ComicRetrievalStatus mapGraphqlStatusToJava(String graphqlStatus) {
        if (graphqlStatus == null) {
            return null;
        }
        return switch (graphqlStatus) {
            case "SUCCESS" -> ComicRetrievalStatus.SUCCESS;
            case "FAILURE" -> ComicRetrievalStatus.PARSING_ERROR;
            case "ERROR" -> ComicRetrievalStatus.NETWORK_ERROR;
            case "NOT_FOUND" -> ComicRetrievalStatus.COMIC_UNAVAILABLE;
            case "SKIPPED" -> ComicRetrievalStatus.COMIC_UNAVAILABLE;
            case "RATE_LIMITED" -> ComicRetrievalStatus.NETWORK_ERROR;
            default -> null;
        };
    }
}
