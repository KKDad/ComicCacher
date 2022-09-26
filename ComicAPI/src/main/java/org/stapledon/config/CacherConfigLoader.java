package org.stapledon.config;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;


@Slf4j
public class CacherConfigLoader
{
    /**
     * Load the configuration from the default resources
     * @return CacherBootstrapConfig that was loaded
     */
    public CacherBootstrapConfig load()
    {
        log.warn("Loading CacherBootstrapConfig from: {}", this.getClass().getClassLoader().getResource("ComicCacher.json"));

        var inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("ComicCacher.json");

        return load(inputStream);
    }

    /**
     * Load the configuration from the specified stream.
     * Do not make this private, it's used by unit tests.
     * @param inputStream Stream to load
     * @return CacherBootstrapConfig that was loaded
     */
    CacherBootstrapConfig load(InputStream inputStream)
    {
        var gson = new Gson();
        var reader = new InputStreamReader(inputStream);
        return gson.fromJson(reader, CacherBootstrapConfig.class);
    }
}
