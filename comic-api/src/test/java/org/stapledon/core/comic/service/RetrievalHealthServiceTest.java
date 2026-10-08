package org.stapledon.core.comic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.common.service.RetrievalStatusService;
import org.stapledon.core.comic.service.RetrievalHealthService.ComicRetrievalHealth;
import org.stapledon.core.comic.service.RetrievalHealthService.DayOutcome;
import org.stapledon.core.comic.service.RetrievalHealthService.RetrievalDay;
import org.stapledon.core.comic.service.RetrievalHealthService.RetrievalHealth;
import org.stapledon.engine.batch.BatchJobMonitoringService;
import org.stapledon.engine.batch.dto.BatchExecutionSummary;
import org.stapledon.engine.downloader.DownloaderFacade;
import org.stapledon.engine.management.ManagementFacade;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class RetrievalHealthServiceTest {
    private static final ZoneId ZONE = ZoneId.of("America/Toronto");
    /** Thursday 2026-10-08, noon in Toronto. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-08T16:00:00Z"), ZONE);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

    @Mock
    private ManagementFacade managementFacade;
    @Mock
    private ComicStorageFacade storageFacade;
    @Mock
    private RetrievalStatusService retrievalStatusService;
    @Mock
    private BatchJobMonitoringService monitoringService;
    @Mock
    private DownloaderFacade downloaderFacade;

    private final List<ComicItem> comics = new ArrayList<>();
    private final List<ComicRetrievalRecord> records = new ArrayList<>();
    private final Map<Integer, List<LocalDate>> dates = new HashMap<>();
    private BatchExecutionSummary lastRun;

    private RetrievalHealthService service;

    @BeforeEach
    void setUp() {
        service = new RetrievalHealthService(managementFacade, storageFacade, retrievalStatusService, monitoringService,
                downloaderFacade, CLOCK, 30);
        lastRun = run(TODAY, true);
        lenient().when(managementFacade.getAllComics()).thenAnswer(i -> comics);
        lenient().when(retrievalStatusService.getRetrievalRecords(any(), any(), any(), any(), anyInt())).thenAnswer(i -> records);
        lenient().when(monitoringService.getRecentJobExecutions(anyString(), anyInt()))
                .thenAnswer(i -> lastRun != null ? List.of(lastRun) : List.of());
        lenient().when(storageFacade.getAvailableDates(any(ComicIdentifier.class)))
                .thenAnswer(i -> dates.getOrDefault(i.<ComicIdentifier>getArgument(0).getId(), List.of()));
        lenient().when(downloaderFacade.isIndexedSource("freefall")).thenReturn(true);
    }

    @Test
    void failureLaterBackfilledIsOnDiskAndRecovered() {
        comic(1, "Daily", "gocomics", null);
        dates.put(1, range(TODAY.minusDays(40), TODAY));
        records.add(failure(1, "Daily", TODAY.minusDays(3), ComicRetrievalStatus.NETWORK_ERROR, TODAY.minusDays(3)));

        ComicRetrievalHealth health = only(service.getHealth(7, 20));

        RetrievalDay day = dayOf(health, TODAY.minusDays(3));
        assertThat(day.outcome()).isEqualTo(DayOutcome.ON_DISK);
        assertThat(day.recovered()).isTrue();
        assertThat(health.days()).extracting(RetrievalDay::outcome).containsOnly(DayOutcome.ON_DISK);
        assertThat(health.missingStreak()).isZero();
        assertThat(health.stale()).isFalse();
        assertThat(health.latestError()).isNull();
    }

    @Test
    void missingOnPublicationDaysAndOffDaysElsewhere() {
        comic(1, "Mondays", "gocomics", List.of(DayOfWeek.MONDAY));
        dates.put(1, List.of(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 28)));
        ComicRetrievalRecord rateLimited = failure(1, "Mondays", LocalDate.of(2026, 10, 5), ComicRetrievalStatus.RATE_LIMITED,
                LocalDate.of(2026, 10, 5));
        records.add(rateLimited);

        ComicRetrievalHealth health = only(service.getHealth(7, 20));

        assertThat(dayOf(health, LocalDate.of(2026, 10, 5)).outcome()).isEqualTo(DayOutcome.MISSING);
        assertThat(health.days()).filteredOn(d -> !d.date().equals(LocalDate.of(2026, 10, 5)))
                .extracting(RetrievalDay::outcome).containsOnly(DayOutcome.OFF_DAY);
        assertThat(health.missingStreak()).isEqualTo(1);
        assertThat(health.latestError()).isEqualTo(rateLimited);
        assertThat(health.expectedLatest()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(health.stale()).isTrue();
        assertThat(health.newest()).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    @Test
    void successRecordWithNothingOnDiskIsMissing() {
        comic(1, "Daily", "gocomics", null);
        dates.put(1, range(TODAY.minusDays(40), TODAY.minusDays(1)));
        records.add(ComicRetrievalRecord.success(1, "Daily", TODAY, "gocomics", 100L, 1000L));

        ComicRetrievalHealth health = only(service.getHealth(7, 20));

        assertThat(dayOf(health, TODAY).outcome()).isEqualTo(DayOutcome.MISSING);
        assertThat(health.stale()).isTrue();
    }

    @Test
    void todayIsPendingUntilTodaysRunFinishes() {
        comic(1, "Daily", "gocomics", null);
        dates.put(1, range(TODAY.minusDays(40), TODAY.minusDays(1)));
        lastRun = run(TODAY.minusDays(1), true);

        ComicRetrievalHealth health = only(service.getHealth(7, 20));

        assertThat(dayOf(health, TODAY).outcome()).isEqualTo(DayOutcome.PENDING);
        assertThat(health.expectedLatest()).isEqualTo(TODAY.minusDays(1));
        assertThat(health.stale()).isFalse();
        assertThat(health.missingStreak()).isZero();
    }

    @Test
    void todayIsPendingWhileTodaysRunIsStillGoing() {
        comic(1, "Daily", "gocomics", null);
        dates.put(1, range(TODAY.minusDays(40), TODAY.minusDays(1)));
        lastRun = run(TODAY, false);

        assertThat(dayOf(only(service.getHealth(7, 20)), TODAY).outcome()).isEqualTo(DayOutcome.PENDING);
    }

    @Test
    void stripOnAnOffDayIsOnDisk() {
        comic(1, "Mondays", "gocomics", List.of(DayOfWeek.MONDAY));
        dates.put(1, List.of(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7)));

        assertThat(dayOf(only(service.getHealth(7, 20)), LocalDate.of(2026, 10, 7)).outcome()).isEqualTo(DayOutcome.ON_DISK);
    }

    @Test
    void daysBeforeTheFirstStripAreOffDays() {
        comic(1, "New", "gocomics", null);
        dates.put(1, range(TODAY.minusDays(2), TODAY));

        ComicRetrievalHealth health = only(service.getHealth(7, 20));

        assertThat(dayOf(health, TODAY.minusDays(3)).outcome()).isEqualTo(DayOutcome.OFF_DAY);
        assertThat(health.missingStreak()).isZero();
    }

    @Test
    void inactiveComicExpectsNothing() {
        comic(1, "Ended", "gocomics", null).setActive(false);
        dates.put(1, List.of(LocalDate.of(2020, 1, 1)));

        ComicRetrievalHealth health = only(service.getHealth(7, 20));

        assertThat(health.days()).extracting(RetrievalDay::outcome).containsOnly(DayOutcome.OFF_DAY);
        assertThat(health.expectedLatest()).isNull();
        assertThat(health.stale()).isFalse();
    }

    @Test
    void indexedSourceIsMissingOnlyWhenAnAttemptFailed() {
        comic(1, "Numbered", "freefall", null);
        dates.put(1, List.of(TODAY.minusDays(5)));
        records.add(failure(1, "Numbered", TODAY.minusDays(1), ComicRetrievalStatus.PARSING_ERROR, TODAY.minusDays(1)));

        ComicRetrievalHealth health = only(service.getHealth(7, 20));

        assertThat(health.indexed()).isTrue();
        assertThat(dayOf(health, TODAY.minusDays(1)).outcome()).isEqualTo(DayOutcome.MISSING);
        assertThat(dayOf(health, TODAY.minusDays(2)).outcome()).isEqualTo(DayOutcome.OFF_DAY);
        assertThat(health.expectedLatest()).isNull();
    }

    @Test
    void missingStreakSkipsOffDaysAndStopsAtAStripOnDisk() {
        comic(1, "Weekdays", "gocomics", List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY));
        // On disk through Friday Oct 2; Mon 5 to Thu 8 missing, with the weekend between
        dates.put(1, range(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 2)));

        assertThat(only(service.getHealth(14, 20)).missingStreak()).isEqualTo(4);
    }

    @Test
    void recordsWithoutComicIdMatchByName() {
        comic(7, "Old Timer", "gocomics", null);
        dates.put(7, range(TODAY.minusDays(40), TODAY.minusDays(2)));
        ComicRetrievalRecord old = ComicRetrievalRecord.failure(null, "Old Timer", TODAY.minusDays(1), "gocomics",
                ComicRetrievalStatus.NETWORK_ERROR, "Timed out", 10L, null);
        records.add(old);

        ComicRetrievalHealth health = only(service.getHealth(7, 20));

        assertThat(dayOf(health, TODAY.minusDays(1)).record()).isEqualTo(old);
        assertThat(health.latestError()).isEqualTo(old);
    }

    @Test
    void todaysErrorsUseTheBatchTimezoneAndMarkRecovered() {
        comic(1, "Daily", "gocomics", null);
        dates.put(1, List.of(TODAY.minusDays(20)));
        // 03:00 UTC is still yesterday in Toronto; 05:00 and 06:00 UTC are today
        records.add(failure(1, "Daily", TODAY.minusDays(1), ComicRetrievalStatus.NETWORK_ERROR,
                OffsetDateTime.parse("2026-10-08T03:00:00Z")));
        ComicRetrievalRecord backfill = failure(1, "Daily", TODAY.minusDays(20), ComicRetrievalStatus.RATE_LIMITED,
                OffsetDateTime.parse("2026-10-08T05:00:00Z"));
        ComicRetrievalRecord latest = failure(1, "Daily", TODAY.minusDays(2), ComicRetrievalStatus.PARSING_ERROR,
                OffsetDateTime.parse("2026-10-08T06:00:00Z"));
        records.add(backfill);
        records.add(latest);
        records.add(ComicRetrievalRecord.success(1, "Daily", TODAY.minusDays(3), "gocomics", 10L, 10L).toBuilder()
                .attemptedAt(OffsetDateTime.parse("2026-10-08T07:00:00Z")).build());

        RetrievalHealth health = service.getHealth(7, 20);

        assertThat(health.todaysErrors()).extracting(e -> e.record()).containsExactly(latest, backfill);
        assertThat(health.todaysErrors()).extracting(e -> e.recovered()).containsExactly(false, true);
    }

    @Test
    void todaysErrorsAreLimited() {
        comic(1, "Daily", "gocomics", null);
        for (int i = 1; i <= 5; i++) {
            records.add(failure(1, "Daily", TODAY.minusDays(i), ComicRetrievalStatus.NETWORK_ERROR,
                    OffsetDateTime.parse("2026-10-08T1" + i + ":00:00Z")));
        }

        assertThat(service.getHealth(7, 2).todaysErrors()).hasSize(2);
    }

    @Test
    void sourcesCountTodaysRecordsAndListEveryActiveSource() {
        comic(1, "A", "gocomics", null);
        comic(2, "B", "gocomics", null);
        comic(3, "C", "comicskingdom", null);
        comic(4, "D", "gocomics", null);
        comic(5, "E", "gocomics", null);
        records.add(ComicRetrievalRecord.success(1, "A", TODAY, "gocomics", 10L, 10L));
        records.add(failure(2, "B", TODAY, ComicRetrievalStatus.RATE_LIMITED, TODAY));
        records.add(failure(4, "D", TODAY, ComicRetrievalStatus.COMIC_UNAVAILABLE, TODAY));
        records.add(failure(5, "E", TODAY, ComicRetrievalStatus.NETWORK_ERROR, TODAY));
        records.add(failure(5, "E", TODAY.minusDays(1), ComicRetrievalStatus.NETWORK_ERROR, TODAY.minusDays(1)));

        RetrievalHealth health = service.getHealth(7, 20);

        assertThat(health.sources()).containsExactly(
                new RetrievalHealthService.SourceRetrievalHealth("comicskingdom", 0, 0, 0, 0),
                new RetrievalHealthService.SourceRetrievalHealth("gocomics", 1, 1, 1, 1));
    }

    @Test
    void windowIsCappedAtRetentionAndComicsSortByName() {
        comic(2, "beta", "gocomics", null);
        comic(1, "Alpha", "gocomics", null);

        RetrievalHealth health = service.getHealth(365, 20);

        assertThat(health.targetDate()).isEqualTo(TODAY);
        assertThat(health.lastRun()).isSameAs(lastRun);
        assertThat(health.comics()).extracting(ComicRetrievalHealth::comicName).containsExactly("Alpha", "beta");
        assertThat(health.comics().getFirst().days()).hasSize(30);
        assertThat(health.comics().getFirst().days().getFirst().date()).isEqualTo(TODAY.minusDays(29));
    }

    @Test
    void noRunYetLeavesTodayPending() {
        comic(1, "Daily", "gocomics", null);
        lastRun = null;

        RetrievalHealth health = service.getHealth(7, 20);

        assertThat(health.lastRun()).isNull();
        assertThat(dayOf(health.comics().getFirst(), TODAY).outcome()).isEqualTo(DayOutcome.PENDING);
    }

    private ComicItem comic(int id, String name, String source, List<DayOfWeek> publicationDays) {
        ComicItem comic = ComicItem.builder().id(id).name(name).source(source).publicationDays(publicationDays).build();
        comics.add(comic);
        return comic;
    }

    private static ComicRetrievalRecord failure(int comicId, String name, LocalDate date, ComicRetrievalStatus status,
            LocalDate attemptedOn) {
        return failure(comicId, name, date, status, attemptedOn.atTime(8, 0).atZone(ZONE).toOffsetDateTime());
    }

    private static ComicRetrievalRecord failure(int comicId, String name, LocalDate date, ComicRetrievalStatus status,
            OffsetDateTime attemptedAt) {
        return ComicRetrievalRecord.failure(comicId, name, date, "gocomics", status, status.name(), 10L, null).toBuilder()
                .attemptedAt(attemptedAt.withOffsetSameInstant(ZoneOffset.UTC)).build();
    }

    private static BatchExecutionSummary run(LocalDate day, boolean finished) {
        OffsetDateTime start = day.atTime(7, 0).atZone(ZONE).toOffsetDateTime();
        return BatchExecutionSummary.builder().executionId(1L).jobName(RetrievalHealthService.DAILY_JOB)
                .status(finished ? "COMPLETED" : "STARTED").startTime(start).endTime(finished ? start.plusMinutes(20) : null).build();
    }

    private static List<LocalDate> range(LocalDate from, LocalDate to) {
        return from.datesUntil(to.plusDays(1)).toList();
    }

    private static ComicRetrievalHealth only(RetrievalHealth health) {
        assertThat(health.comics()).hasSize(1);
        return health.comics().getFirst();
    }

    private static RetrievalDay dayOf(ComicRetrievalHealth health, LocalDate date) {
        return health.days().stream().filter(d -> d.date().equals(date)).findFirst().orElseThrow();
    }
}
