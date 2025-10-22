package org.stapledon.infrastructure.config;

import org.stapledon.common.dto.ComicConfig;
import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.user.UserConfig;
import org.stapledon.common.util.Bootstrap;

import java.io.File;

/**
 * Facade interface for all configuration-related operations.
 * This facade centralizes all configuration handling, focusing exclusively on loading 
 * and saving configuration data.
 */
public interface ConfigurationFacade {
    // Comic configuration methods
    ComicConfig loadComicConfig();
    boolean saveComicConfig(ComicConfig config);
    
    // Bootstrap configuration
    Bootstrap loadBootstrapConfig();
    boolean saveBootstrapConfig(Bootstrap config);
    
    // User configuration
    UserConfig loadUserConfig();
    boolean saveUserConfig(UserConfig config);
    
    // Preference configuration
    PreferenceConfig loadPreferenceConfig();
    boolean savePreferenceConfig(PreferenceConfig config);
    
    // Configuration utility methods
    String getConfigPath(String configName);
    boolean configExists(String configName);
    File getConfigFile(String configName);
}