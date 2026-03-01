package org.stapledon.engine.storage;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;


import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.util.NfsFileOperations;

/**
 * Repository for managing image metadata sidecar files.
 * Metadata is stored as JSON files with the same name as the image but with
 * .json extension.
 * Example: 2023-01-15.png -> 2023-01-15.json
 */
@Slf4j
@ToString
@Repository
@RequiredArgsConstructor
public class ImageMetadataRepository {
    @Qualifier("gsonWithLocalDate")
    private final Gson gson;

    /**
     * Saves metadata for an image file as a sidecar JSON file using atomic write.
     * Only saves if metadata is valid (not empty/unknown).
     */
    public boolean saveMetadata(ImageMetadata metadata) {
        // Don't save invalid/empty metadata
        if (!isValidMetadata(metadata)) {
            log.warn("Refusing to save invalid metadata for {}: format={}, colorMode={}, dimensions={}x{}",
                    metadata.getFilePath(), metadata.getFormat(), metadata.getColorMode(),
                    metadata.getWidth(), metadata.getHeight());
            return false;
        }

        Path metadataFile = getMetadataFile(metadata.getFilePath());

        try {
            String json = gson.toJson(metadata);
            NfsFileOperations.atomicWrite(metadataFile, json);
            log.debug("Saved metadata for image: {}", metadata.getFilePath());
            return true;
        } catch (IOException e) {
            log.error("Failed to save metadata for {}: {}", metadata.getFilePath(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates that metadata contains useful information.
     * Metadata is considered invalid if:
     * - Format is UNKNOWN
     * - ColorMode is UNKNOWN
     * - Dimensions are 0 or negative
     * - File size is 0
     *
     * @param metadata The metadata to validate
     * @return true if metadata is valid, false otherwise
     */
    private boolean isValidMetadata(ImageMetadata metadata) {
        if (metadata == null) {
            return false;
        }

        // Must have a valid format
        if (metadata.getFormat() == null || metadata.getFormat() == org.stapledon.common.dto.ImageFormat.UNKNOWN) {
            return false;
        }

        // Must have valid dimensions
        if (metadata.getWidth() <= 0 || metadata.getHeight() <= 0) {
            return false;
        }

        // Must have a file size
        if (metadata.getSizeInBytes() <= 0) {
            return false;
        }

        // ColorMode UNKNOWN is acceptable - we tried but couldn't determine
        // (better to save partial metadata than none at all)

        return true;
    }

    /**
     * Loads metadata for an image file from its sidecar JSON file.
     */
    public Optional<ImageMetadata> loadMetadata(String imageFilePath) {
        Path metadataFile = getMetadataFile(imageFilePath);

        if (!NfsFileOperations.exists(metadataFile)) {
            return Optional.empty();
        }

        try (Reader reader = Files.newBufferedReader(metadataFile)) {
            ImageMetadata metadata = gson.fromJson(reader, ImageMetadata.class);
            return Optional.ofNullable(metadata);
        } catch (IOException e) {
            log.error("Failed to load metadata for {}: {}", imageFilePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Checks if metadata exists for an image file.
     */
    public boolean metadataExists(String imageFilePath) {
        Path metadataFile = getMetadataFile(imageFilePath);
        return NfsFileOperations.exists(metadataFile);
    }

    /**
     * Deletes metadata for an image file.
     */
    public boolean deleteMetadata(String imageFilePath) {
        Path metadataFile = getMetadataFile(imageFilePath);

        if (!NfsFileOperations.exists(metadataFile)) {
            return true; // Already deleted
        }

        try {
            Files.delete(metadataFile);
            log.debug("Deleted metadata for image: {}", imageFilePath);
            return true;
        } catch (IOException e) {
            log.warn("Failed to delete metadata for image: {}", imageFilePath);
            return false;
        }
    }

    /**
     * Gets the metadata file path for an image file.
     * Replaces the image extension with .json
     */
    private Path getMetadataFile(String imageFilePath) {
        // Remove the image extension and add .json
        String metadataPath = imageFilePath.replaceAll("\\.(png|jpg|jpeg|gif|tif|tiff|bmp|webp)$", ".json");

        // If no extension was found, just append .json
        if (metadataPath.equals(imageFilePath)) {
            metadataPath = imageFilePath + ".json";
        }

        return Path.of(metadataPath);
    }
}
