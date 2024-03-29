package org.stapledon.config;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.config.properties.CacheProperties;
import org.stapledon.dto.ComicConfig;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageCacheStats;

import java.io.*;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonConfigWriter {
    @Qualifier("gsonWithLocalDate")
    private final Gson gson;

    private final CacheProperties cacheProperties;

    private ComicConfig comics;

    public void save(ComicItem item) {
        try {
            loadComics();
            comics.getItems().put(item.getName().hashCode(), item);
            log.info("Saving: {}, Total comics: {}", item.getName(), comics.getItems().entrySet().size());

            Writer writer = new FileWriter(Paths.get(cacheProperties.getLocation(), cacheProperties.getConfig()).toFile());
            gson.toJson(comics, writer);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public ComicItem fetch(String name) {
        try {
            loadComics();
            log.info("Fetching {}", name);

            if (this.comics.getItems().containsKey(name.hashCode()))
                return this.comics.getItems().get(name.hashCode());
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    /**
     * Load any previously saved configuration
     */
    public ComicConfig loadComics() throws FileNotFoundException {
        if (comics != null && !comics.getItems().isEmpty())
            return comics;

        var initialFile = Paths.get(cacheProperties.getLocation(), cacheProperties.getConfig()).toFile();
        if (initialFile.exists()) {
            InputStream inputStream = new FileInputStream(initialFile);
            Reader reader = new InputStreamReader(inputStream);

            comics = gson.fromJson(reader, ComicConfig.class);
            log.info("Loaded {} comics from {}, ", comics.getItems().entrySet().size(), initialFile);
        } else {
            log.warn("{} does not exist, creating", initialFile);
            comics = new ComicConfig();
        }
        return comics;
    }

    /**
     * Save ImageCacheStats Stats to the root of a Image folder
     *
     * @param ic              Statistics to save
     * @param targetDirectory Location to Save them to
     * @return True if successfully written
     */
    public boolean save(ImageCacheStats ic, String targetDirectory) {
        try {
            Writer writer = new FileWriter(targetDirectory + "/stats.db");
            gson.toJson(ic, writer);
            writer.flush();
            writer.close();
            return true;
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
        }
        return false;
    }
}
