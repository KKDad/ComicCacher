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

    @Test
    void getDefault_whenNoConfiguration_returnsBuiltinFallback() {
        UserAgentService service = new UserAgentService(new DownloaderProperties());

        assertThat(service.getDefault()).isEqualTo(UserAgentService.FALLBACK_USER_AGENT);
    }

    @Test
    void getDefault_whenConfigured_returnsConfiguredValue() {
        DownloaderProperties props = new DownloaderProperties();
        props.getUserAgent().setDefaultValue(CUSTOM_DEFAULT);

        UserAgentService service = new UserAgentService(props);

        assertThat(service.getDefault()).isEqualTo(CUSTOM_DEFAULT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void getUserAgent_whenSourceIsBlank_returnsDefault(String source) {
        DownloaderProperties props = new DownloaderProperties();
        props.getUserAgent().setDefaultValue(CUSTOM_DEFAULT);

        UserAgentService service = new UserAgentService(props);

        assertThat(service.getUserAgent(source)).isEqualTo(CUSTOM_DEFAULT);
    }

    @Test
    void getUserAgent_whenSourceHasNoOverride_returnsDefault() {
        DownloaderProperties props = new DownloaderProperties();
        props.getUserAgent().setDefaultValue(CUSTOM_DEFAULT);

        UserAgentService service = new UserAgentService(props);

        assertThat(service.getUserAgent("comicskingdom")).isEqualTo(CUSTOM_DEFAULT);
    }

    @Test
    void getUserAgent_whenSourceHasOverride_returnsOverride() {
        DownloaderProperties props = new DownloaderProperties();
        props.getUserAgent().setDefaultValue(CUSTOM_DEFAULT);

        DownloaderProperties.Source gocomics = new DownloaderProperties.Source();
        gocomics.setUserAgent(GOCOMICS_OVERRIDE);
        props.setSources(Map.of("gocomics", gocomics));

        UserAgentService service = new UserAgentService(props);

        assertThat(service.getUserAgent("gocomics")).isEqualTo(GOCOMICS_OVERRIDE);
        assertThat(service.getUserAgent("comicskingdom")).isEqualTo(CUSTOM_DEFAULT);
    }

    @Test
    void getUserAgent_whenSourceOverrideIsBlank_fallsBackToDefault() {
        DownloaderProperties props = new DownloaderProperties();
        props.getUserAgent().setDefaultValue(CUSTOM_DEFAULT);

        DownloaderProperties.Source gocomics = new DownloaderProperties.Source();
        gocomics.setUserAgent("");
        props.setSources(Map.of("gocomics", gocomics));

        UserAgentService service = new UserAgentService(props);

        assertThat(service.getUserAgent("gocomics")).isEqualTo(CUSTOM_DEFAULT);
    }
}
