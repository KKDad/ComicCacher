package org.stapledon.common.service;

import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.util.Bootstrap;

import java.io.File;

/**
 * Service interface for comic and bootstrap configuration operations.
 * This interface contains ONLY comic-related config (no user/preference).
 * Used by comic-engine to access configuration without depending on ComicAPI.
 */
public interface ComicConfigurationService {
    /**
     * Loads the comic configuration
     * @return The comic configuration
     */
    ComicConfig loadComicConfig();

    /**
     * Saves the comic configuration
     * @param config The configuration to save
     * @return true if successful
     */
    boolean saveComicConfig(ComicConfig config);

    /**
     * Loads the bootstrap configuration
     * @return The bootstrap configuration
     */
    Bootstrap loadBootstrapConfig();

    /**
     * Saves the bootstrap configuration
     * @param config The bootstrap configuration to save
     * @return true if successful
     */
    boolean saveBootstrapConfig(Bootstrap config);

    /**
     * Gets the path for a configuration file
     * @param configName The name of the configuration
     * @return The path to the configuration file
     */
    String getConfigPath(String configName);

    /**
     * Checks if a configuration file exists
     * @param configName The name of the configuration
     * @return true if the configuration exists
     */
    boolean configExists(String configName);

    /**
     * Gets the File object for a configuration
     * @param configName The name of the configuration
     * @return The File object
     */
    File getConfigFile(String configName);
}
