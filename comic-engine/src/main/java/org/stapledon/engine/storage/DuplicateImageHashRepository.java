package org.stapledon.engine.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ImageHashRecord;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository for managing image hash records used in duplicate detection.
 * Hash data is stored per-comic/per-year in JSON files named "image-hashes.json".
 * Example: cache/AdamAtHome/2025/image-hashes.json
 *
 * Uses an in-memory cache for performance - loaded on first access per comic/year.
 */
@Slf4j
@ToString
@Repository
@RequiredArgsConstructor
public class DuplicateImageHashRepository {

    private static final String HASH_FILE_NAME = "image-hashes.json";

    @Qualifier("gsonWithLocalDate")
    private final Gson gson;

    private final CacheProperties cacheProperties;

    /**
     * In-memory cache of loaded hash records: key is "comicId:year", value is hash map.
     */
    private final Map<String, Map<String, ImageHashRecord>> cache = new ConcurrentHashMap<>();

    /**
     * Finds an image hash record by hash value for a specific comic and year.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param year The year to search in
     * @param hash The hash value to find
     * @return Optional containing the matching record, or empty if not found
     */
    public Optional<ImageHashRecord> findByHash(int comicId, String comicName, int year, String hash) {
        Map<String, ImageHashRecord> yearHashes = loadHashes(comicId, comicName, year);
        return Optional.ofNullable(yearHashes.get(hash));
    }

    /**
     * Adds a new hash record for a comic image.
     * Updates both the in-memory cache and the JSON file.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param year The year
     * @param record The hash record to add
     */
    public void addHash(int comicId, String comicName, int year, ImageHashRecord record) {
        Map<String, ImageHashRecord> yearHashes = loadHashes(comicId, comicName, year);

        // Add to cache
        yearHashes.put(record.getHash(), record);

        // Persist to disk
        saveHashes(comicId, comicName, year, yearHashes);
    }

    /**
     * Loads all hash records for a specific comic and year.
     * Returns from cache if available, otherwise loads from disk.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param year The year
     * @return Map of hash value to ImageHashRecord
     */
    public Map<String, ImageHashRecord> loadHashes(int comicId, String comicName, int year) {
        String cacheKey = getCacheKey(comicId, year);

        // Check in-memory cache first
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        // Load from disk
        File hashFile = getHashFile(comicId, comicName, year);
        Map<String, ImageHashRecord> hashes = new ConcurrentHashMap<>();

        if (hashFile.exists()) {
            try (FileReader reader = new FileReader(hashFile)) {
                Type type = new TypeToken<Map<String, ImageHashRecord>>() {}.getType();
                Map<String, ImageHashRecord> loaded = gson.fromJson(reader, type);
                if (loaded != null) {
                    hashes.putAll(loaded);
                }
                log.info("Loaded {} hash records for comic {} year {} from {}", hashes.size(), comicName, year, hashFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to load hash file {}: {}", hashFile.getAbsolutePath(), e.getMessage(), e);
            }
        } else {
            log.info("No existing hash file for comic {} year {}, creating new hash cache", comicName, year);
        }

        // Store in cache
        cache.put(cacheKey, hashes);

        return hashes;
    }

    /**
     * Saves hash records to disk for a specific comic and year.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param year The year
     * @param hashes The hash records to save
     */
    private void saveHashes(int comicId, String comicName, int year, Map<String, ImageHashRecord> hashes) {
        File hashFile = getHashFile(comicId, comicName, year);

        // Ensure parent directory exists
        File parentDir = hashFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            log.error("Failed to create directory: {}", parentDir.getAbsolutePath());
            return;
        }

        try (FileWriter writer = new FileWriter(hashFile)) {
            gson.toJson(hashes, writer);
            writer.flush();
            log.info("Saved {} hash records to {}", hashes.size(), hashFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save hash file {}: {}", hashFile.getAbsolutePath(), e.getMessage(), e);
        }
    }

    /**
     * Gets the File object for the hash JSON file.
     *
     * @param comicId The comic ID
     * @param comicName The comic name
     * @param year The year
     * @return The File object for the hash JSON file
     */
    private File getHashFile(int comicId, String comicName, int year) {
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        String yearPath = String.valueOf(year);
        String cacheRoot = cacheProperties.getLocation();

        return new File(String.format("%s/%s/%s/%s", cacheRoot, comicNameParsed, yearPath, HASH_FILE_NAME));
    }

    /**
     * Gets a directory name for a comic - uses the comic name if available,
     * otherwise falls back to the comic ID.
     *
     * @param comicId The comic ID
     * @param comicName The comic name (can be null)
     * @return A string to use as the directory name
     */
    private String getComicNameParsed(int comicId, String comicName) {
        if (comicName == null) {
            return "comic_" + comicId;
        }
        return comicName.replace(" ", "");
    }

    /**
     * Creates a cache key from comic ID and year.
     *
     * @param comicId The comic ID
     * @param year The year
     * @return The cache key
     */
    private String getCacheKey(int comicId, int year) {
        return comicId + ":" + year;
    }

    /**
     * Clears the in-memory cache.
     * Useful for testing or when you want to force reload from disk.
     */
    public void clearCache() {
        cache.clear();
        log.debug("Cleared hash repository cache");
    }
}
