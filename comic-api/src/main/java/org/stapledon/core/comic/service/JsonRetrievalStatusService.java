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

        // Success rate
        long successCount = countsByStatus.getOrDefault(ComicRetrievalStatus.SUCCESS, 0L);
        double successRate = records.isEmpty() ? 0 : (double) successCount / records.size();
        summary.put("successRate", successRate);

        // Average duration for successful retrievals
        double avgDurationMillis = records.stream()
                .filter(r -> r.getStatus() == ComicRetrievalStatus.SUCCESS)
                .mapToLong(r -> r.getRetrievalDurationMs())
                .average()
                .orElse(0);
        summary.put("averageDurationMillis", avgDurationMillis);

        // Most common error types
        Map<ComicRetrievalStatus, Long> errorCounts = countsByStatus.entrySet().stream()
                .filter(e -> e.getKey() != ComicRetrievalStatus.SUCCESS)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        summary.put("errorCounts", errorCounts);

        // Comics with most failures
        Map<String, Long> failuresByComic = records.stream()
                .filter(r -> r.getStatus() != ComicRetrievalStatus.SUCCESS)
                .collect(Collectors.groupingBy(ComicRetrievalRecord::getComicName, Collectors.counting()));
        summary.put("comicsWithMostFailures", failuresByComic);

        return summary;
    }

    @Override
    public boolean deleteRetrievalRecord(String id) {
        return repository.deleteRecord(id);
    }

    @Override
    public int purgeOldRecords(int daysToKeep) {
        return repository.purgeOldRecords(daysToKeep);
    }
}