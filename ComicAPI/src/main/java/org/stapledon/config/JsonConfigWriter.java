package org.stapledon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.LoggerFactory;
import org.stapledon.dto.ComicConfig;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageCacheStats;

import java.io.*;

public class JsonConfigWriter
{
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(JsonConfigWriter.class);
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
            logger.info("Saving: {}, Total comics: {}", item.name, comics.items.entrySet().size());

            Writer writer = new FileWriter(configPath);
            gson.toJson(comics, writer);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public ComicItem fetch(String name)
    {
        try {
            loadComics();
            logger.info("Fetching {}", name);

            if (this.comics.items.containsKey(name.hashCode()))
                return this.comics.items.get(name.hashCode());
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
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
            logger.info("Loaded {} comics from {}, ", comics.items.entrySet().size(), configPath);
        } else {
            logger.warn("{} does not exist, creating", configPath);
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
            logger.error(ioe.getMessage(), ioe);
        }
        return false;
    }
}
