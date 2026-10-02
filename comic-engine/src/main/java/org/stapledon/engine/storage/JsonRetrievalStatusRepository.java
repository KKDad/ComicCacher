package org.stapledon.engine.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
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
    private final Clock clock;

    private ComicRetrievalRecordStorage recordStorage;

    /**
     * Reset the records (for testing purposes)
     */
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

        recordStorage.setLastUpdated(OffsetDateTime.now(ZoneOffset.UTC));

        Path storageFile = NfsFileOperations.resolvePath(cacheProperties.getLocation(), STORAGE_FILE);

        try {
            String json = gson.toJson(recordStorage);
            NfsFileOperations.atomicWrite(storageFile, json);
        } catch (IOException e) {
            log.error("Failed to save retrieval records to {}", storageFile, e);
        }
    }

    /**
     * Saves the record, stamping {@code attemptedAt} when it has none, and replaces the record for the same comic and date. Matching by
     * name and date as well as by id replaces records written before ids used the comic id.
     */
    @Override
    public synchronized void saveRecord(ComicRetrievalRecord record) {
        ComicRetrievalRecordStorage storage = loadRecords();

        ComicRetrievalRecord stamped = record.getAttemptedAt() != null ? record
                : record.toBuilder().attemptedAt(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)).build();
        storage.getRecords().removeIf(r -> r.getId().equals(stamped.getId())
                || Objects.equals(r.getComicName(), stamped.getComicName()) && Objects.equals(r.getComicDate(), stamped.getComicDate()));
        storage.getRecords().add(stamped);
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
        OffsetDateTime cutoff = OffsetDateTime.now(clock).minusDays(daysToKeep);
        LocalDate cutoffDate = LocalDate.now(clock).minusDays(daysToKeep);

        // By when the attempt was made, so a backfill of an old strip keeps its record; older records have only the strip's date
        storage.getRecords().removeIf(record -> record.getAttemptedAt() != null
                ? record.getAttemptedAt().isBefore(cutoff)
                : record.getComicDate().isBefore(cutoffDate));

        int removedCount = initialSize - storage.getRecords().size();

        if (removedCount > 0) {
            saveRecords();
            log.info("Purged {} retrieval records older than {} days", removedCount, daysToKeep);
        }

        return removedCount;
    }
}
