package org.stapledon.infrastructure.config;

import com.google.gson.Gson;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.stapledon.core.comic.downloader.ComicDownloaderFacade;
import org.stapledon.core.comic.downloader.ComicDownloaderFacadeImpl;
import org.stapledon.core.comic.management.ComicManagementFacade;
import org.stapledon.core.comic.management.ComicManagementFacadeImpl;
import org.stapledon.infrastructure.config.properties.CacheProperties;
import org.stapledon.infrastructure.config.properties.StartupReconcilerProperties;
import org.stapledon.infrastructure.storage.ComicStorageFacade;
import org.stapledon.infrastructure.storage.ComicStorageFacadeImpl;

import java.io.File;

/**
 * Integration test specific configuration
 * Provides real implementations of facades with test-specific settings
 */
@TestConfiguration
@Profile("integration")
public class IntegrationTestConfig {

    /**
     * Create a custom test Gson instance that handles serialization/deserialization properly
     * This avoids the JsonIOException in integration tests
     */
    @Bean
    @Primary
    public Gson gson() {
        return new GsonProvider().gson();
    }

    /**
     * Create a test ConfigurationFacade for integration tests
     */
    @Bean
    @Primary
    public ConfigurationFacade configurationFacade(Gson gson, CacheProperties cacheProperties) {
        File configRoot = new File(cacheProperties.getLocation());
        if (!configRoot.exists()) {
            configRoot.mkdirs();
        }
        return new TestConfigurationFacade(gson, cacheProperties, configRoot);
    }

    /**
     * Create a test ComicStorageFacade for integration tests
     */
    @Bean
    @Primary
    public ComicStorageFacade comicStorageFacade(CacheProperties cacheProperties) {
        return new ComicStorageFacadeImpl(cacheProperties);
    }

    /**
     * Create a test ComicDownloaderFacade for integration tests
     */
    @Bean
    @Primary
    public ComicDownloaderFacade comicDownloaderFacade() {
        return new ComicDownloaderFacadeImpl();
    }

    /**
     * Create a test ComicManagementFacade for integration tests
     */
    @Bean
    @Primary
    public ComicManagementFacade comicManagementFacade(
            ComicStorageFacade storageFacade,
            ConfigurationFacade configFacade,
            ComicDownloaderFacade downloaderFacade,
            StartupReconcilerProperties reconcilerProperties,
            TaskExecutionTracker taskExecutionTracker) {
        return new ComicManagementFacadeImpl(
                storageFacade,
                configFacade,
                downloaderFacade,
                reconcilerProperties,
                taskExecutionTracker
        );
    }

    /**
     * Special test implementation of ConfigurationFacade for integration tests
     * Uses in-memory maps to avoid file I/O issues
     */
    private static class TestConfigurationFacade extends ConfigurationFacadeImpl {
        public TestConfigurationFacade(Gson gson, CacheProperties properties, File configRoot) {
            super(gson, properties);
            // Initialize empty configurations for integration tests
            initializeTestConfigs(configRoot);
        }

        private void initializeTestConfigs(File configRoot) {
            try {
                // Create empty comic config
                saveComicConfig(new org.stapledon.api.dto.comic.ComicConfig());
                
                // Create empty user config
                org.stapledon.api.dto.user.UserConfig userConfig = new org.stapledon.api.dto.user.UserConfig();
                userConfig.setUsers(new java.util.HashMap<>());
                saveUserConfig(userConfig);
                
                // Create empty preference config
                org.stapledon.api.dto.preference.PreferenceConfig preferenceConfig = new org.stapledon.api.dto.preference.PreferenceConfig();
                preferenceConfig.setPreferences(new java.util.HashMap<>());
                savePreferenceConfig(preferenceConfig);
                
                // Create bootstrap config
                org.stapledon.common.util.Bootstrap bootstrap = new org.stapledon.common.util.Bootstrap();
                bootstrap.setDailyComics(new java.util.ArrayList<>());
                bootstrap.setKingComics(new java.util.ArrayList<>());
                saveBootstrapConfig(bootstrap);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize test configurations", e);
            }
        }
    }
}