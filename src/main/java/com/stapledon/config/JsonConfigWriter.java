package com.stapledon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stapledon.interop.ComicItem;
import com.stapledon.interop.Comics;
import org.apache.log4j.Logger;

import java.io.*;

class JsonConfigWriter
{
    private final Logger logger = Logger.getLogger(JsonConfigWriter.class);


    void save(ComicItem item, String path) throws IOException
    {

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        Comics comics;

        File initialFile = new File(path);
        if (initialFile.exists()) {
            InputStream inputStream = new FileInputStream(initialFile);
            Reader reader = new InputStreamReader(inputStream);

            comics = gson.fromJson(reader, Comics.class);
        } else {
            logger.warn(String.format("%s does not exist, creating", path));
            comics = new Comics();

        }
        comics.items.add(item);
        Writer writer = new FileWriter(path);
        gson.toJson(comics, writer);
        writer.flush();
        writer.close();
    }

}
