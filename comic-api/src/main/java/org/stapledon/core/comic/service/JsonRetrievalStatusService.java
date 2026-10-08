package org.stapledon.core.comic.service;

import org.springframework.stereotype.Service;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.repository.RetrievalStatusRepository;
import org.stapledon.common.service.RetrievalStatusService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@Service
@RequiredArgsConstructor
public class JsonRetrievalStatusService implements RetrievalStatusService {
    private final RetrievalStatusRepository repository;

    @Override
    public void recordRetrievalResult(ComicRetrievalRecord record) {
        repository.saveRecord(record);
        log.debug("Recorded retrieval result: {} for comic {} on {}",
                record.getStatus(), record.getComicName(), record.getComicDate());
    }

    @Override
    public Optional<ComicRetrievalRecord> getRetrievalRecord(String id) {
        return repository.getRecord(id);
    }

    @Override
    public List<ComicRetrievalRecord> getRetrievalRecords(
            String comicName,
            ComicRetrievalStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int limit) {

        return repository.getRecords(comicName, status, fromDate, toDate, limit);
    }

    @Override
    public Map<String, Object> getRetrievalSummary(LocalDate fromDate, LocalDate toDate) {
        List<ComicRetrievalRecord> records = repository.getRecords(null, null, fromDate, toDate, Integer.MAX_VALUE);

        Map<String, Object> summary = new HashMap<>();

        // Total counts by status
        Map<ComicRetrievalStatus, Long> countsByStatus = records.stream()
                .collect(Collectors.groupingBy(ComicRetrievalRecord::getStatus, Collectors.counting()));
        summary.put("countsByStatus", countsByStatus);

        // Total count
        summary.put("totalCount", records.size());

        // Success rate over the attempts that could succeed: a strip the source doesn't have (COMIC_UNAVAILABLE) is neither
        long successCount = countsByStatus.getOrDefault(ComicRetrievalStatus.SUCCESS, 0L);
        long unavailableCount = countsByStatus.getOrDefault(ComicRetrievalStatus.COMIC_UNAVAILABLE, 0L);
        long attemptable = records.size() - unavailableCount;
        double successRate = attemptable == 0 ? 0 : (double) successCount / attemptable;
        summary.put("successRate", successRate);

        // Average duration for successful retrievals
        double avgDurationMillis = records.stream()
                .filter(r -> r.getStatus() == ComicRetrievalStatus.SUCCESS)
                .mapToLong(ComicRetrievalRecord::getRetrievalDurationMs)
                .average()
                .orElse(0);
        summary.put("averageDurationMillis", avgDurationMillis);

        // Counts by status for each comic
        Map<String, Map<ComicRetrievalStatus, Long>> countsByComic = records.stream()
                .collect(Collectors.groupingBy(ComicRetrievalRecord::getComicName,
                        Collectors.groupingBy(ComicRetrievalRecord::getStatus, Collectors.counting())));
        summary.put("countsByComic", countsByComic);

        return summary;
    }

    @Override
    public boolean deleteRetrievalRecord(String id) {
        boolean deleted = repository.deleteRecord(id);
        log.info("Retrieval record {} delete: deleted={}", id, deleted);
        return deleted;
    }

    @Override
    public int purgeOldRecords(int daysToKeep) {
        int purged = repository.purgeOldRecords(daysToKeep);
        log.info("Purged {} retrieval records older than {} days", purged, daysToKeep);
        return purged;
    }
}