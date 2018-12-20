package org.stapledon.config;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;


public class YamlConfigLoader {

    public ComicCacherConfig load()
    {
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("ComicCacher.yaml");

        return load(inputStream);
    }

    public ComicCacherConfig load(InputStream inputStream)
    {

        Constructor constructor = new Constructor(ComicCacherConfig.class);
        TypeDescription customTypeDescription = new TypeDescription(ComicCacherConfig.class);
        customTypeDescription.addPropertyParameters("dailyComics", DailyComicConfig.class);
        constructor.addTypeDescription(customTypeDescription);
        Yaml yaml = new Yaml(constructor);

        return yaml.load(inputStream);
    }
}
