package org.stapledon.engine.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicErrorRecord;
import org.stapledon.common.service.ErrorTrackingService;
import org.stapledon.common.util.NfsFileOperations;

/**
 * JSON file-based implementation of ErrorTrackingService.
 * Stores error records in last_errors.json in the cache root directory.
 */
@Slf4j
@ToString
@Repository
public class JsonErrorTrackingRepository implements ErrorTrackingService {
    private static final String STORAGE_FILE = "last_errors.json";

    @Qualifier("gsonWithLocalDate")
    private final Gson gson;
    private final CacheProperties cacheProperties;
    private final int maxErrorsPerComic;

    private Map<String, List<ComicErrorRecord>> errorCache;

    public JsonErrorTrackingRepository(
            @Qualifier("gsonWithLocalDate") Gson gson,
            CacheProperties cacheProperties,
            @Value("${comics.metrics.error-tracking.max-errors-per-comic:5}") int maxErrorsPerComic) {
        this.gson = gson;
        this.cacheProperties = cacheProperties;
        this.maxErrorsPerComic = maxErrorsPerComic;
    }

    /**
     * Load errors from storage file
     */
    private synchronized Map<String, List<ComicErrorRecord>> loadErrors() {
        if (errorCache != null) {
            return errorCache;
        }

        Path storageFile = NfsFileOperations.resolvePath(cacheProperties.getLocation(), STORAGE_FILE);

        if (!NfsFileOperations.exists(storageFile)) {
            errorCache = new HashMap<>();
            return errorCache;
        }

        try (Reader reader = Files.newBufferedReader(storageFile)) {
            Type mapType = new TypeToken<Map<String, List<ComicErrorRecord>>>() {
            }.getType();
            errorCache = gson.fromJson(reader, mapType);

            if (errorCache == null) {
                errorCache = new HashMap<>();
            }

            return errorCache;
        } catch (IOException e) {
            log.error("Failed to load error records: {}", e.getMessage(), e);
            errorCache = new HashMap<>();
            return errorCache;
        }
    }

    /**
     * Save errors to storage file using atomic write for NFS safety
     */
    private synchronized void saveErrors() {
        if (errorCache == null) {
            return;
        }

        Path storageFile = NfsFileOperations.resolvePath(cacheProperties.getLocation(), STORAGE_FILE);

        try {
            String json = gson.toJson(errorCache);
            NfsFileOperations.atomicWrite(storageFile, json);
        } catch (IOException e) {
            log.error("Failed to save error records: {}", e.getMessage(), e);
        }
    }

    @Override
    public void recordError(ComicErrorRecord error) {
        Map<String, List<ComicErrorRecord>> errors = loadErrors();

        List<ComicErrorRecord> comicErrors = errors.computeIfAbsent(
                error.getComicName(),
                k -> new ArrayList<>());

        // Add new error at the beginning
        comicErrors.add(0, error);

        // Keep only the last N errors
        if (comicErrors.size() > maxErrorsPerComic) {
            comicErrors.subList(maxErrorsPerComic, comicErrors.size()).clear();
        }

        saveErrors();
        log.debug("Recorded error for comic {}: {}", error.getComicName(), error.getErrorMessage());
    }

    @Override
    public void clearErrors(String comicName) {
        Map<String, List<ComicErrorRecord>> errors = loadErrors();

        if (errors.remove(comicName) != null) {
            saveErrors();
            log.debug("Cleared errors for comic {}", comicName);
        }
    }

    @Override
    public List<ComicErrorRecord> getErrors(String comicName) {
        Map<String, List<ComicErrorRecord>> errors = loadErrors();
        return errors.getOrDefault(comicName, List.of());
    }

    @Override
    public Map<String, List<ComicErrorRecord>> getAllErrors() {
        Map<String, List<ComicErrorRecord>> errors = loadErrors();
        // Sort each list by timestamp descending (newest first)
        return errors.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .sorted(Comparator.comparing(ComicErrorRecord::getTimestamp).reversed())
                                .collect(Collectors.toList())));
    }

    @Override
    public int getComicErrorCount() {
        Map<String, List<ComicErrorRecord>> errors = loadErrors();
        return errors.size();
    }

    @Override
    public void clearOldErrors(int hoursToKeep) {
        Map<String, List<ComicErrorRecord>> errors = loadErrors();
        java.time.OffsetDateTime cutoff = java.time.OffsetDateTime.now().minusHours(hoursToKeep);

        boolean modified = false;
        for (Map.Entry<String, List<ComicErrorRecord>> entry : errors.entrySet()) {
            List<ComicErrorRecord> comicErrors = entry.getValue();
            int originalSize = comicErrors.size();

            // Remove errors older than cutoff
            comicErrors.removeIf(error -> {
                try {
                    // timestamp is already an OffsetDateTime, no parsing needed
                    java.time.OffsetDateTime errorTime = error.getTimestamp();
                    return errorTime != null && errorTime.isBefore(cutoff);
                } catch (Exception e) {
                    log.warn("Error checking timestamp for comic {}: {}", entry.getKey(), e.getMessage());
                    return false; // Keep errors if timestamp check fails
                }
            });

            if (comicErrors.size() < originalSize) {
                modified = true;
                log.debug("Cleared {} old errors for comic {}",
                        originalSize - comicErrors.size(), entry.getKey());
            }
        }

        // Remove empty entries
        errors.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        if (modified) {
            saveErrors();
            log.info("Cleared old errors (keeping last {} hours)", hoursToKeep);
        }
    }

    /**
     * Reset the error cache (for testing purposes)
     */
    @com.google.common.annotations.VisibleForTesting
    public synchronized void resetErrors() {
        errorCache = new HashMap<>();
        saveErrors();
    }
}
