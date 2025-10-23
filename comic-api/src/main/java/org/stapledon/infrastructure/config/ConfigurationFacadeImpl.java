package org.stapledon.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.user.UserConfig;
import org.stapledon.common.util.Bootstrap;
import org.stapledon.common.config.CacheProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Paths;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the Configuration Facade that centralizes all configuration handling.
 * This facade handles loading and saving configuration data for comics, users, and preferences.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigurationFacadeImpl implements ApplicationConfigurationFacade {
    
    @Qualifier("gsonWithLocalDate")
    private final Gson gson;
    private final CacheProperties cacheProperties;
    
    private ComicConfig comicConfig;
    private UserConfig userConfig;
    private PreferenceConfig preferenceConfig;
    private Bootstrap bootstrapConfig;
    
    @Override
    public ComicConfig loadComicConfig() {
        if (comicConfig != null && comicConfig.getItems() != null && !comicConfig.getItems().isEmpty()) {
            return comicConfig;
        }
        
        File configFile = getConfigFile(cacheProperties.getConfig());
        
        if (!configFile.exists()) {
            log.warn("{} does not exist, creating new comic configuration", configFile);
            comicConfig = new ComicConfig();
            return comicConfig;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            comicConfig = gson.fromJson(reader, ComicConfig.class);
            
            if (comicConfig == null) {
                log.warn("Null comic configuration from {}, creating new one", configFile);
                comicConfig = new ComicConfig();
            } else if (comicConfig.getItems() == null) {
                log.warn("Null items map in comic configuration from {}, initializing", configFile);
                comicConfig.setItems(new java.util.concurrent.ConcurrentHashMap<>());
            }
            
            log.info("Loaded {} comics from {}", comicConfig.getItems().size(), configFile);
            return comicConfig;
        } catch (JsonParseException e) {
            log.error("Malformed JSON in {}: {}", configFile, e.getMessage());
            comicConfig = new ComicConfig();
            return comicConfig;
        } catch (IOException e) {
            log.error("Error reading comic configuration: {}", e.getMessage(), e);
            comicConfig = new ComicConfig();
            return comicConfig;
        }
    }
    
    @Override
    public boolean saveComicConfig(ComicConfig config) {
        File configFile = getConfigFile(cacheProperties.getConfig());
        
        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
            this.comicConfig = config;
            return true;
        } catch (IOException e) {
            log.error("Error saving comic configuration: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Bootstrap loadBootstrapConfig() {
        if (bootstrapConfig != null) {
            return bootstrapConfig;
        }
        
        // Note: This method assumes ComicCacher.json is in the classpath resources
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("ComicCacher.json")) {
            if (is != null) {
                Reader reader = new InputStreamReader(is);
                bootstrapConfig = gson.fromJson(reader, Bootstrap.class);
                log.info("Loaded bootstrap configuration with {} daily comics and {} king comics", 
                        bootstrapConfig.getDailyComics().size(), 
                        bootstrapConfig.getKingComics().size());
                return bootstrapConfig;
            } else {
                log.error("ComicCacher.json not found in classpath");
                bootstrapConfig = Bootstrap.builder().build();
                return bootstrapConfig;
            }
        } catch (IOException e) {
            log.error("Error loading bootstrap configuration: {}", e.getMessage(), e);
            bootstrapConfig = Bootstrap.builder().build();
            return bootstrapConfig;
        }
    }
    
    @Override
    public boolean saveBootstrapConfig(Bootstrap config) {
        // Bootstrap config is typically read-only from resources
        // This implementation would need to be adapted if saving to resources is required
        log.warn("Saving bootstrap configuration is not supported");
        return false;
    }
    
    @Override
    public UserConfig loadUserConfig() {
        if (userConfig != null && userConfig.getUsers() != null) {
            return userConfig;
        }
        
        File configFile = getConfigFile(cacheProperties.getUsersConfig());
        
        if (!configFile.exists()) {
            log.warn("{} does not exist, creating new user configuration", configFile);
            userConfig = new UserConfig();
            return userConfig;
        }
        
        try (InputStream inputStream = new FileInputStream(configFile);
             Reader reader = new InputStreamReader(inputStream)) {
            
            userConfig = gson.fromJson(reader, UserConfig.class);
            
            if (userConfig == null) {
                log.warn("Null user configuration from {}, creating new one", configFile);
                userConfig = new UserConfig();
            } else if (userConfig.getUsers() == null) {
                log.warn("Null users map in user configuration from {}, initializing", configFile);
                userConfig.setUsers(new java.util.concurrent.ConcurrentHashMap<>());
            }
            
            log.info("Loaded {} users from {}", userConfig.getUsers().size(), configFile);
            return userConfig;
        } catch (JsonParseException e) {
            log.error("Malformed JSON in {}: {}", configFile, e.getMessage());
            throw e; // For integration testing, propagate the original exception
        } catch (IOException e) {
            log.error("Error reading user configuration: {}", e.getMessage(), e);
            userConfig = new UserConfig();
            return userConfig;
        }
    }
    
    @Override
    public boolean saveUserConfig(UserConfig config) {
        File configFile = getConfigFile(cacheProperties.getUsersConfig());
        
        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
            this.userConfig = config;
            return true;
        } catch (IOException e) {
            log.error("Error saving user configuration: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public PreferenceConfig loadPreferenceConfig() {
        if (preferenceConfig != null && preferenceConfig.getPreferences() != null) {
            return preferenceConfig;
        }
        
        File configFile = getConfigFile(cacheProperties.getPreferencesConfig());
        
        if (!configFile.exists()) {
            log.warn("{} does not exist, creating new preference configuration", configFile);
            preferenceConfig = new PreferenceConfig();
            return preferenceConfig;
        }
        
        try (InputStream inputStream = new FileInputStream(configFile);
             Reader reader = new InputStreamReader(inputStream)) {
            
            preferenceConfig = gson.fromJson(reader, PreferenceConfig.class);
            
            if (preferenceConfig == null) {
                log.warn("Null preference configuration from {}, creating new one", configFile);
                preferenceConfig = new PreferenceConfig();
            } else if (preferenceConfig.getPreferences() == null) {
                log.warn("Null preferences map in preference configuration from {}, initializing", configFile);
                preferenceConfig.setPreferences(new java.util.concurrent.ConcurrentHashMap<>());
            }
            
            log.info("Loaded {} preferences from {}", preferenceConfig.getPreferences().size(), configFile);
            return preferenceConfig;
        } catch (IOException e) {
            log.error("Error reading preference configuration: {}", e.getMessage(), e);
            preferenceConfig = new PreferenceConfig();
            return preferenceConfig;
        }
    }
    
    @Override
    public boolean savePreferenceConfig(PreferenceConfig config) {
        File configFile = getConfigFile(cacheProperties.getPreferencesConfig());
        
        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
            this.preferenceConfig = config;
            return true;
        } catch (IOException e) {
            log.error("Error saving preference configuration: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String getConfigPath(String configName) {
        return Paths.get(cacheProperties.getLocation(), configName).toString();
    }
    
    @Override
    public boolean configExists(String configName) {
        return getConfigFile(configName).exists();
    }
    
    @Override
    public File getConfigFile(String configName) {
        File parentDir = new File(cacheProperties.getLocation());
        
        // Ensure parent directory exists
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            log.error("Failed to create directory: {}", parentDir);
        }
        
        return Paths.get(cacheProperties.getLocation(), configName).toFile();
    }
}