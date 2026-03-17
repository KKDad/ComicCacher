package org.stapledon.engine.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalRecordStorage;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.repository.RetrievalStatusRepository;
import org.stapledon.common.util.NfsFileOperations;

@Slf4j
@ToString
@Repository
@RequiredArgsConstructor
public class JsonRetrievalStatusRepository implements RetrievalStatusRepository {
    private static final String STORAGE_FILE = "retrieval-status.json";

    @Qualifier("gsonWithLocalDate")
    private final Gson gson;
    private final CacheProperties cacheProperties;

    private ComicRetrievalRecordStorage recordStorage;

    /**
     * Reset the records (for testing purposes)
     */
    @com.google.common.annotations.VisibleForTesting
    public synchronized void resetRecords() {
        recordStorage = new ComicRetrievalRecordStorage();
    }

    /**
     * Load records from storage file
     */
    private synchronized ComicRetrievalRecordStorage loadRecords() {
        if (recordStorage != null) {
            return recordStorage;
        }

        Path storageFile = NfsFileOperations.resolvePath(cacheProperties.getLocation(), STORAGE_FILE);

        if (!NfsFileOperations.exists(storageFile)) {
            recordStorage = new ComicRetrievalRecordStorage();
            return recordStorage;
        }

        try (Reader reader = Files.newBufferedReader(storageFile)) {
            Type storageType = new TypeToken<ComicRetrievalRecordStorage>() {
            }.getType();
            recordStorage = gson.fromJson(reader, storageType);

            if (recordStorage == null) {
                recordStorage = new ComicRetrievalRecordStorage();
            }

            return recordStorage;
        } catch (IOException e) {
            log.error("Failed to load retrieval records: {}", e.getMessage(), e);
            recordStorage = new ComicRetrievalRecordStorage();
            return recordStorage;
        }
    }

    /**
     * Save records to storage file using atomic write for NFS safety
     */
    private synchronized void saveRecords() {
        if (recordStorage == null) {
            return;
        }

        recordStorage.setLastUpdated(java.time.OffsetDateTime.now());

        Path storageFile = NfsFileOperations.resolvePath(cacheProperties.getLocation(), STORAGE_FILE);

        try {
            String json = gson.toJson(recordStorage);
            NfsFileOperations.atomicWrite(storageFile, json);
        } catch (IOException e) {
            log.error("Failed to save retrieval records: {}", e.getMessage(), e);
        }
    }

    @Override
    public void saveRecord(ComicRetrievalRecord record) {
        ComicRetrievalRecordStorage storage = loadRecords();

        // Since we're using comic name and date as ID, we need to check for existing
        // record
        // and replace it if it exists
        storage.getRecords().removeIf(r -> r.getId().equals(record.getId()));
        storage.getRecords().add(record);
        saveRecords();
    }

    @Override
    public Optional<ComicRetrievalRecord> getRecord(String id) {
        ComicRetrievalRecordStorage storage = loadRecords();

        return storage.getRecords().stream()
                .filter(record -> record.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<ComicRetrievalRecord> getRecords(
            String comicName,
            ComicRetrievalStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int limit) {

        ComicRetrievalRecordStorage storage = loadRecords();

        return storage.getRecords().stream()
                .filter(record -> comicName == null || record.getComicName().equals(comicName))
                .filter(record -> status == null || record.getStatus() == status)
                .filter(record -> fromDate == null
                        || (record.getComicDate() != null && !record.getComicDate().isBefore(fromDate)))
                .filter(record -> toDate == null
                        || (record.getComicDate() != null && !record.getComicDate().isAfter(toDate)))
                .sorted((r1, r2) -> r2.getComicDate().compareTo(r1.getComicDate())) // Latest first
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteRecord(String id) {
        ComicRetrievalRecordStorage storage = loadRecords();

        int initialSize = storage.getRecords().size();
        storage.getRecords().removeIf(record -> record.getId().equals(id));

        if (storage.getRecords().size() < initialSize) {
            saveRecords();
            return true;
        }

        return false;
    }

    @Override
    public int purgeOldRecords(int daysToKeep) {
        ComicRetrievalRecordStorage storage = loadRecords();

        int initialSize = storage.getRecords().size();
        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);

        storage.getRecords().removeIf(record -> record.getComicDate().isBefore(cutoffDate));

        int removedCount = initialSize - storage.getRecords().size();

        if (removedCount > 0) {
            saveRecords();
            log.info("Purged {} retrieval records older than {} days", removedCount, daysToKeep);
        }

        return removedCount;
    }

    @Override
    public int getRecordCountByStatus(ComicRetrievalStatus status) {
        ComicRetrievalRecordStorage storage = loadRecords();

        return (int) storage.getRecords().stream()
                .filter(record -> record.getStatus() == status)
                .count();
    }

    /**
     * Automatically purge old records every day at 2:30 AM
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void scheduledPurge() {
        log.info("Running scheduled purge of old retrieval records");
        purgeOldRecords(7);
    }
}
