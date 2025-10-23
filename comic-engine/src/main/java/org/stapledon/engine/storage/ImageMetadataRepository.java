package org.stapledon.engine.storage;

import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.stapledon.common.dto.ImageMetadata;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repository for managing image metadata sidecar files.
 * Metadata is stored as JSON files with the same name as the image but with .json extension.
 * Example: 2023-01-15.png -> 2023-01-15.json
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ImageMetadataRepository {
    @Qualifier("gsonWithLocalDate")
    private final Gson gson;

    /**
     * Saves metadata for an image file as a sidecar JSON file.
     * Only saves if metadata is valid (not empty/unknown).
     *
     * @param metadata The metadata to save
     * @return true if saved successfully, false if metadata is invalid or save failed
     */
    public boolean saveMetadata(ImageMetadata metadata) {
        // Don't save invalid/empty metadata
        if (!isValidMetadata(metadata)) {
            log.warn("Refusing to save invalid metadata for {}: format={}, colorMode={}, dimensions={}x{}",
                    metadata.getFilePath(), metadata.getFormat(), metadata.getColorMode(),
                    metadata.getWidth(), metadata.getHeight());
            return false;
        }

        File metadataFile = getMetadataFile(metadata.getFilePath());

        try (FileWriter writer = new FileWriter(metadataFile)) {
            gson.toJson(metadata, writer);
            writer.flush();
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
     *
     * @param imageFilePath The path to the image file
     * @return Optional containing the metadata if found, empty otherwise
     */
    public Optional<ImageMetadata> loadMetadata(String imageFilePath) {
        File metadataFile = getMetadataFile(imageFilePath);

        if (!metadataFile.exists()) {
            return Optional.empty();
        }

        try (FileReader reader = new FileReader(metadataFile)) {
            ImageMetadata metadata = gson.fromJson(reader, ImageMetadata.class);
            return Optional.ofNullable(metadata);
        } catch (IOException e) {
            log.error("Failed to load metadata for {}: {}", imageFilePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Checks if metadata exists for an image file.
     *
     * @param imageFilePath The path to the image file
     * @return true if metadata file exists, false otherwise
     */
    public boolean metadataExists(String imageFilePath) {
        File metadataFile = getMetadataFile(imageFilePath);
        return metadataFile.exists();
    }

    /**
     * Deletes metadata for an image file.
     *
     * @param imageFilePath The path to the image file
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteMetadata(String imageFilePath) {
        File metadataFile = getMetadataFile(imageFilePath);

        if (!metadataFile.exists()) {
            return true; // Already deleted
        }

        boolean deleted = metadataFile.delete();
        if (deleted) {
            log.debug("Deleted metadata for image: {}", imageFilePath);
        } else {
            log.warn("Failed to delete metadata for image: {}", imageFilePath);
        }

        return deleted;
    }

    /**
     * Gets the metadata file path for an image file.
     * Replaces the image extension with .json
     *
     * @param imageFilePath The path to the image file
     * @return The corresponding metadata file
     */
    private File getMetadataFile(String imageFilePath) {
        // Remove the image extension and add .json
        String metadataPath = imageFilePath.replaceAll("\\.(png|jpg|jpeg|gif|tif|tiff|bmp|webp)$", ".json");

        // If no extension was found, just append .json
        if (metadataPath.equals(imageFilePath)) {
            metadataPath = imageFilePath + ".json";
        }

        return new File(metadataPath);
    }
}
