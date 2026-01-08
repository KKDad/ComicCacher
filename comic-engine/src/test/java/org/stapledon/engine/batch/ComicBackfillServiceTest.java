package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.engine.batch.ComicBackfillService.BackfillTask;
import org.stapledon.engine.management.ManagementFacade;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ComicBackfillServiceTest {

    @Mock
    private ManagementFacade managementFacade;

    @Mock
    private ComicStorageFacade storageFacade;

    @Mock
    private BackfillConfigurationService configService;

    private ComicBackfillService service;

    private static final int MAX_CONSECUTIVE_FAILURES = 14;
    private static final int DEFAULT_MAX_PER_DAY = 100;
    private static final int DEFAULT_MAX_DAYS_BACK = 365;

    @BeforeEach
    void setUp() {
        service = new ComicBackfillService(managementFacade, storageFacade, configService);

        // Setup default configuration service behavior using lenient to avoid
        // UnnecessaryStubbingException for tests that don't use all stubs
        lenient().when(configService.getMaxConsecutiveFailures()).thenReturn(MAX_CONSECUTIVE_FAILURES);
        lenient().when(configService.getMaxPerDayForSource(anyString())).thenReturn(DEFAULT_MAX_PER_DAY);
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
        assertThat(result.stream().anyMatch(t -> t.date().equals(missingDate1))).isTrue();
        assertThat(result.stream().anyMatch(t -> t.date().equals(missingDate2))).isTrue();
    }

    @Test
    void findMissingStrips_stopsAfterMaxConsecutiveFailures() {
        ComicItem comic = createComic(1, "Old Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        // Mock all strips as missing (comic doesn't exist this far back)
        when(storageFacade.comicStripExists(any(ComicIdentifier.class), any(LocalDate.class))).thenReturn(false);

        List<BackfillTask> result = service.findMissingStrips();

        // Should stop after MAX_CONSECUTIVE_FAILURES missing strips
        assertThat(result.size() <= MAX_CONSECUTIVE_FAILURES).isTrue();
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
            DayOfWeek dayOfWeek = task.date().getDayOfWeek();
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
            assertThat(task.date().isAfter(today)).as("Should not scan future dates: " + task.date()).isFalse();
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
        assertThat(result.stream().filter(t -> t.date().equals(missingDate)).count()).isEqualTo(2);
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
            assertThat(task.date().isBefore(earliestAllowed)).as("Should not scan before earliest allowed date: " + task.date()).isFalse();
        }
    }

    @Test
    void findMissingStrips_respectsMaxPerDay() {
        ComicItem comic = createComic(1, "Test Comic", true);
        when(managementFacade.getAllComics()).thenReturn(List.of(comic));

        // Set max per day to 5
        when(configService.getMaxPerDayForSource("test-source")).thenReturn(5);

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
            assertThat(task.date().isBefore(comicOldest)).as("Should not scan before comic's oldest date: " + task.date()).isFalse();
        }
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
