package org.stapledon.config;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.dto.Bootstrap;

import java.io.InputStream;
import java.io.InputStreamReader;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class CacherConfigLoader {
    @Qualifier("gsonWithLocalDate")
    private final Gson gson;

    /**
     * Load the configuration from the default resources
     *
     * @return CacherBootstrapConfig that was loaded
     */
    @Bean
    public Bootstrap load() {
        log.warn("Loading CacherBootstrapConfig from: {}", this.getClass().getClassLoader().getResource("ComicCacher.json"));
        var inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("ComicCacher.json");

        return load(inputStream);
    }

    /**
     * Load the configuration from the specified stream.
     * Do not make this private, it's used by unit tests.
     *
     * @param inputStream Stream to load
     * @return CacherBootstrapConfig that was loaded
     */
    protected Bootstrap load(InputStream inputStream) {
        var reader = new InputStreamReader(inputStream);
        return gson.fromJson(reader, Bootstrap.class);
    }
}
