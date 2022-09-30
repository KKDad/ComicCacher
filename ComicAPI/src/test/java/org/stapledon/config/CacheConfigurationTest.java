package org.stapledon.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.stapledon.config.properties.CacheProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheConfigurationTest {

    @Mock
    CacheProperties cacheProperties;

    @InjectMocks
    CacheConfiguration subject;

    @Test
    void cacheLocation() {
        when(cacheProperties.getLocation()).thenReturn("testValue");
        when(cacheProperties.getConfig()).thenReturn("myfancyconfigname.json");

        assertThat(subject.cacheLocation()).isEqualTo("testValue");
        assertThat(subject.configName()).isEqualTo("myfancyconfigname.json");
    }
}