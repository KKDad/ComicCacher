package org.stapledon.common.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import org.stapledon.common.config.properties.DownloaderProperties;


class UserAgentServiceTest {

    private static final String CUSTOM_DEFAULT = "CustomAgent/1.0";
    private static final String GOCOMICS_OVERRIDE = "GoComicsAgent/2.0";

    private static DownloaderProperties propsWithDefault(String defaultValue) {
        return DownloaderProperties.builder()
                .userAgent(DownloaderProperties.UserAgent.builder().defaultValue(defaultValue).build())
                .build();
    }

    private static DownloaderProperties propsWithDefaultAndSources(String defaultValue,
                                                                    Map<String, DownloaderProperties.Source> sources) {
        return DownloaderProperties.builder()
                .userAgent(DownloaderProperties.UserAgent.builder().defaultValue(defaultValue).build())
                .sources(sources)
                .build();
    }

    @Test
    void getDefault_whenNoConfiguration_returnsBuiltinFallback() {
        UserAgentService service = new UserAgentService(DownloaderProperties.builder().build());

        assertThat(service.getDefault()).isEqualTo(UserAgentService.FALLBACK_USER_AGENT);
    }

    @Test
    void getDefault_whenConfigured_returnsConfiguredValue() {
        UserAgentService service = new UserAgentService(propsWithDefault(CUSTOM_DEFAULT));

        assertThat(service.getDefault()).isEqualTo(CUSTOM_DEFAULT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void getUserAgent_whenSourceIsBlank_returnsDefault(String source) {
        UserAgentService service = new UserAgentService(propsWithDefault(CUSTOM_DEFAULT));

        assertThat(service.getUserAgent(source)).isEqualTo(CUSTOM_DEFAULT);
    }

    @Test
    void getUserAgent_whenSourceHasNoOverride_returnsDefault() {
        UserAgentService service = new UserAgentService(propsWithDefault(CUSTOM_DEFAULT));

        assertThat(service.getUserAgent("comicskingdom")).isEqualTo(CUSTOM_DEFAULT);
    }

    @Test
    void getUserAgent_whenSourceHasOverride_returnsOverride() {
        DownloaderProperties.Source gocomics = DownloaderProperties.Source.builder()
                .userAgent(GOCOMICS_OVERRIDE)
                .build();
        UserAgentService service = new UserAgentService(
                propsWithDefaultAndSources(CUSTOM_DEFAULT, Map.of("gocomics", gocomics)));

        assertThat(service.getUserAgent("gocomics")).isEqualTo(GOCOMICS_OVERRIDE);
        assertThat(service.getUserAgent("comicskingdom")).isEqualTo(CUSTOM_DEFAULT);
    }

    @Test
    void getUserAgent_whenSourceOverrideIsBlank_fallsBackToDefault() {
        DownloaderProperties.Source gocomics = DownloaderProperties.Source.builder()
                .userAgent("")
                .build();
        UserAgentService service = new UserAgentService(
                propsWithDefaultAndSources(CUSTOM_DEFAULT, Map.of("gocomics", gocomics)));

        assertThat(service.getUserAgent("gocomics")).isEqualTo(CUSTOM_DEFAULT);
    }
}
