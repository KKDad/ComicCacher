package org.stapledon.config;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;


public class JsonConfigLoader {

    public ComicCacherConfig load()
    {
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("ComicCacher.json");

        return load(inputStream);
    }

    public ComicCacherConfig load(InputStream inputStream)
    {
        Gson gson = new Gson();
        Reader reader = new InputStreamReader(inputStream);
        return gson.fromJson(reader, ComicCacherConfig.class);
    }
}
