package org.stapledon.core.comic.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.common.service.RetrievalStatusService;
import org.stapledon.engine.batch.BatchJobMonitoringService;
import org.stapledon.engine.batch.dto.BatchExecutionSummary;
import org.stapledon.engine.downloader.DownloaderFacade;
import org.stapledon.engine.management.ManagementFacade;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * Builds the retrieval-status page's view: each comic's final result per day, judged by the strips on disk first and the
 * retrieval records second, plus the latest daily run, today's results by source and today's errors. Everything it reads is
 * held in memory (the comic config, the date indexes, the retrieval records and the batch history), so a 30-day window costs
 * no storage reads.
 */
@Slf4j
@Service
public class RetrievalHealthService {
    /** The scheduled daily download job; its latest run decides whether today is still pending. */
    public static final String DAILY_JOB = "ComicDownloadJob";

    private final ManagementFacade managementFacade;
    private final ComicStorageFacade storageFacade;
    private final RetrievalStatusService retrievalStatusService;
    private final BatchJobMonitoringService monitoringService;
    private final DownloaderFacade downloaderFacade;
    private final Clock clock;
    private final int retentionDays;

    public RetrievalHealthService(ManagementFacade managementFacade, ComicStorageFacade storageFacade,
            RetrievalStatusService retrievalStatusService, BatchJobMonitoringService monitoringService,
            DownloaderFacade downloaderFacade, Clock clock, @Value("${batch.record-purge.days-to-keep:30}") int retentionDays) {
        this.managementFacade = managementFacade;
        this.storageFacade = storageFacade;
        this.retrievalStatusService = retrievalStatusService;
        this.monitoringService = monitoringService;
        this.downloaderFacade = downloaderFacade;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    /**
     * A comic's final result on a day.
     */
    public enum DayOutcome {
        ON_DISK, MISSING, OFF_DAY, PENDING
    }

    /**
     * When a comic is expected to have a strip: active, dated (not numbered), on a publication day, and not before its first
     * strip on disk.
     */
    record Schedule(boolean active, boolean indexed, List<DayOfWeek> publicationDays, LocalDate firstStrip) {
        boolean publishesOn(LocalDate date) {
            return publicationDays == null || publicationDays.isEmpty() || publicationDays.contains(date.getDayOfWeek());
        }

        boolean expects(LocalDate date) {
            return active && !indexed && (firstStrip == null || !date.isBefore(firstStrip)) && publishesOn(date);
        }
    }

    public record RetrievalHealth(LocalDate targetDate, BatchExecutionSummary lastRun, List<SourceRetrievalHealth> sources,
            List<RetrievalError> todaysErrors, List<ComicRetrievalHealth> comics) {
    }

    public record SourceRetrievalHealth(String source, int success, int unavailable, int rateLimited, int failed) {
    }

    public record RetrievalError(ComicRetrievalRecord record, boolean recovered) {
    }

    public record ComicRetrievalHealth(int comicId, String comicName, String source, boolean enabled, boolean active,
            boolean indexed, List<DayOfWeek> publicationDays, LocalDate newest, LocalDate expectedLatest, boolean stale,
            int missingStreak, ComicRetrievalRecord latestError, List<RetrievalDay> days) {
    }

    public record RetrievalDay(LocalDate date, DayOutcome outcome, boolean recovered, ComicRetrievalRecord record) {
    }

    /**
     * Builds the view for the last {@code days} days (capped at the record retention), with up to {@code errorLimit} of today's
     * errors.
     */
    public RetrievalHealth getHealth(int days, int errorLimit) {
        int window = Math.clamp(days, 1, Math.max(1, retentionDays));
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(window - 1L);
        ZoneId zone = clock.getZone();

        BatchExecutionSummary lastRun = monitoringService.getRecentJobExecutions(DAILY_JOB, 1).stream().findFirst().orElse(null);
        boolean runFinishedToday = runFinishedOn(lastRun, today, zone);

        List<ComicRetrievalRecord> records = retrievalStatusService.getRetrievalRecords(null, null, null, null, Integer.MAX_VALUE);
        RecordIndex index = new RecordIndex(records);

        List<ComicItem> comics = managementFacade.getAllComics();
        Map<Integer, List<LocalDate>> datesByComic = new HashMap<>();
        Map<String, ComicItem> comicsByName = new HashMap<>();
        List<ComicRetrievalHealth> comicHealth = new ArrayList<>(comics.size());
        for (ComicItem comic : comics) {
            List<LocalDate> available = storageFacade.getAvailableDates(ComicIdentifier.from(comic));
            datesByComic.put(comic.getId(), available);
            comicsByName.put(comic.getName(), comic);
            comicHealth.add(buildComic(comic, available, index.forComic(comic), from, today, runFinishedToday));
        }
        comicHealth.sort(Comparator.comparing(ComicRetrievalHealth::comicName, String.CASE_INSENSITIVE_ORDER));

        // Failures only: a strip the source didn't have (COMIC_UNAVAILABLE) isn't an error, and the grid shows it as missing anyway
        List<RetrievalError> todaysErrors = records.stream()
                .filter(RetrievalHealthService::isFailure)
                .filter(r -> r.getAttemptedAt() != null && r.getAttemptedAt().atZoneSameInstant(zone).toLocalDate().equals(today))
                .sorted(Comparator.comparing(ComicRetrievalRecord::getAttemptedAt).reversed())
                .limit(Math.max(0, errorLimit))
                .map(r -> new RetrievalError(r, isOnDisk(r, datesByComic, comicsByName)))
                .toList();

        List<SourceRetrievalHealth> sources = sourceHealth(comics, records, today);

        log.debug("Retrieval health: {} comics over {} days, {} errors today, last run {}", comicHealth.size(), window,
                todaysErrors.size(), lastRun != null ? lastRun.getStatus() : "none");
        return new RetrievalHealth(today, lastRun, sources, todaysErrors, comicHealth);
    }

    private ComicRetrievalHealth buildComic(ComicItem comic, List<LocalDate> available, Map<LocalDate, ComicRetrievalRecord> records,
            LocalDate from, LocalDate today, boolean runFinishedToday) {
        boolean indexed = comic.getSource() != null && downloaderFacade.isIndexedSource(comic.getSource());
        LocalDate firstStrip = available.isEmpty() ? null : available.getFirst();
        LocalDate newest = available.isEmpty() ? null : available.getLast();
        Schedule schedule = new Schedule(comic.isActive(), indexed, comic.getPublicationDays(), firstStrip);

        Set<LocalDate> onDisk = new HashSet<>();
        for (int i = available.size() - 1; i >= 0 && !available.get(i).isBefore(from); i--) {
            onDisk.add(available.get(i));
        }

        List<RetrievalDay> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
            ComicRetrievalRecord record = records.get(date);
            boolean present = onDisk.contains(date);
            DayOutcome outcome = outcome(date, present, record, schedule, today, runFinishedToday);
            boolean recovered = present && record != null && record.getStatus() != ComicRetrievalStatus.SUCCESS;
            days.add(new RetrievalDay(date, outcome, recovered, record));
        }

        ComicRetrievalRecord latestError = days.reversed().stream()
                .filter(d -> d.outcome() == DayOutcome.MISSING && d.record() != null)
                .map(RetrievalDay::record)
                .findFirst()
                .orElse(null);
        LocalDate expectedLatest = expectedLatest(schedule, today, runFinishedToday);
        boolean stale = expectedLatest != null && (newest == null || newest.isBefore(expectedLatest));

        return new ComicRetrievalHealth(comic.getId(), comic.getName(), comic.getSource(), comic.isEnabled(), comic.isActive(),
                indexed, comic.getPublicationDays(), newest, expectedLatest, stale, missingStreak(days), latestError, days);
    }

    /**
     * The final result for a day: on disk wins whatever the record says; then today before the run is pending; a numbered
     * source is missing only when an attempt failed; a dated comic is missing on any day it was expected.
     */
    static DayOutcome outcome(LocalDate date, boolean onDisk, ComicRetrievalRecord record, Schedule schedule, LocalDate today,
            boolean runFinishedToday) {
        if (onDisk) {
            return DayOutcome.ON_DISK;
        }
        if (date.equals(today) && !runFinishedToday) {
            return DayOutcome.PENDING;
        }
        if (schedule.indexed()) {
            return schedule.active() && isFailure(record) ? DayOutcome.MISSING : DayOutcome.OFF_DAY;
        }
        return schedule.expects(date) ? DayOutcome.MISSING : DayOutcome.OFF_DAY;
    }

    /**
     * Expected days in a row, counting back from the newest, with no strip on disk. Off days and a pending today don't break it.
     */
    static int missingStreak(List<RetrievalDay> days) {
        int streak = 0;
        for (RetrievalDay day : days.reversed()) {
            switch (day.outcome()) {
                case MISSING -> streak++;
                case ON_DISK -> {
                    return streak;
                }
                default -> {
                    // OFF_DAY and PENDING neither count nor break the streak
                }
            }
        }
        return streak;
    }

    /**
     * The latest day a strip should be on disk by now: the newest publication day up to today (yesterday while today's run is
     * pending), or null for a comic nothing is expected of.
     */
    static LocalDate expectedLatest(Schedule schedule, LocalDate today, boolean runFinishedToday) {
        if (!schedule.active() || schedule.indexed()) {
            return null;
        }
        LocalDate date = runFinishedToday ? today : today.minusDays(1);
        for (int i = 0; i < 7; i++, date = date.minusDays(1)) {
            if (schedule.publishesOn(date)) {
                return schedule.firstStrip() != null && date.isBefore(schedule.firstStrip()) ? null : date;
            }
        }
        return null;
    }

    /**
     * Whether the run started today (in the batch timezone) and has ended.
     */
    static boolean runFinishedOn(BatchExecutionSummary run, LocalDate today, ZoneId zone) {
        return run != null && run.getStartTime() != null && run.getEndTime() != null
                && run.getStartTime().atZoneSameInstant(zone).toLocalDate().equals(today);
    }

    private static boolean isFailure(ComicRetrievalRecord record) {
        return record != null && record.getStatus() != ComicRetrievalStatus.SUCCESS
                && record.getStatus() != ComicRetrievalStatus.COMIC_UNAVAILABLE;
    }

    private static boolean isOnDisk(ComicRetrievalRecord record, Map<Integer, List<LocalDate>> datesByComic,
            Map<String, ComicItem> comicsByName) {
        Integer comicId = record.getComicId();
        if (comicId == null) {
            ComicItem comic = comicsByName.get(record.getComicName());
            comicId = comic != null ? comic.getId() : null;
        }
        List<LocalDate> dates = comicId != null ? datesByComic.get(comicId) : null;
        return dates != null && dates.contains(record.getComicDate());
    }

    private static List<SourceRetrievalHealth> sourceHealth(List<ComicItem> comics, List<ComicRetrievalRecord> records, LocalDate today) {
        Map<String, Map<ComicRetrievalStatus, Long>> counts = new TreeMap<>();
        comics.stream()
                .filter(ComicItem::isActive)
                .map(ComicItem::getSource)
                .filter(Objects::nonNull)
                .forEach(source -> counts.putIfAbsent(source, Map.of()));
        records.stream()
                .filter(r -> today.equals(r.getComicDate()) && r.getSource() != null)
                .collect(Collectors.groupingBy(ComicRetrievalRecord::getSource,
                        Collectors.groupingBy(ComicRetrievalRecord::getStatus, Collectors.counting())))
                .forEach(counts::put);

        return counts.entrySet().stream()
                .map(e -> {
                    Map<ComicRetrievalStatus, Long> c = e.getValue();
                    long success = c.getOrDefault(ComicRetrievalStatus.SUCCESS, 0L);
                    long unavailable = c.getOrDefault(ComicRetrievalStatus.COMIC_UNAVAILABLE, 0L);
                    long rateLimited = c.getOrDefault(ComicRetrievalStatus.RATE_LIMITED, 0L);
                    long total = c.values().stream().mapToLong(Long::longValue).sum();
                    return new SourceRetrievalHealth(e.getKey(), (int) success, (int) unavailable, (int) rateLimited,
                            (int) (total - success - unavailable - rateLimited));
                })
                .toList();
    }

    /**
     * Records by comic id, falling back to the comic name for records written before the id was kept.
     */
    private static final class RecordIndex {
        private final Map<Integer, Map<LocalDate, ComicRetrievalRecord>> byId = new HashMap<>();
        private final Map<String, Map<LocalDate, ComicRetrievalRecord>> byName = new HashMap<>();

        RecordIndex(List<ComicRetrievalRecord> records) {
            for (ComicRetrievalRecord r : records) {
                if (r.getComicDate() == null) {
                    continue;
                }
                if (r.getComicId() != null) {
                    byId.computeIfAbsent(r.getComicId(), k -> new HashMap<>()).put(r.getComicDate(), r);
                } else if (r.getComicName() != null) {
                    byName.computeIfAbsent(r.getComicName(), k -> new HashMap<>()).put(r.getComicDate(), r);
                }
            }
        }

        Map<LocalDate, ComicRetrievalRecord> forComic(ComicItem comic) {
            Map<LocalDate, ComicRetrievalRecord> merged = new HashMap<>(byName.getOrDefault(comic.getName(), Map.of()));
            merged.putAll(byId.getOrDefault(comic.getId(), Map.of()));
            return merged;
        }
    }
}
