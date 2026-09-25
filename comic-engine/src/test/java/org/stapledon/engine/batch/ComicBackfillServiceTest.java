package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.engine.batch.ComicBackfillService.BackfillTask;
import org.stapledon.engine.batch.ComicBackfillService.DateBackfillTask;
import org.stapledon.engine.downloader.DownloaderFacade;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.engine.storage.ComicIndexService;

@ExtendWith(MockitoExtension.class)
class ComicBackfillServiceTest {

    @Mock
    private ManagementFacade managementFacade;

    @Mock
    private ComicStorageFacade storageFacade;

    @Mock
    private BackfillConfigurationService configService;

    @Mock
    private DownloaderFacade downloaderFacade;

    @Mock
    private ComicIndexService comicIndexService;

    @Mock
    private BackfillStateService backfillState;

    private ComicBackfillService service;

    private static final int MAX_CONSECUTIVE_FAILURES = 14;
    private static final int DEFAULT_MAX_PER_RUN = 100;
    private static final int RECENT_DAYS = 7;
    private static final int DEFAULT_MAX_DAYS_BACK = 365;

    @BeforeEach
    void setUp() {
        service = new ComicBackfillService(managementFacade, storageFacade, configService, downloaderFacade, comicIndexService, backfillState);

        // Setup default configuration service behavior using lenient to avoid
        // UnnecessaryStubbingException for tests that don't use all stubs
        lenient().when(configService.getMaxConsecutiveFailures()).thenReturn(MAX_CONSECUTIVE_FAILURES);
        lenient().when(configService.getMaxPerRunForSource(anyString())).thenReturn(DEFAULT_MAX_PER_RUN);
        lenient().when(configService.getRecentDaysForSource(anyString())).thenReturn(RECENT_DAYS);
        lenient().when(configService.getMaxDaysBackForSource(anyString())).thenReturn(DEFAULT_MAX_DAYS_BACK);
        lenient().when(configService.getEarliestAllowedDate(anyString())).thenReturn(LocalDate.now().minusDays(DEFAULT_MAX_DAYS_BACK));
        lenient().when(configService.isSourceEnabled(anyString())).thenReturn(true);
    }

    @Test
    void findMissingStrips_withNoComics_returnsEmptyList() {
        when(managementFacade.getAllComics()).thenReturn(List.of());

        List<BackfillTask> result = service.findMissingStrips();

        assertThat(result.isEmpty()).isTrue();
        verify(managementFacade).getAllComics();
        verifyNoInteractions(storageFacade);
    }

    @Test
    void findMissingStrips_withInactiveComic_skipsComic() {
        ComicItem comic = createComic(1, "Inactive Comic", false);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        List<BackfillTask> result = service.findMissingStrips();

        assertThat(result.isEmpty()).isTrue();
        verifyNoInteractions(storageFacade);
    }

    @Test
    void findMissingStrips_withComicWithNoSource_skipsComic() {
        ComicItem comic = createComic(1, "No Source Comic", true);
        comic.setSource(null);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        List<BackfillTask> result = service.findMissingStrips();

        assertThat(result.isEmpty()).isTrue();
        verifyNoInteractions(storageFacade);
    }

    @Test
    void findMissingStrips_withDisabledSource_skipsComic() {
        ComicItem comic = createComic(1, "Disabled Source Comic", true);
        comic.setSource("disabled-source");
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        when(configService.isSourceEnabled("disabled-source")).thenReturn(false);

        List<BackfillTask> result = service.findMissingStrips();

        assertThat(result.isEmpty()).isTrue();
        verifyNoInteractions(storageFacade);
    }

    @Test
    void findMissingStrips_withAllStripsExisting_returnsEmptyList() {
        ComicItem comic = createComic(1, "Complete Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        // Mock all strips as existing
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(true);

        List<BackfillTask> result = service.findMissingStrips();

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void repeatedScans_doNotRecheckStripsAlreadySeenCached() {
        when(configService.isRememberCachedStrips()).thenReturn(true);
        ComicItem comic = createComic(1, "Complete Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(true);

        assertThat(service.hasMissingStrips(null)).isFalse();
        clearInvocations(storageFacade);

        assertThat(service.hasMissingStrips(null)).isFalse();
        assertThat(service.findMissingStrips()).isEmpty();

        // Only the recent window is rechecked on disk; the older history comes from memory
        verify(storageFacade, times(2 * RECENT_DAYS)).comicStripExists(any(ComicIdentifier.class), any(LocalDate.class));
    }

    @Test
    void repeatedScans_withMemoryOff_checkEveryDateOnDisk() {
        ComicItem comic = createComic(1, "Complete Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(true);

        service.hasMissingStrips(null);
        int firstScanChecks = mockingDetails(storageFacade).getInvocations().size();
        service.hasMissingStrips(null);

        assertThat(mockingDetails(storageFacade).getInvocations()).hasSize(2 * firstScanChecks);
    }

    @Test
    void repeatedScans_noticeARememberedStripThatWentMissing() {
        when(configService.isRememberCachedStrips()).thenReturn(true);
        ComicItem comic = createComic(1, "Comic", true);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        AtomicBoolean deleted = new AtomicBoolean();
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class)))
                .thenAnswer(invocation -> !(deleted.get() && invocation.getArgument(1).equals(yesterday)));

        assertThat(service.hasMissingStrips(null)).isFalse();
        deleted.set(true);

        List<BackfillTask> tasks = service.findMissingStrips();

        assertThat(tasks).containsExactly(new DateBackfillTask(comic, yesterday));
    }

    @Test
    void repeatedScans_recheckMissingStrips() {
        ComicItem comic = createComic(1, "Partial Comic", true);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        AtomicBoolean saved = new AtomicBoolean();
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class)))
                .thenAnswer(invocation -> saved.get() || !invocation.getArgument(1).equals(yesterday));

        when(configService.isRememberCachedStrips()).thenReturn(true);
        assertThat(service.hasMissingStrips(null)).isTrue();
        // Another job saves the strip between scans
        saved.set(true);

        assertThat(service.hasMissingStrips(null)).isFalse();
        verify(storageFacade, times(2)).comicStripExists(any(ComicIdentifier.class), eq(yesterday));
    }

    @Test
    void findMissingStrips_withSomeMissingStrips_returnsMissingDates() {
        ComicItem comic = createComic(1, "Partial Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        LocalDate today = LocalDate.now();
        LocalDate missingDate1 = today.minusDays(1);
        LocalDate missingDate2 = today.minusDays(3);

        // Mock specific dates as missing
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenAnswer(invocation -> {
            LocalDate date = invocation.getArgument(1);
            return !date.equals(missingDate1) && !date.equals(missingDate2);
        });

        List<BackfillTask> result = service.findMissingStrips();

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.stream()
                .filter(DateBackfillTask.class::isInstance)
                .map(DateBackfillTask.class::cast)
                .anyMatch(t -> t.date().equals(missingDate1))).isTrue();
        assertThat(result.stream()
                .filter(DateBackfillTask.class::isInstance)
                .map(DateBackfillTask.class::cast)
                .anyMatch(t -> t.date().equals(missingDate2))).isTrue();
    }

    @Test
    void findMissingStrips_stopsAfterMaxConsecutiveFailures() {
        ComicItem comic = createComic(1, "Old Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        // Mock all strips as missing (comic doesn't exist this far back)
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        List<BackfillTask> result = service.findMissingStrips();

        // The recent window is always filled; the history walk stops after MAX_CONSECUTIVE_FAILURES missing strips
        assertThat(result).hasSize(RECENT_DAYS + MAX_CONSECUTIVE_FAILURES);
    }

    @Test
    void findMissingStrips_resetsConsecutiveCounterWhenStripFound() {
        ComicItem comic = createComic(1, "Spotty Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        // Pattern: several missing, 1 found, repeat
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenAnswer(invocation -> {
            LocalDate date = invocation.getArgument(1);
            int dayOfYear = date.getDayOfYear();
            return dayOfYear % 11 == 0; // Every 11th day exists
        });

        List<BackfillTask> result = service.findMissingStrips();

        // Should continue past MAX_CONSECUTIVE_FAILURES because counter resets
        assertThat(result.size() > MAX_CONSECUTIVE_FAILURES).isTrue();
    }

    @Test
    void findMissingStrips_respectsPublicationDays() {
        ComicItem comic = createComic(1, "Weekday Comic", true);
        comic.setPublicationDays(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));

        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        // Mock all strips as missing
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        List<BackfillTask> result = service.findMissingStrips();

        // Verify no weekend dates are included
        for (BackfillTask task : result) {
            DateBackfillTask dateTask = (DateBackfillTask) task;
            DayOfWeek dayOfWeek = dateTask.date().getDayOfWeek();
            assertThat(dayOfWeek).isNotEqualTo(DayOfWeek.SATURDAY);
            assertThat(dayOfWeek).isNotEqualTo(DayOfWeek.SUNDAY);
        }
    }

    @Test
    void findMissingStrips_doesNotScanFutureDates() {
        ComicItem comic = createComic(1, "Current Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        // Mock all strips as missing
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        List<BackfillTask> result = service.findMissingStrips();

        LocalDate today = LocalDate.now();

        // Verify no future dates are included
        for (BackfillTask task : result) {
            DateBackfillTask dateTask = (DateBackfillTask) task;
            assertThat(dateTask.date().isAfter(today)).as("Should not scan future dates: " + dateTask.date()).isFalse();
        }
    }

    @Test
    void findMissingStrips_withMultipleComics_processesAll() {
        ComicItem comic1 = createComic(1, "Comic One", true);
        ComicItem comic2 = createComic(2, "Comic Two", true);

        when(managementFacade.getAllComics()).thenReturn(List.of(comic1, comic2));

        LocalDate missingDate = LocalDate.now().minusDays(5);

        // Mock first date as missing for both comics, all others exist
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenAnswer(invocation -> !invocation.getArgument(1).equals(missingDate));

        List<BackfillTask> result = service.findMissingStrips();

        // Should have tasks for both comics
        assertThat(result.stream()
                .filter(DateBackfillTask.class::isInstance)
                .map(DateBackfillTask.class::cast)
                .filter(t -> t.date().equals(missingDate)).count()).isEqualTo(2);
    }

    @Test
    void findMissingStrips_respectsMaxDaysBack() {
        ComicItem comic = createComic(1, "Test Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        // Set max days back to 30
        LocalDate earliestAllowed = LocalDate.now().minusDays(30);
        when(configService.getEarliestAllowedDate("test-source")).thenReturn(earliestAllowed);

        // Mock all strips as missing
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        List<BackfillTask> result = service.findMissingStrips();

        // Verify no dates before earliest allowed
        for (BackfillTask task : result) {
            DateBackfillTask dateTask = (DateBackfillTask) task;
            assertThat(dateTask.date().isBefore(earliestAllowed)).as("Should not scan before earliest allowed date: " + dateTask.date()).isFalse();
        }
    }

    @Test
    void findMissingStrips_respectsMaxPerRun() {
        ComicItem comic = createComic(1, "Test Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        // Set max per run to 5
        when(configService.getMaxPerRunForSource("test-source")).thenReturn(5);

        // Mock all strips as missing
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        List<BackfillTask> result = service.findMissingStrips();

        // Should be limited to 5 tasks
        assertThat(result.size()).isLessThanOrEqualTo(5);
    }

    @Test
    void findMissingStrips_respectsComicOldestDate() {
        ComicItem comic = createComic(1, "Newer Comic", true);
        LocalDate comicOldest = LocalDate.now().minusDays(10);
        comic.setOldest(comicOldest);

        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        // Mock all strips as missing
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        List<BackfillTask> result = service.findMissingStrips();

        // Verify no dates before comic's oldest date
        for (BackfillTask task : result) {
            DateBackfillTask dateTask = (DateBackfillTask) task;
            assertThat(dateTask.date().isBefore(comicOldest)).as("Should not scan before comic's oldest date: " + dateTask.date()).isFalse();
        }
    }

    @Test
    void findMissingStrips_recentGapsInEveryComicComeBeforeOlderGaps() {
        // "Aardvark" has lots of old gaps; "Zebra" only misses yesterday. With a budget of 3, Zebra's recent gap must still be picked.
        ComicItem early = createComic(1, "Aardvark", true);
        ComicItem late = createComic(2, "Zebra", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(early, late));
        when(configService.getMaxPerRunForSource("test-source")).thenReturn(3);

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate oldestRecent = LocalDate.now().minusDays(RECENT_DAYS - 1L);
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenAnswer(invocation -> {
            ComicIdentifier id = invocation.getArgument(0);
            LocalDate date = invocation.getArgument(1);
            if (id.getId() == 1) {
                return !date.isBefore(oldestRecent); // recent days all present, everything older missing
            }
            return !date.equals(yesterday);
        });

        List<BackfillTask> result = service.findMissingStrips();

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(new DateBackfillTask(late, yesterday));
        assertThat(result.subList(1, 3)).allMatch(t -> t.comic().equals(early));
    }

    @Test
    void findMissingStrips_recentPassGoesNewestDateFirstAcrossComics() {
        ComicItem one = createComic(1, "One", true);
        ComicItem two = createComic(2, "Two", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(one, two));
        when(configService.getMaxPerRunForSource("test-source")).thenReturn(4);
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        LocalDate today = LocalDate.now();
        List<BackfillTask> result = service.findMissingStrips();

        assertThat(result).containsExactly(
                new DateBackfillTask(one, today), new DateBackfillTask(two, today),
                new DateBackfillTask(one, today.minusDays(1)), new DateBackfillTask(two, today.minusDays(1)));
    }

    @Test
    void findMissingStrips_historyPassIsRoundRobin() {
        ComicItem one = createComic(1, "One", true);
        ComicItem two = createComic(2, "Two", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(one, two));
        when(configService.getMaxPerRunForSource("test-source")).thenReturn(4);

        LocalDate oldestRecent = LocalDate.now().minusDays(RECENT_DAYS - 1L);
        // Recent window complete; every older date missing for both comics
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class)))
                .thenAnswer(invocation -> !((LocalDate) invocation.getArgument(1)).isBefore(oldestRecent));

        List<BackfillTask> result = service.findMissingStrips();

        LocalDate first = oldestRecent.minusDays(1);
        assertThat(result).containsExactly(
                new DateBackfillTask(one, first), new DateBackfillTask(two, first),
                new DateBackfillTask(one, first.minusDays(1)), new DateBackfillTask(two, first.minusDays(1)));
    }

    @Test
    void findMissingStrips_skipsGivenUpDates() {
        ComicItem comic = createComic(1, "Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class)))
                .thenAnswer(invocation -> !invocation.getArgument(1).equals(yesterday));
        when(backfillState.isGivenUp(comic, yesterday)).thenReturn(true);

        assertThat(service.findMissingStrips()).isEmpty();
    }

    @Test
    void findMissingStrips_stopsAtLearnedHorizon() {
        ComicItem comic = createComic(1, "Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        LocalDate horizon = LocalDate.now().minusDays(10);
        when(backfillState.horizonFloor(comic)).thenReturn(Optional.of(horizon));
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        List<BackfillTask> result = service.findMissingStrips();

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(t -> ((DateBackfillTask) t).date().isAfter(horizon));
    }

    @Test
    void findMissingStrips_respectsDailyCeiling() {
        ComicItem comic = createComic(1, "Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        when(configService.getMaxPerDayForSource("test-source")).thenReturn(10);
        when(backfillState.attemptsToday("test-source")).thenReturn(8);
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        assertThat(service.findMissingStrips()).hasSize(2);
    }

    @Test
    void findMissingStrips_dailyCeilingReachedSkipsSource() {
        ComicItem comic = createComic(1, "Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        when(configService.getMaxPerDayForSource("test-source")).thenReturn(10);
        when(backfillState.attemptsToday("test-source")).thenReturn(10);

        assertThat(service.findMissingStrips()).isEmpty();
        verifyNoInteractions(storageFacade);
    }

    @Test
    void hasMissingStrips_trueWhenAnythingIsMissing() {
        ComicItem comic = createComic(1, "Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        assertThat(service.hasMissingStrips(null)).isTrue();
    }

    @Test
    void hasMissingStrips_falseWhenNothingIsMissing() {
        ComicItem comic = createComic(1, "Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(true);

        assertThat(service.hasMissingStrips(null)).isFalse();
    }

    @Test
    void hasMissingStrips_neverDiscoversIndexedComicsOverTheNetwork() {
        ComicItem comic = createComic(1, "Indexed", true);
        comic.setSource("freefall");
        comic.setLastStripNumber(null);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));
        when(downloaderFacade.isIndexedSource("freefall")).thenReturn(true);

        assertThat(service.hasMissingStrips(null)).isFalse();
        verify(managementFacade, never()).downloadLatestIndexedComic(any());
    }

    private ComicItem createComic(int id, String name, boolean active) {
        ComicItem comic = new ComicItem();
        comic.setId(id);
        comic.setName(name);
        comic.setActive(active);
        comic.setSource("test-source");
        comic.setSourceIdentifier("test-identifier");
        return comic;
    }
}
