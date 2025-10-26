package org.stapledon.infrastructure.config;

import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageCacheStats;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration writer for comic-related data.
 * This implementation now delegates to ApplicationConfigurationFacade for most operations.
 */
@Slf4j
@ToString
@Component
@RequiredArgsConstructor
public class JsonConfigWriter {
    @Qualifier("gsonWithLocalDate")
    private final Gson gson;
    private final CacheProperties cacheProperties;
    private final ConfigurationFacade configurationFacade;

    private ComicConfig comics;

    /**
     * Save a comic item to configuration
     *
     * @param item Comic item to save
     */
    public void save(ComicItem item) {
        try {
            loadComics();
            comics.getItems().put(item.getName().hashCode(), item);
            log.info("Saving: {}, Total comics: {}", item.getName(), comics.getItems().entrySet().size());
            
            configurationFacade.saveComicConfig(comics);
        } catch (Exception e) {
            log.error("Failed to save comic item: {}", e.getMessage(), e);
        }
    }

    /**
     * Fetch a comic item by name
     *
     * @param name Comic name
     * @return Comic item or null if not found
     */
    public ComicItem fetch(String name) {
        try {
            loadComics();
            log.info("Fetching {}", name);

            if (this.comics.getItems().containsKey(name.hashCode()))
                return this.comics.getItems().get(name.hashCode());
        } catch (Exception e) {
            log.error("Failed to fetch comic: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Load comic configuration
     *
     * @return Comic configuration
     */
    public ComicConfig loadComics() {
        if (comics != null && !comics.getItems().isEmpty())
            return comics;

        try {
            comics = configurationFacade.loadComicConfig();
        } catch (Exception e) {
            log.error("Failed to load comics: {}", e.getMessage(), e);
            comics = new ComicConfig();
        }
        return comics;
    }

    /**
     * Save ImageCacheStats to the root of an image folder
     *
     * @param ic              Statistics to save
     * @param targetDirectory Location to Save them to
     * @return True if successfully written
     */
    public boolean save(ImageCacheStats ic, String targetDirectory) {
        try {
            Writer writer = new FileWriter(targetDirectory + "/stats.json");
            gson.toJson(ic, writer);
            writer.flush();
            writer.close();
            return true;
        } catch (IOException ioe) {
            log.error("Failed to save image cache stats: {}", ioe.getMessage(), ioe);
        }
        return false;
    }
}