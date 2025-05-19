package org.stapledon.infrastructure.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.stapledon.StapledonAccountGivens;
import org.stapledon.api.dto.comic.ComicConfig;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.api.dto.comic.ImageDto;
import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.user.UserConfig;
import org.stapledon.common.util.Bootstrap;
import org.stapledon.core.comic.downloader.ComicDownloaderFacade;
import org.stapledon.core.comic.downloader.ComicDownloaderFacadeImpl;
import org.stapledon.core.comic.downloader.ComicDownloaderStrategy;
import org.stapledon.core.comic.dto.ComicDownloadRequest;
import org.stapledon.core.comic.dto.ComicDownloadResult;
import org.stapledon.core.comic.management.ComicManagementFacade;
import org.stapledon.core.comic.management.ComicManagementFacadeImpl;
import org.stapledon.events.CacheMissEvent;
import org.stapledon.infrastructure.config.properties.CacheProperties;
import org.stapledon.infrastructure.config.properties.StartupReconcilerProperties;
import org.stapledon.infrastructure.storage.ComicStorageFacade;
import java.io.InputStream;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(org.stapledon.infrastructure.config.IComicsBootstrap.class, new IComicsBootstrapTypeAdapter())
                .serializeNulls();

        // Register the concrete implementation for the interface
        gsonBuilder.registerTypeAdapter(org.stapledon.infrastructure.config.IntegrationTestBootstrap.class, 
                new IntegrationTestBootstrapTypeAdapter());
                
        return gsonBuilder.create();
    }
    
    /**
     * Bean for IComicsBootstrap implementation
     * This provides a concrete class that can be properly serialized/deserialized
     */
    @Bean
    @Primary
    public IComicsBootstrap testComicsBootstrap() {
        return new IntegrationTestBootstrap();
    }
    
    /**
     * Type adapter for IComicsBootstrap interface to solve serialization/deserialization issues
     */
    static class IComicsBootstrapTypeAdapter implements com.google.gson.JsonSerializer<IComicsBootstrap>,
            com.google.gson.JsonDeserializer<IComicsBootstrap> {
        
        @Override
        public IComicsBootstrap deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT,
                com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            // Always deserialize to our concrete implementation
            IntegrationTestBootstrap result = new IntegrationTestBootstrap();
            
            if (json.isJsonObject()) {
                com.google.gson.JsonObject obj = json.getAsJsonObject();
                
                if (obj.has("stripName")) {
                    result.setStripName(obj.get("stripName").getAsString());
                }
                
                if (obj.has("startDate")) {
                    try {
                        LocalDate date = context.deserialize(obj.get("startDate"), LocalDate.class);
                        result.setStartDate(date);
                    } catch (Exception e) {
                        // Default to current implementation if date can't be parsed
                    }
                }
                
                if (obj.has("source")) {
                    result.setSource(obj.get("source").getAsString());
                }
                
                if (obj.has("sourceIdentifier")) {
                    result.setSourceIdentifier(obj.get("sourceIdentifier").getAsString());
                }
            }
            
            return result;
        }
        
        @Override
        public com.google.gson.JsonElement serialize(IComicsBootstrap src, java.lang.reflect.Type typeOfSrc,
                com.google.gson.JsonSerializationContext context) {
            // Create a JSON object with all the required fields
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("stripName", src.stripName());
            json.add("startDate", context.serialize(src.startDate()));
            json.addProperty("source", src.getSource());
            json.addProperty("sourceIdentifier", src.getSourceIdentifier());
            return json;
        }
    }
    
    /**
     * Type adapter for IntegrationTestBootstrap to handle serialization/deserialization
     */
    static class IntegrationTestBootstrapTypeAdapter implements com.google.gson.JsonSerializer<IntegrationTestBootstrap>,
            com.google.gson.JsonDeserializer<IntegrationTestBootstrap> {
        
        @Override
        public IntegrationTestBootstrap deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT,
                com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            IntegrationTestBootstrap result = new IntegrationTestBootstrap();
            
            if (json.isJsonObject()) {
                com.google.gson.JsonObject obj = json.getAsJsonObject();
                
                if (obj.has("stripName")) {
                    result.setStripName(obj.get("stripName").getAsString());
                }
                
                if (obj.has("startDate")) {
                    try {
                        LocalDate date = context.deserialize(obj.get("startDate"), LocalDate.class);
                        result.setStartDate(date);
                    } catch (Exception e) {
                        // Default to current implementation if date can't be parsed
                    }
                }
                
                if (obj.has("source")) {
                    result.setSource(obj.get("source").getAsString());
                }
                
                if (obj.has("sourceIdentifier")) {
                    result.setSourceIdentifier(obj.get("sourceIdentifier").getAsString());
                }
            }
            
            return result;
        }
        
        @Override
        public com.google.gson.JsonElement serialize(IntegrationTestBootstrap src, java.lang.reflect.Type typeOfSrc,
                com.google.gson.JsonSerializationContext context) {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("stripName", src.stripName());
            json.add("startDate", context.serialize(src.startDate()));
            json.addProperty("source", src.getSource());
            json.addProperty("sourceIdentifier", src.getSourceIdentifier());
            return json;
        }
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
     * Create a test AccountGivens for integration tests
     */
    @Bean
    @Primary
    public StapledonAccountGivens accountGivens() {
        return new StapledonAccountGivens();
    }

    /**
     * Create a test ComicStorageFacade for integration tests
     */
    @Bean
    @Primary
    public ComicStorageFacade comicStorageFacade(CacheProperties cacheProperties) {
        return new TestComicStorageFacade(cacheProperties);
    }

    /**
     * Create a test ComicDownloaderFacade for integration tests
     */
    @Bean
    @Primary
    public ComicDownloaderFacade comicDownloaderFacade() {
        ComicDownloaderFacadeImpl facade = new ComicDownloaderFacadeImpl();
        
        // Register test downloaders
        facade.registerDownloaderStrategy("gocomics", new TestComicDownloaderStrategy());
        facade.registerDownloaderStrategy("comicskingdom", new TestComicDownloaderStrategy());
        
        return facade;
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
        
        // Configure reconciler properties for tests
        reconcilerProperties.setEnabled(false);
        reconcilerProperties.setScheduleTime("04:00");
        
        return new ComicManagementFacadeImpl(
                storageFacade,
                configFacade,
                downloaderFacade,
                reconcilerProperties,
                taskExecutionTracker
        );
    }
    
    /**
     * Override the CacherConfigLoader bean for integration tests
     * This prevents the JsonIOException with IComicsBootstrap
     */
    @Bean
    @Primary
    public CacherConfigLoader cacherConfigLoader(Gson gson) {
        return new CacherConfigLoader(gson) {
            @Override
            protected Bootstrap load(InputStream inputStream) {
                // Return a test bootstrap instead of trying to deserialize from inputStream
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.setDailyComics(new java.util.ArrayList<>());
                bootstrap.setKingComics(new java.util.ArrayList<>());
                return bootstrap;
            }
        };
    }

    /**
     * Adapter for LocalDate serialization/deserialization
     */
    static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(final JsonWriter jsonWriter, final LocalDate localDate) throws IOException {
            if (localDate == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(localDate.toString());
            }
        }

        @Override
        public LocalDate read(final JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
                int year = 0;
                int month = 0;
                int day = 0;
                jsonReader.beginObject();
                while (jsonReader.peek() != JsonToken.END_OBJECT) {
                    var name = jsonReader.nextName();
                    switch (name) {
                        case "year":
                            year = jsonReader.nextInt();
                            break;
                        case "month":
                            month = jsonReader.nextInt();
                            break;
                        case "day":
                            day = jsonReader.nextInt();
                            break;
                        default:
                            throw new IllegalArgumentException(String.format("Unexpected name=%s", name));
                    }
                }
                jsonReader.endObject();
                return LocalDate.of(year, month, day);
            } else if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            } else {
                return LocalDate.parse(jsonReader.nextString());
            }
        }
    }

    /**
     * Adapter for LocalDateTime serialization/deserialization
     */
    static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(JsonWriter jsonWriter, LocalDateTime localDateTime) throws IOException {
            if (localDateTime == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(formatter.format(localDateTime));
            }
        }

        @Override
        public LocalDateTime read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }

            String dateTimeStr = jsonReader.nextString();
            return LocalDateTime.parse(dateTimeStr, formatter);
        }
    }

    /**
     * Special test implementation of ConfigurationFacade for integration tests
     * Uses in-memory maps to avoid file I/O issues
     */
    private static class TestConfigurationFacade extends ConfigurationFacadeImpl {
        private final Map<String, Object> configStore = new ConcurrentHashMap<>();
        
        public TestConfigurationFacade(Gson gson, CacheProperties properties, File configRoot) {
            super(gson, properties);
            // Initialize empty configurations for integration tests
            initializeTestConfigs();
        }

        private void initializeTestConfigs() {
            try {
                // Create test comic with test image
                ComicConfig comicConfig = new ComicConfig();
                ComicItem testComic = ComicItem.builder()
                        .id(1)
                        .name("Test Comic")
                        .author("Test Author")
                        .description("Integration Test Comic")
                        .newest(LocalDate.now())
                        .oldest(LocalDate.now().minusDays(30))
                        .enabled(true)
                        .avatarAvailable(true)
                        .source("gocomics")
                        .sourceIdentifier("testcomic")
                        .build();
                
                Map<Integer, ComicItem> items = new ConcurrentHashMap<>();
                items.put(testComic.getId(), testComic);
                comicConfig.setItems(items);
                configStore.put("comic", comicConfig);
                
                // Create empty user config
                UserConfig userConfig = new UserConfig();
                userConfig.setUsers(new HashMap<>());
                configStore.put("user", userConfig);
                
                // Create empty preference config
                PreferenceConfig preferenceConfig = new PreferenceConfig();
                preferenceConfig.setPreferences(new HashMap<>());
                configStore.put("preference", preferenceConfig);
                
                // Create bootstrap config
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.setDailyComics(new ArrayList<>());
                bootstrap.setKingComics(new ArrayList<>());
                configStore.put("bootstrap", bootstrap);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize test configurations", e);
            }
        }
        
        @Override
        public ComicConfig loadComicConfig() {
            return (ComicConfig) configStore.get("comic");
        }
        
        @Override
        public boolean saveComicConfig(ComicConfig config) {
            configStore.put("comic", config);
            return true;
        }
        
        @Override
        public UserConfig loadUserConfig() {
            return (UserConfig) configStore.get("user");
        }
        
        @Override
        public boolean saveUserConfig(UserConfig config) {
            configStore.put("user", config);
            return true;
        }
        
        @Override
        public PreferenceConfig loadPreferenceConfig() {
            return (PreferenceConfig) configStore.get("preference");
        }
        
        @Override
        public boolean savePreferenceConfig(PreferenceConfig config) {
            configStore.put("preference", config);
            return true;
        }
        
        @Override
        public Bootstrap loadBootstrapConfig() {
            return (Bootstrap) configStore.get("bootstrap");
        }
        
        @Override
        public boolean saveBootstrapConfig(Bootstrap config) {
            configStore.put("bootstrap", config);
            return true;
        }
    }
    
    /**
     * Test implementation of ComicStorageFacade for integration tests
     */
    private static class TestComicStorageFacade implements ComicStorageFacade {
        private final Map<String, byte[]> comicStore = new ConcurrentHashMap<>();
        private final Map<String, LocalDate> dateStore = new ConcurrentHashMap<>();
        private final List<Consumer<CacheMissEvent>> cacheMissListeners = new ArrayList<>();
        private final CacheProperties properties;
        
        public TestComicStorageFacade(CacheProperties properties) {
            this.properties = properties;
            // Initialize with a test comic
            saveComicStrip(1, "Test Comic", LocalDate.now(), "test image data".getBytes());
            saveAvatar(1, "Test Comic", "test avatar data".getBytes());
        }
        
        @Override
        public boolean saveComicStrip(int comicId, String comicName, LocalDate date, byte[] imageData) {
            String key = String.format("%d_%s_%s", comicId, comicName, date);
            comicStore.put(key, imageData);
            dateStore.put(String.format("%d_%s", comicId, comicName), date);
            return true;
        }
        
        @Override
        public Optional<ImageDto> getComicStrip(int comicId, String comicName, LocalDate date) {
            String key = String.format("%d_%s_%s", comicId, comicName, date);
            byte[] data = comicStore.get(key);
            
            if (data == null) {
                // Trigger cache miss event
                notifyCacheMiss(comicId, comicName, date);
                return Optional.empty();
            }
            
            return Optional.of(ImageDto.builder()
                    .imageData("data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(data))
                    .mimeType("image/png")
                    .width(600)
                    .height(400)
                    .imageDate(date)
                    .build());
        }
        
        @Override
        public Optional<ImageDto> getAvatar(int comicId, String comicName) {
            String key = String.format("%d_%s_avatar", comicId, comicName);
            byte[] data = comicStore.get(key);
            
            if (data == null) {
                return Optional.empty();
            }
            
            return Optional.of(ImageDto.builder()
                    .imageData("data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(data))
                    .mimeType("image/png")
                    .width(200)
                    .height(200)
                    .build());
        }
        
        @Override
        public boolean saveAvatar(int comicId, String comicName, byte[] imageData) {
            String key = String.format("%d_%s_avatar", comicId, comicName);
            comicStore.put(key, imageData);
            return true;
        }
        
        @Override
        public Optional<LocalDate> getNewestDateWithComic(int comicId, String comicName) {
            String key = String.format("%d_%s", comicId, comicName);
            return Optional.ofNullable(dateStore.get(key));
        }
        
        @Override
        public Optional<LocalDate> getOldestDateWithComic(int comicId, String comicName) {
            String key = String.format("%d_%s", comicId, comicName);
            return Optional.ofNullable(dateStore.get(key));
        }
        
        @Override
        public Optional<LocalDate> getNextDateWithComic(int comicId, String comicName, LocalDate from) {
            // For testing, just return the next day
            return Optional.of(from.plusDays(1));
        }
        
        @Override
        public Optional<LocalDate> getPreviousDateWithComic(int comicId, String comicName, LocalDate from) {
            // For testing, just return the previous day
            return Optional.of(from.minusDays(1));
        }
        
        @Override
        public boolean deleteComic(int comicId, String comicName) {
            // Remove all entries for this comic
            comicStore.keySet().removeIf(key -> key.startsWith(comicId + "_" + comicName));
            dateStore.remove(String.format("%d_%s", comicId, comicName));
            return true;
        }
        
        @Override
        public boolean purgeOldImages(int comicId, String comicName, int daysToKeep) {
            // No-op for testing
            return true;
        }
        
        @Override
        public void addCacheMissListener(Consumer<CacheMissEvent> listener) {
            cacheMissListeners.add(listener);
        }
        
        @Override
        public boolean comicStripExists(int comicId, String comicName, LocalDate date) {
            String key = String.format("%d_%s_%s", comicId, comicName, date);
            return comicStore.containsKey(key);
        }
        
        @Override
        public File getCacheRoot() {
            return new File(properties.getLocation());
        }
        
        @Override
        public String getComicCacheRoot(int comicId, String comicName) {
            return properties.getLocation() + "/" + comicName.replace(" ", "");
        }
        
        @Override
        public List<String> getYearsWithContent(int comicId, String comicName) {
            return List.of("2023", "2024", "2025");
        }
        
        @Override
        public long getStorageSize(int comicId, String comicName) {
            return 1024 * 1024; // 1MB for testing
        }
        
        private void notifyCacheMiss(int comicId, String comicName, LocalDate date) {
            CacheMissEvent event = new CacheMissEvent(comicId, comicName, date);
            for (Consumer<CacheMissEvent> listener : cacheMissListeners) {
                listener.accept(event);
            }
        }
    }
    
    /**
     * Test implementation of ComicDownloaderStrategy for integration tests
     */
    private static class TestComicDownloaderStrategy implements ComicDownloaderStrategy {
        @Override
        public String getSource() {
            return "test";
        }
        
        @Override
        public ComicDownloadResult downloadComic(ComicDownloadRequest request) {
            // Always succeed with test data
            return ComicDownloadResult.builder()
                    .request(request)
                    .successful(true)
                    .imageData("test comic image data".getBytes())
                    .build();
        }
        
        @Override
        public Optional<byte[]> downloadAvatar(int comicId, String comicName, String sourceIdentifier) {
            // Always succeed with test data
            return Optional.of("test avatar data".getBytes());
        }
    }
}