package org.stapledon.config;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;


public class CacherConfigLoader {

    public CacherBootstrapConfig load()
    {
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("ComicCacher.json");

        return load(inputStream);
    }

    CacherBootstrapConfig load(InputStream inputStream)
    {
        Gson gson = new Gson();
        Reader reader = new InputStreamReader(inputStream);
        return gson.fromJson(reader, CacherBootstrapConfig.class);
    }
}
