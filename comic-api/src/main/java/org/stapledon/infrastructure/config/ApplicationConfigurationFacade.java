package org.stapledon.infrastructure.config;

import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.user.UserConfig;
import org.stapledon.common.service.ComicConfigurationService;

/**
 * Full configuration facade including user and preference config.
 * Extends ComicConfigurationService to include API-specific methods.
 *
 * This interface provides the complete set of configuration operations for the API layer,
 * while comic-engine depends only on the base ComicConfigurationService.
 */
public interface ApplicationConfigurationFacade extends ComicConfigurationService {
    /**
     * Loads the user configuration
     * @return The user configuration
     */
    UserConfig loadUserConfig();

    /**
     * Saves the user configuration
     * @param config The user configuration to save
     * @return true if successful
     */
    boolean saveUserConfig(UserConfig config);

    /**
     * Loads the preference configuration
     * @return The preference configuration
     */
    PreferenceConfig loadPreferenceConfig();

    /**
     * Saves the preference configuration
     * @param config The preference configuration to save
     * @return true if successful
     */
    boolean savePreferenceConfig(PreferenceConfig config);
}
