package org.stapledon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.LoggerFactory;
import org.stapledon.dto.ComicConfig;
import org.stapledon.dto.ComicItem;

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
            logger.info("Saving {}", item.name);


            comics.items.put(item.name.hashCode(), item);

            saveComics();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void saveComics() throws IOException {
        Writer writer = new FileWriter(configPath);
        gson.toJson(comics, writer);
        writer.flush();
        writer.close();
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
    private void loadComics() throws FileNotFoundException
    {
        if (comics != null)
            return;

        File initialFile = new File(configPath);
        if (initialFile.exists()) {
            InputStream inputStream = new FileInputStream(initialFile);
            Reader reader = new InputStreamReader(inputStream);

            comics = gson.fromJson(reader, ComicConfig.class);
            logger.info("Loaded {}", configPath);
        } else {
            logger.warn("{} does not exist, creating", configPath);
            comics = new ComicConfig();
        }
    }
}
