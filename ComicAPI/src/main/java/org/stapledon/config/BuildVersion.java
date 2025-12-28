package org.stapledon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.stapledon.ComicApiApplication;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

@Slf4j
@Component
public class BuildVersion {

    private Properties buildProps;

    public BuildVersion() {
        var url = ComicApiApplication.class.getClassLoader().getResource("META-INF/build-info.properties");
        if (url == null) {
            buildProps = new Properties();
            return;
        }
        var props = new Properties();
        try (var stream = url.openStream()) {
            props.load(stream);
            buildProps = props;
        } catch (IOException e) {
            log.error("Unable to load build properties");
        }
        logProperties();
    }

    public String getBuildProperty(String key) {
        if (buildProps == null) {
            return null;
        }
        return (String) buildProps.getOrDefault(key, null);
    }

    public void logProperties() {
        var props = Arrays.asList("build.artifact", "build.group", "build.name", "build.time", "build.version");
        log.info("*****************************************");
        props.forEach(prop -> {
            String buildProperty = getBuildProperty(prop);
            log.info("{} -> {}", prop, buildProperty);
        });
        log.info("*****************************************");
    }
}