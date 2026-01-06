package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stapledon.common.dto.BackfillSourceConfig;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

class BackfillConfigurationServiceTest {

    private BackfillConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new BackfillConfigurationService();
        service.setDefaultMaxPerDay(50);
        service.setDefaultMaxDaysBack(365);
        service.setMaxConsecutiveFailures(3);
    }

    @Test
    void getMaxPerDayForSource_withNoSourceConfig_returnsDefault() {
        int result = service.getMaxPerDayForSource("unknown-source");

        assertThat(result).isEqualTo(50);
    }

    @Test
    void getMaxPerDayForSource_withSourceConfig_returnsSourceValue() {
        Map<String, BackfillSourceConfig> sources = new HashMap<>();
        sources.put("gocomics", BackfillSourceConfig.builder()
                .source("gocomics")
                .maxPerDay(20)
                .build());
        service.setSources(sources);

        int result = service.getMaxPerDayForSource("gocomics");

        assertThat(result).isEqualTo(20);
    }

    @Test
    void getMaxPerDayForSource_withZeroSourceValue_returnsDefault() {
        Map<String, BackfillSourceConfig> sources = new HashMap<>();
        sources.put("gocomics", BackfillSourceConfig.builder()
                .source("gocomics")
                .maxPerDay(0)
                .build());
        service.setSources(sources);

        int result = service.getMaxPerDayForSource("gocomics");

        assertThat(result).isEqualTo(50);
    }

    @Test
    void getMaxDaysBackForSource_withNoSourceConfig_returnsDefault() {
        int result = service.getMaxDaysBackForSource("unknown-source");

        assertThat(result).isEqualTo(365);
    }

    @Test
    void getMaxDaysBackForSource_withSourceConfig_returnsSourceValue() {
        Map<String, BackfillSourceConfig> sources = new HashMap<>();
        sources.put("comicskingdom", BackfillSourceConfig.builder()
                .source("comicskingdom")
                .maxDaysBack(180)
                .build());
        service.setSources(sources);

        int result = service.getMaxDaysBackForSource("comicskingdom");

        assertThat(result).isEqualTo(180);
    }

    @Test
    void getEarliestAllowedDate_calculatesCorrectly() {
        service.setDefaultMaxDaysBack(30);

        LocalDate result = service.getEarliestAllowedDate("test-source");

        assertThat(result).isEqualTo(LocalDate.now().minusDays(30));
    }

    @Test
    void getEarliestAllowedDate_usesSourceSpecificLimit() {
        Map<String, BackfillSourceConfig> sources = new HashMap<>();
        sources.put("short-history", BackfillSourceConfig.builder()
                .source("short-history")
                .maxDaysBack(7)
                .build());
        service.setSources(sources);

        LocalDate result = service.getEarliestAllowedDate("short-history");

        assertThat(result).isEqualTo(LocalDate.now().minusDays(7));
    }

    @Test
    void isSourceEnabled_withNoConfig_returnsTrue() {
        boolean result = service.isSourceEnabled("unknown-source");

        assertThat(result).isTrue();
    }

    @Test
    void isSourceEnabled_withEnabledSource_returnsTrue() {
        Map<String, BackfillSourceConfig> sources = new HashMap<>();
        sources.put("enabled-source", BackfillSourceConfig.builder()
                .source("enabled-source")
                .enabled(true)
                .build());
        service.setSources(sources);

        boolean result = service.isSourceEnabled("enabled-source");

        assertThat(result).isTrue();
    }

    @Test
    void isSourceEnabled_withDisabledSource_returnsFalse() {
        Map<String, BackfillSourceConfig> sources = new HashMap<>();
        sources.put("disabled-source", BackfillSourceConfig.builder()
                .source("disabled-source")
                .enabled(false)
                .build());
        service.setSources(sources);

        boolean result = service.isSourceEnabled("disabled-source");

        assertThat(result).isFalse();
    }

    @Test
    void isSourceEnabled_withNullEnabled_returnsTrue() {
        Map<String, BackfillSourceConfig> sources = new HashMap<>();
        sources.put("null-enabled", BackfillSourceConfig.builder()
                .source("null-enabled")
                .enabled(null)
                .build());
        service.setSources(sources);

        boolean result = service.isSourceEnabled("null-enabled");

        assertThat(result).isTrue();
    }
}
