package org.stapledon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.stapledon.dto.ComicConfig;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageCacheStats;

import java.io.*;

@Slf4j
public class JsonConfigWriter
{
    private final Gson gson;
    private final String configPath;
    private ComicConfig comics;

    public JsonConfigWriter(String path)
    {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configPath = path;
    }

    public void save(ComicItem item)
    {
        try {
            loadComics();
            comics.items.put(item.name.hashCode(), item);
            log.info("Saving: {}, Total comics: {}", item.name, comics.items.entrySet().size());

            Writer writer = new FileWriter(configPath);
            gson.toJson(comics, writer);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public ComicItem fetch(String name)
    {
        try {
            loadComics();
            log.info("Fetching {}", name);

            if (this.comics.items.containsKey(name.hashCode()))
                return this.comics.items.get(name.hashCode());
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    /**
     * Load any previously saved configuration
     */
    public ComicConfig loadComics() throws FileNotFoundException
    {
        if (comics != null && !comics.items.isEmpty())
            return comics;

        var initialFile = new File(configPath);
        if (initialFile.exists()) {
            InputStream inputStream = new FileInputStream(initialFile);
            Reader reader = new InputStreamReader(inputStream);

            comics = gson.fromJson(reader, ComicConfig.class);
            log.info("Loaded {} comics from {}, ", comics.items.entrySet().size(), configPath);
        } else {
            log.warn("{} does not exist, creating", configPath);
            comics = new ComicConfig();
        }
        return comics;
    }

    /**
     * Save ImageCacheStats Stats to the root of a Image folder
     * @param ic Statistics to save
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
