package org.stapledon.config;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;


public class CacherConfigLoader
{
    private static final Logger logger = LoggerFactory.getLogger(CacherConfigLoader.class);

    /**
     * Load the configuration from the default resources
     * @return CacherBootstrapConfig that was loaded
     */
    public CacherBootstrapConfig load()
    {
        logger.warn("Loading CacherBootstrapConfig from: {}", this.getClass().getClassLoader().getResource("ComicCacher.json"));

        InputStream inputStream = this.getClass()
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
        Gson gson = new Gson();
        Reader reader = new InputStreamReader(inputStream);
        return gson.fromJson(reader, CacherBootstrapConfig.class);
    }
}
