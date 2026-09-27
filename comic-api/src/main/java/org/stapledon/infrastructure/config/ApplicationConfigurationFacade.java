package org.stapledon.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.user.UserConfig;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.util.NfsFileOperations;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the Configuration Facade that centralizes all configuration
 * handling.
 * This facade handles loading and saving configuration data for comics, users,
 * and preferences.
 */
@Slf4j
@ToString
@Component
public class ApplicationConfigurationFacade implements ConfigurationFacade {

    private final Gson gson;
    private final CacheProperties cacheProperties;
    private final String cacheLocation;

    private ComicConfig comicConfig;
    private UserConfig userConfig;
    private PreferenceConfig preferenceConfig;

    public ApplicationConfigurationFacade(
            @Qualifier("gsonWithLocalDate") Gson gson,
            CacheProperties cacheProperties,
            @Qualifier("cacheLocation") String cacheLocation) {
        this.gson = gson;
        this.cacheProperties = cacheProperties;
        this.cacheLocation = cacheLocation;
    }

    @Override
    public ComicConfig loadComicConfig() {
        if (comicConfig != null && comicConfig.getItems() != null && !comicConfig.getItems().isEmpty()) {
            return comicConfig;
        }
        ComicConfig loaded = readConfig(cacheProperties.getConfig(), ComicConfig.class, ComicConfig::new, "comic");
        if (loaded.getItems() == null) {
            log.warn("Null items map in comic configuration, initializing");
            loaded.setItems(new java.util.concurrent.ConcurrentHashMap<>());
        }
        log.info("Loaded {} comics from {}", loaded.getItems().size(), getConfigFile(cacheProperties.getConfig()));
        comicConfig = loaded;
        return comicConfig;
    }

    @Override
    public boolean saveComicConfig(ComicConfig config) {
        if (writeConfig(cacheProperties.getConfig(), config, "comic")) {
            this.comicConfig = config;
            return true;
        }
        return false;
    }

    @Override
    public UserConfig loadUserConfig() {
        if (userConfig != null && userConfig.getUsers() != null) {
            return userConfig;
        }
        UserConfig loaded = readConfig(cacheProperties.getUsersConfig(), UserConfig.class, UserConfig::new, "user");
        if (loaded.getUsers() == null) {
            log.warn("Null users map in user configuration, initializing");
            loaded.setUsers(new java.util.concurrent.ConcurrentHashMap<>());
        }
        log.info("Loaded {} users from {}", loaded.getUsers().size(), getConfigFile(cacheProperties.getUsersConfig()));
        userConfig = loaded;
        return userConfig;
    }

    @Override
    public boolean saveUserConfig(UserConfig config) {
        if (writeConfig(cacheProperties.getUsersConfig(), config, "user")) {
            this.userConfig = config;
            return true;
        }
        return false;
    }

    @Override
    public PreferenceConfig loadPreferenceConfig() {
        if (preferenceConfig != null && preferenceConfig.getPreferences() != null) {
            return preferenceConfig;
        }
        PreferenceConfig loaded = readConfig(cacheProperties.getPreferencesConfig(), PreferenceConfig.class, PreferenceConfig::new, "preference");
        if (loaded.getPreferences() == null) {
            log.warn("Null preferences map in preference configuration, initializing");
            loaded.setPreferences(new java.util.concurrent.ConcurrentHashMap<>());
        }
        log.info("Loaded {} preferences from {}", loaded.getPreferences().size(), getConfigFile(cacheProperties.getPreferencesConfig()));
        preferenceConfig = loaded;
        return preferenceConfig;
    }

    @Override
    public boolean savePreferenceConfig(PreferenceConfig config) {
        if (writeConfig(cacheProperties.getPreferencesConfig(), config, "preference")) {
            this.preferenceConfig = config;
            return true;
        }
        return false;
    }

    /**
     * Reads a configuration file. A missing file (or an empty one) gives a new, empty configuration.
     * <p>
     * A file that exists but cannot be read or parsed throws, and nothing is cached. Handing back an empty configuration instead would let the
     * next save overwrite every user, preference or comic in the file.
     */
    private <T> T readConfig(String configName, Class<T> type, Supplier<T> empty, String what) {
        File configFile = getConfigFile(configName);
        if (!configFile.exists()) {
            log.warn("{} does not exist, creating new {} configuration", configFile, what);
            return empty.get();
        }
        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            T loaded = gson.fromJson(reader, type);
            if (loaded == null) {
                log.warn("Empty {} configuration in {}, creating new one", what, configFile);
                return empty.get();
            }
            return loaded;
        } catch (JsonParseException e) {
            log.error("Malformed JSON in {} configuration {}; refusing to continue with an empty one", what, configFile, e);
            throw e;
        } catch (IOException e) {
            log.error("Cannot read {} configuration {}; refusing to continue with an empty one", what, configFile, e);
            throw new UncheckedIOException("Cannot read " + configFile, e);
        }
    }

    /**
     * Writes a configuration file atomically (temp file, then move), so a failed write never leaves a truncated file.
     */
    private boolean writeConfig(String configName, Object config, String what) {
        File configFile = getConfigFile(configName);
        try {
            NfsFileOperations.atomicWrite(configFile.toPath(), gson.toJson(config));
            return true;
        } catch (IOException e) {
            log.error("Error saving {} configuration to {}", what, configFile, e);
            return false;
        }
    }

    @Override
    public File getConfigFile(String configName) {
        File parentDir = new File(cacheLocation);

        // Ensure parent directory exists
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            log.error("Failed to create directory {} (exists: {}, parent writable: {})", parentDir.getAbsolutePath(), parentDir.exists(),
                    parentDir.getAbsoluteFile().getParentFile() != null && parentDir.getAbsoluteFile().getParentFile().canWrite());
        }

        return Paths.get(cacheLocation, configName).toFile();
    }
}