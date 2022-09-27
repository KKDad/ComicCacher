package org.stapledon.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CacheConfiguration.class)
@TestPropertySource(properties = {
        "comics.cache.location=testValue",
        "comics.config=myfancyconfigname.json"
})
@ComponentScan(basePackages = {"org.stapledon"})
class CacheConfigurationTest {

    @Autowired
    CacheConfiguration subject;

    @Test
    void cacheLocation() {
        assertThat(subject.cacheLocation()).isEqualTo("testValue");
        assertThat(subject.configName()).isEqualTo("myfancyconfigname.json");
    }
}