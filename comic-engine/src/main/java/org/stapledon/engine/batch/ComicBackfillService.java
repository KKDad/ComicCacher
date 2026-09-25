package org.stapledon.engine.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.engine.downloader.DownloaderFacade;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.engine.storage.ComicIndexService;

/**
 * Service for identifying missing comic strips that need to be backfilled.
 * <p>
 * Intelligently determines which dates to scan based on:
 * <ul>
 * <li>Comic's known publication date range (oldest to newest)</li>
 * <li>Comic's publication days schedule</li>
 * <li>Whether the comic is active or discontinued</li>
 * <li>Source-specific per-run budgets and history depth</li>
 * <li>What earlier runs learned: given-up dates and how far back each source serves strips</li>
 * <li>What strips are already cached</li>
 * </ul>
 * <p>
 * This service pre-filters comics once upfront rather than logging skip
 * messages
 * during each date iteration, significantly reducing log noise.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComicBackfillService {

    private final ManagementFacade managementFacade;
    private final ComicStorageFacade storageFacade;
    private final BackfillConfigurationService config;
    private final DownloaderFacade downloaderFacade;
    private final ComicIndexService comicIndexService;
    private final BackfillStateService backfillState;

    /**
     * Sealed interface representing a backfill task for either date-based or indexed comics.
     */
    public sealed interface BackfillTask permits DateBackfillTask, StripBackfillTask {

        /**
         * Returns the comic this task is for.
         */
        ComicItem comic();
    }

    /**
     * Represents a date that needs to be backfilled for a daily comic.
     */
    public record DateBackfillTask(ComicItem comic, LocalDate date) implements BackfillTask {
    }

    /**
     * Represents a strip number that needs to be backfilled for an indexed comic.
     */
    public record StripBackfillTask(ComicItem comic, int stripNumber) implements BackfillTask {
    }

    /**
     * Scans all eligible comics and identifies missing strips with no source filter.
     */
    public List<BackfillTask> findMissingStrips() {
        return findMissingStrips(null);
    }

    /**
     * Picks this run's backfill tasks. When sourceFilter is non-null and not "ALL", only comics from that source are considered.
     * <p>
     * For each source, within its per-run budget ({@code max-per-run}, further capped by what is left of {@code max-per-day}):
     * <ol>
     * <li><b>Recent pass:</b> the last {@code recent-days} days, newest first, across every comic (every comic's today, then every comic's yesterday, and so on).
     * These are the strips most likely to fall behind a source's history paywall, so they go first.</li>
     * <li><b>History pass:</b> older gaps, round-robin one per comic per round, newest first, so no comic takes the whole budget.</li>
     * </ol>
     * Both passes honour the publication-day schedule, the source's {@code max-days-back}, the comic's known oldest date, and what
     * {@link BackfillStateService} has learned (given-up dates and history horizons). The history pass also stops a comic after
     * {@code max-consecutive-failures} missing strips in a row (it likely didn't exist that far back).
     *
     * @return backfill tasks in the order they should run
     */
    public List<BackfillTask> findMissingStrips(String sourceFilter) {
        return selectTasks(sourceFilter, true, Integer.MAX_VALUE);
    }

    /**
     * True when a run would have anything to do. Uses only local storage and backfill state: it never calls a source, so it can gate scheduled runs cheaply.
     * Indexed comics whose latest strip number isn't known yet are not counted, because finding it needs a web request.
     */
    public boolean hasMissingStrips(String sourceFilter) {
        return !selectTasks(sourceFilter, false, 1).isEmpty();
    }

    private List<BackfillTask> selectTasks(String sourceFilter, boolean allowNetwork, int stopAfter) {
        if (allowNetwork) {
            log.info("Scanning for missing comic strips (sourceFilter={})", sourceFilter);
        }

        List<ComicItem> allComics = managementFacade.getAllComics();

        // Pre-filter comics - only active comics with valid, enabled sources
        List<ComicItem> eligibleComics = filterEligibleComics(allComics, sourceFilter);

        if (allowNetwork) {
            log.info("Found {} eligible comics out of {} total (filtered {} inactive/invalid)",
                    eligibleComics.size(), allComics.size(),
                    allComics.size() - eligibleComics.size());
        }

        Map<String, List<ComicItem>> comicsBySource = new LinkedHashMap<>();
        for (ComicItem comic : eligibleComics) {
            comicsBySource.computeIfAbsent(comic.getSource(), k -> new ArrayList<>()).add(comic);
        }

        List<BackfillTask> allTasks = new ArrayList<>();
        int sourcesWithTasks = 0;
        for (Map.Entry<String, List<ComicItem>> entry : comicsBySource.entrySet()) {
            String source = entry.getKey();
            int budget = Math.min(remainingBudget(source, allowNetwork), stopAfter - allTasks.size());
            if (budget <= 0) {
                continue;
            }

            List<BackfillTask> sourceTasks = downloaderFacade.isIndexedSource(source)
                    ? selectIndexedTasks(entry.getValue(), budget, allowNetwork)
                    : selectDateTasks(entry.getValue(), source, budget);

            if (!sourceTasks.isEmpty()) {
                sourcesWithTasks++;
                if (allowNetwork) {
                    logSourceTasks(source, sourceTasks, budget);
                }
            }
            allTasks.addAll(sourceTasks);
            if (allTasks.size() >= stopAfter) {
                break;
            }
        }

        if (allowNetwork) {
            log.info("Total missing strips found: {} (across {} sources)", allTasks.size(), sourcesWithTasks);
        }
        return allTasks;
    }

    /**
     * This run's budget for a source: {@code max-per-run}, capped by what is left of the optional {@code max-per-day} ceiling.
     */
    private int remainingBudget(String source, boolean logLimits) {
        int budget = config.getMaxPerRunForSource(source);
        int perDay = config.getMaxPerDayForSource(source);
        if (perDay > 0) {
            int left = perDay - backfillState.attemptsToday(source);
            if (left <= 0 && logLimits) {
                log.info("Skipping source '{}' - reached its daily limit of {} backfill downloads", source, perDay);
            }
            budget = Math.min(budget, left);
        }
        return budget;
    }

    private List<BackfillTask> selectIndexedTasks(List<ComicItem> comics, int budget, boolean allowNetwork) {
        List<BackfillTask> tasks = new ArrayList<>();
        for (ComicItem comic : comics) {
            if (tasks.size() >= budget) {
                break;
            }
            tasks.addAll(scanIndexedComicForMissingStrips(comic, budget - tasks.size(), allowNetwork));
        }
        return tasks;
    }

    /**
     * Recent pass across every comic, then a round-robin history pass (see {@link #findMissingStrips(String)}).
     */
    private List<BackfillTask> selectDateTasks(List<ComicItem> comics, String source, int budget) {
        LocalDate today = LocalDate.now();
        LocalDate oldestRecent = today.minusDays(config.getRecentDaysForSource(source) - 1L);

        List<ComicScan> scans = new ArrayList<>();
        for (ComicItem comic : comics) {
            DateRange range = calculateScanRange(comic);
            if (range == null) {
                log.debug("No valid scan range for comic '{}'", comic.getName());
                continue;
            }
            scans.add(new ComicScan(comic, range));
        }

        List<BackfillTask> tasks = new ArrayList<>();

        // Recent pass: newest dates first, across every comic
        for (LocalDate date = today; !date.isBefore(oldestRecent) && tasks.size() < budget; date = date.minusDays(1)) {
            for (ComicScan scan : scans) {
                if (tasks.size() >= budget) {
                    break;
                }
                if (scan.isMissing(date)) {
                    tasks.add(new DateBackfillTask(scan.comic, date));
                }
            }
        }

        // History pass: one older gap per comic per round
        LocalDate historyStart = oldestRecent.minusDays(1);
        List<ComicScan> active = new ArrayList<>();
        for (ComicScan scan : scans) {
            if (scan.startHistory(historyStart)) {
                active.add(scan);
            }
        }
        while (tasks.size() < budget && !active.isEmpty()) {
            Iterator<ComicScan> it = active.iterator();
            while (it.hasNext() && tasks.size() < budget) {
                ComicScan scan = it.next();
                LocalDate next = scan.nextHistoryGap();
                if (next == null) {
                    it.remove();
                } else {
                    tasks.add(new DateBackfillTask(scan.comic, next));
                }
            }
        }
        return tasks;
    }

    private void logSourceTasks(String source, List<BackfillTask> tasks, int budget) {
        Map<String, Integer> perComic = new LinkedHashMap<>();
        for (BackfillTask task : tasks) {
            perComic.merge(task.comic().getName(), 1, Integer::sum);
        }
        int running = 0;
        for (Map.Entry<String, Integer> e : perComic.entrySet()) {
            running += e.getValue();
            log.info("Found {} missing strips for {} (source '{}': {}/{})", e.getValue(), e.getKey(), source, running, budget);
        }
    }

    /**
     * Walks one comic's dates for the recent and history passes, skipping what is already cached, not published that day, or given up.
     */
    private final class ComicScan {
        private final ComicItem comic;
        private final DateRange range;
        private final int maxConsecutive = config.getMaxConsecutiveFailures();
        private LocalDate cursor;
        private int consecutiveMissing;
        private boolean exhausted;

        private ComicScan(ComicItem comic, DateRange range) {
            this.comic = comic;
            this.range = range;
        }

        /**
         * Whether the recent pass should backfill this date.
         */
        boolean isMissing(LocalDate date) {
            return !date.isAfter(range.start()) && !date.isBefore(range.end()) && shouldCheckDate(comic, date)
                    && !storageFacade.comicStripExists(ComicIdentifier.from(comic), date)
                    && !backfillState.isGivenUp(comic, date);
        }

        /**
         * Positions the history walk at {@code from} (or the range start, if older). Returns false when there is no history to walk.
         */
        boolean startHistory(LocalDate from) {
            cursor = from.isAfter(range.start()) ? range.start() : from;
            return !cursor.isBefore(range.end());
        }

        /**
         * The next older missing date, or null when the walk is done.
         */
        LocalDate nextHistoryGap() {
            while (!exhausted && !cursor.isBefore(range.end())) {
                LocalDate date = cursor;
                cursor = cursor.minusDays(1);
                if (!shouldCheckDate(comic, date)) {
                    continue;
                }
                if (storageFacade.comicStripExists(ComicIdentifier.from(comic), date)) {
                    consecutiveMissing = 0;
                    continue;
                }
                consecutiveMissing++;
                if (consecutiveMissing >= maxConsecutive) {
                    // Too many missing in a row: the comic likely didn't exist this far back
                    log.debug("Stopping scan for {} at {} - {} consecutive missing strips (comic likely didn't exist this far back)",
                            comic.getName(), date, consecutiveMissing);
                    exhausted = true;
                }
                if (!backfillState.isGivenUp(comic, date)) {
                    return date;
                }
            }
            return null;
        }
    }

    /**
     * Filters comics to only include those eligible for backfill.
     * <p>
     * A comic is eligible if:
     * <ul>
     * <li>It is active (not discontinued)</li>
     * <li>It has a valid source configured</li>
     * <li>Its source is enabled for backfill</li>
     * <li>Its source matches the sourceFilter (if provided and not "ALL")</li>
     * </ul>
     */
    private List<ComicItem> filterEligibleComics(List<ComicItem> comics, String sourceFilter) {
        List<ComicItem> eligible = new ArrayList<>();
        boolean hasSourceFilter = sourceFilter != null && !"ALL".equalsIgnoreCase(sourceFilter);

        for (ComicItem comic : comics) {
            if (!comic.isActive()) {
                log.debug("Excluding inactive/discontinued comic: {}", comic.getName());
                continue;
            }

            if (comic.getSource() == null || comic.getSource().isEmpty()) {
                log.debug("Excluding comic with no source: {}", comic.getName());
                continue;
            }

            if (!config.isSourceEnabled(comic.getSource())) {
                log.debug("Excluding comic {} - source '{}' disabled for backfill",
                        comic.getName(), comic.getSource());
                continue;
            }

            if (hasSourceFilter && !sourceFilter.equalsIgnoreCase(comic.getSource())) {
                log.debug("Excluding comic {} - source '{}' doesn't match filter '{}'",
                        comic.getName(), comic.getSource(), sourceFilter);
                continue;
            }

            eligible.add(comic);
        }

        return eligible;
    }

    /**
     * Calculates the effective date range to scan for a specific comic.
     * <p>
     * Takes into account:
     * <ul>
     * <li>Today's date (don't scan future dates)</li>
     * <li>Comic's known oldest date (don't scan before comic existed)</li>
     * <li>Source-specific max-days-back limit</li>
     * <li>Comic's newest date (for discontinued comics)</li>
     * <li>The comic's or source's learned history horizon</li>
     * </ul>
     *
     * @param comic the comic to calculate range for
     * @return DateRange to scan (start is most recent, end is oldest), or null if
     *         no valid range
     */
    private DateRange calculateScanRange(ComicItem comic) {

        // Start from today (scan backwards from most recent)
        LocalDate scanStart = LocalDate.now();

        // If comic is discontinued and has a newest date, don't scan after it
        if (!comic.isActive() && comic.getNewest() != null
                && comic.getNewest().isBefore(scanStart)) {
            scanStart = comic.getNewest();
        }

        // Calculate the earliest allowed date based on source limits

        // End at the earliest of: comic's oldest date OR source limit
        LocalDate scanEnd = config.getEarliestAllowedDate(comic.getSource());

        if (comic.getOldest() != null && comic.getOldest().isAfter(scanEnd)) {
            scanEnd = comic.getOldest();
        }

        // Don't go back past what the source has been learned to serve
        Optional<LocalDate> horizon = backfillState.horizonFloor(comic);
        if (horizon.isPresent() && !horizon.get().isBefore(scanEnd)) {
            scanEnd = horizon.get().plusDays(1);
        }

        // Validate the range makes sense (start should be after or equal to end)
        if (scanStart.isBefore(scanEnd)) {
            return null;
        }

        return new DateRange(scanStart, scanEnd);
    }

    /**
     * Represents a date range for scanning (start is most recent, end is oldest).
     */
    private record DateRange(LocalDate start, LocalDate end) {
    }

    /**
     * Scans an indexed comic for missing strips by iterating strip numbers
     * backwards from the last known strip number.
     *
     * @param comic        the indexed comic to scan
     * @param maxTasks     maximum number of tasks to return
     * @param allowNetwork whether an unknown latest strip number may be discovered with a web request
     * @return list of backfill tasks for missing strips
     */
    private List<BackfillTask> scanIndexedComicForMissingStrips(ComicItem comic, int maxTasks, boolean allowNetwork) {
        List<BackfillTask> tasks = new ArrayList<>();

        Integer lastStrip = comic.getLastStripNumber();
        Integer firstStrip = comic.getFirstStripNumber();

        if ((lastStrip == null || lastStrip <= 0) && !allowNetwork) {
            log.debug("Latest strip number for indexed comic '{}' is unknown; finding it needs a web request", comic.getName());
            return tasks;
        }
        if (lastStrip == null || lastStrip <= 0) {
            log.info("Auto-discovering latest strip number for indexed comic '{}'", comic.getName());
            var result = managementFacade.downloadLatestIndexedComic(comic);
            if (result.isPresent() && result.get().isSuccessful() && result.get().getStripNumber() != null) {
                lastStrip = result.get().getStripNumber();
                log.info("Discovered lastStripNumber={} for '{}'", lastStrip, comic.getName());
            } else {
                log.warn("Could not auto-discover latest strip for '{}'", comic.getName());
                return tasks;
            }
        }

        // Load already-downloaded strip numbers to avoid wasteful re-downloads
        Set<Integer> downloadedStrips = comicIndexService.getDownloadedStripNumbers(
                comic.getId(), comic.getName());

        int startStrip = downloadedStrips.contains(lastStrip) ? lastStrip - 1 : lastStrip;
        int endStrip = firstStrip != null ? firstStrip : 1;
        int consecutiveMissing = 0;
        int maxConsecutive = config.getMaxConsecutiveFailures();

        for (int stripNum = startStrip; stripNum >= endStrip && tasks.size() < maxTasks; stripNum--) {
            if (downloadedStrips.contains(stripNum)) {
                consecutiveMissing = 0;
                continue;
            }

            tasks.add(new StripBackfillTask(comic, stripNum));
            consecutiveMissing++;

            if (consecutiveMissing >= maxConsecutive) {
                log.debug("Stopping scan for {} at strip #{} - {} consecutive missing strips",
                        comic.getName(), stripNum, consecutiveMissing);
                break;
            }
        }

        return tasks;
    }

    /**
     * Determines if we should check for a comic on this date based on
     * publication schedule.
     *
     * @param comic the comic to check
     * @param date  the date to check
     * @return true if the comic potentially published on this date
     */
    private boolean shouldCheckDate(ComicItem comic, LocalDate date) {
        // If no publication days specified, assume daily publication
        if (comic.getPublicationDays() == null || comic.getPublicationDays().isEmpty()) {
            return true;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return comic.getPublicationDays().contains(dayOfWeek);
    }
}
