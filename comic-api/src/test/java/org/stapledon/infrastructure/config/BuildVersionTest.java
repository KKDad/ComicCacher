package org.stapledon.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

class BuildVersionTest {

    @Test
    void getBuildPropertyTest() {
        BuildVersion version = new BuildVersion();

        var props = Arrays.asList("build.artifact", "build.group", "build.name", "build.time", "build.version");

        props.forEach(prop -> {
            String buildProperty = version.getBuildProperty(prop);
            assertThat(buildProperty).isNotNull();
        });
    }

    @Test
    void logTest() {
        BuildVersion version = new BuildVersion();
        assertThatNoException().isThrownBy(version::logProperties);
    }


}
