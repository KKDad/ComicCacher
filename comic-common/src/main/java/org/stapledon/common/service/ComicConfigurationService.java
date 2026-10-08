package org.stapledon.common.service;

import org.stapledon.common.dto.ComicConfig;

import java.io.File;

/**
 * Service interface for comic configuration operations.
 * This interface contains ONLY comic-related config (no user/preference).
 * Used by comic-engine to access configuration without depending on ComicAPI.
 */
public interface ComicConfigurationService {
    /**
     * Loads the comic configuration.
     * @return The comic configuration
     */
    ComicConfig loadComicConfig();

    /**
     * Saves the comic configuration.
     * @param config The configuration to save
     * @return true if successful
     */
    boolean saveComicConfig(ComicConfig config);

    /**
     * Gets the File object for a configuration.
     */
    File getConfigFile(String configName);
}
