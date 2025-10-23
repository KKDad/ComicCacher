package org.stapledon.common.infrastructure.config;

import org.stapledon.common.dto.ImageCacheStats;

/**
 * Interface for writing statistics to storage.
 * Used by metrics collectors to persist computed statistics.
 */
public interface StatsWriter {
    /**
     * Save ImageCacheStats to the root of an image folder
     *
     * @param stats           Statistics to save
     * @param targetDirectory Location to save them to
     * @return True if successfully written
     */
    boolean save(ImageCacheStats stats, String targetDirectory);
}
