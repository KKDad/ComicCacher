package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.util.GsonUtils;

@DisplayName("BackfillStateService")
class BackfillStateServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Toronto");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    @TempDir
    Path tempDir;

    private CacheProperties cacheProperties;
    private Gson gson;
    private BackfillConfigurationService config;
    private Clock clock;
    private BackfillStateService service;

    @BeforeEach
    void setUp() {
        cacheProperties = CacheProperties.builder().location(tempDir.toString()).build();
        gson = GsonUtils.createGsonBuilder().create();
        // Zero values fall back to the built-in defaults: give up after 2, horizon after 3, 3 comics within 2 days, 7 recent days, retry after 30 days
        config = BackfillConfigurationService.builder().build();
        clock = clockAt(TODAY);
        service = new BackfillStateService(cacheProperties, gson, config, clock);
    }

    @Test
    @DisplayName("gives up on a date after repeated unavailable results")
    void givesUpAfterRepeatedFailures() {
        ComicItem comic = comic(1, "BC");
        LocalDate date = LocalDate.of(2025, 6, 29);

        service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_UNAVAILABLE);
        assertThat(service.isGivenUp(comic, date)).isFalse();

        service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_UNAVAILABLE);
        assertThat(service.isGivenUp(comic, date)).isTrue();
    }

    @Test
    @DisplayName("tries a given-up date again once the retry period has passed")
    void givenUpDatesExpire() {
        ComicItem comic = comic(1, "Agnes");
        LocalDate date = LocalDate.of(2026, 7, 5);
        service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_DUPLICATE);
        service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_DUPLICATE);

        BackfillStateService later = new BackfillStateService(cacheProperties, gson, config, clockAt(TODAY.plusDays(31)));

        assertThat(later.isGivenUp(comic, date)).isFalse();
    }

    @Test
    @DisplayName("a success clears the date's failures")
    void successClearsFailures() {
        ComicItem comic = comic(1, "BC");
        LocalDate date = TODAY.minusDays(1);
        service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_UNAVAILABLE);
        service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_UNAVAILABLE);

        service.recordSuccess(comic, date);

        assertThat(service.isGivenUp(comic, date)).isFalse();
    }

    @Test
    @DisplayName("learns a comic horizon after several old dates in a row are unavailable")
    void learnsComicHorizon() {
        ComicItem comic = comic(1, "Luann");
        LocalDate newest = TODAY.minusDays(8);

        service.recordUnavailable(comic, newest, BackfillStateService.OUTCOME_UNAVAILABLE);
        service.recordUnavailable(comic, newest.minusDays(1), BackfillStateService.OUTCOME_UNAVAILABLE);
        assertThat(service.horizonFloor(comic)).isEmpty();

        service.recordUnavailable(comic, newest.minusDays(2), BackfillStateService.OUTCOME_UNAVAILABLE);

        assertThat(service.horizonFloor(comic)).contains(newest);
    }

    @Test
    @DisplayName("recent-window failures don't count towards a horizon")
    void recentFailuresDoNotSetHorizon() {
        ComicItem comic = comic(1, "Luann");

        for (int i = 0; i < 5; i++) {
            service.recordUnavailable(comic, TODAY.minusDays(i), BackfillStateService.OUTCOME_UNAVAILABLE);
        }

        assertThat(service.horizonFloor(comic)).isEmpty();
    }

    @Test
    @DisplayName("the same date failing on several runs isn't mistaken for a horizon")
    void repeatedDateDoesNotSetHorizon() {
        ComicItem comic = comic(1, "BC");
        LocalDate date = LocalDate.of(2025, 6, 29);

        for (int i = 0; i < 5; i++) {
            service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_UNAVAILABLE);
        }

        assertThat(service.horizonFloor(comic)).isEmpty();
    }

    @Test
    @DisplayName("an older success breaks the run of failures")
    void successBreaksFailureRun() {
        ComicItem comic = comic(1, "Luann");
        LocalDate start = TODAY.minusDays(20);

        service.recordUnavailable(comic, start, BackfillStateService.OUTCOME_UNAVAILABLE);
        service.recordUnavailable(comic, start.minusDays(1), BackfillStateService.OUTCOME_UNAVAILABLE);
        service.recordSuccess(comic, start.minusDays(2));
        service.recordUnavailable(comic, start.minusDays(3), BackfillStateService.OUTCOME_UNAVAILABLE);

        assertThat(service.horizonFloor(comic)).isEmpty();
    }

    @Test
    @DisplayName("learns a source horizon when several comics stop at about the same age")
    void learnsSourceHorizon() {
        // Three comics hit a wall at 8, 9 and 8 days back; a fourth comic with no failures inherits the source horizon
        hitWall(comic(1, "Luann"), 8);
        hitWall(comic(2, "Ziggy"), 9);
        assertThat(service.horizonFloor(comic(4, "Pickles"))).isEmpty();

        hitWall(comic(3, "Shoe"), 8);

        assertThat(service.horizonFloor(comic(4, "Pickles"))).contains(TODAY.minusDays(9));
    }

    @Test
    @DisplayName("a download older than the source horizon clears it")
    void oldSuccessClearsSourceHorizon() {
        hitWall(comic(1, "Luann"), 8);
        hitWall(comic(2, "Ziggy"), 8);
        hitWall(comic(3, "Shoe"), 8);
        ComicItem other = comic(4, "Pickles");
        assertThat(service.horizonFloor(other)).isPresent();

        service.recordSuccess(other, TODAY.minusDays(40));

        assertThat(service.horizonFloor(other)).isEmpty();
    }

    @Test
    @DisplayName("counts today's attempts per source and starts over the next day")
    void countsAttemptsPerDay() {
        service.recordAttempt("gocomics");
        service.recordAttempt("gocomics");

        assertThat(service.attemptsToday("gocomics")).isEqualTo(2);
        assertThat(service.attemptsToday("comicskingdom")).isZero();

        BackfillStateService tomorrow = new BackfillStateService(cacheProperties, gson, config, clockAt(TODAY.plusDays(1)));
        assertThat(tomorrow.attemptsToday("gocomics")).isZero();
    }

    @Test
    @DisplayName("persists to backfill-state.json and reloads")
    void persistsAndReloads() {
        ComicItem comic = comic(1, "BC");
        LocalDate date = LocalDate.of(2025, 6, 29);
        service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_UNAVAILABLE);
        service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_UNAVAILABLE);

        assertThat(Files.exists(tempDir.resolve(BackfillStateService.STATE_FILENAME))).isTrue();
        BackfillStateService reloaded = new BackfillStateService(cacheProperties, gson, config, clock);
        assertThat(reloaded.isGivenUp(comic, date)).isTrue();
    }

    @Test
    @DisplayName("reset forgets everything")
    void resetForgetsEverything() {
        ComicItem comic = comic(1, "BC");
        LocalDate date = LocalDate.of(2025, 6, 29);
        service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_UNAVAILABLE);
        service.recordUnavailable(comic, date, BackfillStateService.OUTCOME_UNAVAILABLE);
        hitWall(comic(2, "Luann"), 8);

        service.reset();

        assertThat(service.isGivenUp(comic, date)).isFalse();
        assertThat(service.horizonFloor(comic(2, "Luann"))).isEmpty();
        assertThat(new BackfillStateService(cacheProperties, gson, config, clock).isGivenUp(comic, date)).isFalse();
    }

    @Test
    @DisplayName("a corrupt state file starts empty instead of failing")
    void corruptFileStartsEmpty() throws Exception {
        Files.writeString(tempDir.resolve(BackfillStateService.STATE_FILENAME), "{not json");

        assertThat(service.isGivenUp(comic(1, "BC"), TODAY)).isFalse();
    }

    private void hitWall(ComicItem comic, int daysBack) {
        LocalDate newest = TODAY.minusDays(daysBack);
        for (int i = 0; i < 3; i++) {
            service.recordUnavailable(comic, newest.minusDays(i), BackfillStateService.OUTCOME_UNAVAILABLE);
        }
    }

    private static Clock clockAt(LocalDate date) {
        Instant noon = date.atTime(12, 0).atZone(ZONE).toInstant();
        return Clock.fixed(noon, ZONE);
    }

    private static ComicItem comic(int id, String name) {
        ComicItem comic = new ComicItem();
        comic.setId(id);
        comic.setName(name);
        comic.setSource("gocomics");
        return comic;
    }
}
