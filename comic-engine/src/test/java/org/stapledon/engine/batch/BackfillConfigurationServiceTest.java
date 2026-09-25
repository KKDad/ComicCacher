package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import org.stapledon.common.dto.BackfillSourceConfig;

class BackfillConfigurationServiceTest {

    private static BackfillConfigurationService.BackfillConfigurationServiceBuilder defaultBuilder() {
        return BackfillConfigurationService.builder()
                .enabled(true)
                .defaultMaxPerDay(50)
                .defaultMaxDaysBack(365)
                .maxConsecutiveFailures(3);
    }

    private static BackfillConfigurationService serviceWithSources(Map<String, BackfillSourceConfig> sources) {
        return defaultBuilder().sources(sources).build();
    }

    @Test
    void getMaxPerDayForSource_withNoSourceConfig_returnsDefault() {
        BackfillConfigurationService service = defaultBuilder().build();

        int result = service.getMaxPerDayForSource("unknown-source");

        assertThat(result).isEqualTo(50);
    }

    @Test
    void getMaxPerDayForSource_withSourceConfig_returnsSourceValue() {
        BackfillConfigurationService service = serviceWithSources(Map.of(
                "gocomics", BackfillSourceConfig.builder().source("gocomics").maxPerDay(20).build()));

        int result = service.getMaxPerDayForSource("gocomics");

        assertThat(result).isEqualTo(20);
    }

    @Test
    void getMaxPerDayForSource_withZeroSourceValue_returnsDefault() {
        BackfillConfigurationService service = serviceWithSources(Map.of(
                "gocomics", BackfillSourceConfig.builder().source("gocomics").maxPerDay(0).build()));

        int result = service.getMaxPerDayForSource("gocomics");

        assertThat(result).isEqualTo(50);
    }

    @Test
    void getMaxPerDayForSource_unsetMeansNoCeiling() {
        BackfillConfigurationService service = BackfillConfigurationService.builder().build();

        assertThat(service.getMaxPerDayForSource("gocomics")).isZero();
    }

    @Test
    void getMaxPerRunForSource_usesSourceThenDefaultThenFallback() {
        BackfillConfigurationService service = defaultBuilder().defaultMaxPerRun(25).sources(Map.of(
                "gocomics", BackfillSourceConfig.builder().source("gocomics").maxPerRun(40).build())).build();

        assertThat(service.getMaxPerRunForSource("gocomics")).isEqualTo(40);
        assertThat(service.getMaxPerRunForSource("comicskingdom")).isEqualTo(25);
        assertThat(BackfillConfigurationService.builder().build().getMaxPerRunForSource("gocomics"))
                .isEqualTo(BackfillConfigurationService.FALLBACK_MAX_PER_RUN);
    }

    @Test
    void getRecentDaysForSource_usesSourceThenDefaultThenFallback() {
        BackfillConfigurationService service = defaultBuilder().defaultRecentDays(5).sources(Map.of(
                "gocomics", BackfillSourceConfig.builder().source("gocomics").recentDays(10).build())).build();

        assertThat(service.getRecentDaysForSource("gocomics")).isEqualTo(10);
        assertThat(service.getRecentDaysForSource("comicskingdom")).isEqualTo(5);
        assertThat(BackfillConfigurationService.builder().build().getRecentDaysForSource("gocomics"))
                .isEqualTo(BackfillConfigurationService.FALLBACK_RECENT_DAYS);
    }

    @Test
    void learningThresholds_fallBackWhenUnset() {
        BackfillConfigurationService service = BackfillConfigurationService.builder().build();

        assertThat(service.getGiveUpAfter()).isEqualTo(BackfillConfigurationService.FALLBACK_GIVE_UP_AFTER);
        assertThat(service.getHorizonConsecutiveFailures()).isEqualTo(BackfillConfigurationService.FALLBACK_HORIZON_CONSECUTIVE_FAILURES);
        assertThat(service.getHorizonMinComics()).isEqualTo(BackfillConfigurationService.FALLBACK_HORIZON_MIN_COMICS);
        assertThat(service.getHorizonToleranceDays()).isEqualTo(BackfillConfigurationService.FALLBACK_HORIZON_TOLERANCE_DAYS);
        assertThat(service.getRetryGivenUpAfterDays()).isEqualTo(BackfillConfigurationService.FALLBACK_RETRY_GIVEN_UP_AFTER_DAYS);
        assertThat(BackfillConfigurationService.builder().giveUpAfter(4).build().getGiveUpAfter()).isEqualTo(4);
    }

    @Test
    void getMaxDaysBackForSource_withNoSourceConfig_returnsDefault() {
        BackfillConfigurationService service = defaultBuilder().build();

        int result = service.getMaxDaysBackForSource("unknown-source");

        assertThat(result).isEqualTo(365);
    }

    @Test
    void getMaxDaysBackForSource_withSourceConfig_returnsSourceValue() {
        BackfillConfigurationService service = serviceWithSources(Map.of(
                "comicskingdom", BackfillSourceConfig.builder().source("comicskingdom").maxDaysBack(180).build()));

        int result = service.getMaxDaysBackForSource("comicskingdom");

        assertThat(result).isEqualTo(180);
    }

    @Test
    void getEarliestAllowedDate_calculatesCorrectly() {
        BackfillConfigurationService service = defaultBuilder().defaultMaxDaysBack(30).build();

        LocalDate result = service.getEarliestAllowedDate("test-source");

        assertThat(result).isEqualTo(LocalDate.now().minusDays(30));
    }

    @Test
    void getEarliestAllowedDate_usesSourceSpecificLimit() {
        BackfillConfigurationService service = serviceWithSources(Map.of(
                "short-history", BackfillSourceConfig.builder().source("short-history").maxDaysBack(7).build()));

        LocalDate result = service.getEarliestAllowedDate("short-history");

        assertThat(result).isEqualTo(LocalDate.now().minusDays(7));
    }

    @Test
    void isSourceEnabled_withNoConfig_returnsTrue() {
        BackfillConfigurationService service = defaultBuilder().build();

        assertThat(service.isSourceEnabled("unknown-source")).isTrue();
    }

    @Test
    void isSourceEnabled_withEnabledSource_returnsTrue() {
        BackfillConfigurationService service = serviceWithSources(Map.of(
                "enabled-source", BackfillSourceConfig.builder().source("enabled-source").enabled(true).build()));

        assertThat(service.isSourceEnabled("enabled-source")).isTrue();
    }

    @Test
    void isSourceEnabled_withDisabledSource_returnsFalse() {
        BackfillConfigurationService service = serviceWithSources(Map.of(
                "disabled-source", BackfillSourceConfig.builder().source("disabled-source").enabled(false).build()));

        assertThat(service.isSourceEnabled("disabled-source")).isFalse();
    }

    @Test
    void isSourceEnabled_withNullEnabled_returnsTrue() {
        BackfillConfigurationService service = serviceWithSources(Map.of(
                "null-enabled", BackfillSourceConfig.builder().source("null-enabled").enabled(null).build()));

        assertThat(service.isSourceEnabled("null-enabled")).isTrue();
    }
}
