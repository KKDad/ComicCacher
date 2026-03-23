package org.stapledon.engine.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <li>Source-specific rate limits and history depth</li>
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
     * Scans eligible comics and identifies missing strips, respecting
     * source-specific rate limits. When sourceFilter is non-null and not "ALL",
     * only comics from that source are considered.
     * <p>
     * This method filters comics upfront and only scans dates where the comic
     * is expected to have published. The filtering considers:
     * <ul>
     * <li>Active status (skips discontinued comics)</li>
     * <li>Source availability and enablement</li>
     * <li>Source-specific rate limits (max per day)</li>
     * <li>Source-specific history depth (max days back)</li>
     * <li>Known publication date range</li>
     * <li>Publication day schedule</li>
     * </ul>
     *
     * @return List of backfill tasks (comic + date pairs) limited by per-source
     *         daily limits
     */
    public List<BackfillTask> findMissingStrips(String sourceFilter) {
        log.info("Scanning for missing comic strips (sourceFilter={})", sourceFilter);

        List<ComicItem> allComics = managementFacade.getAllComics();

        // Pre-filter comics - only active comics with valid, enabled sources
        List<ComicItem> eligibleComics = filterEligibleComics(allComics, sourceFilter);

        log.info("Found {} eligible comics out of {} total (filtered {} inactive/invalid)",
                eligibleComics.size(), allComics.size(),
                allComics.size() - eligibleComics.size());

        // Track counts per source for rate limiting
        Map<String, Integer> taskCountBySource = new HashMap<>();
        List<BackfillTask> allTasks = new ArrayList<>();

        for (ComicItem comic : eligibleComics) {
            String source = comic.getSource();

            // Check if we've hit the daily limit for this source
            int currentCount = taskCountBySource.getOrDefault(source, 0);
            int maxPerDay = config.getMaxPerDayForSource(source);

            if (currentCount >= maxPerDay) {
                log.debug("Skipping {} - reached daily limit of {} for source '{}'",
                        comic.getName(), maxPerDay, source);
                continue;
            }

            int remainingQuota = maxPerDay - currentCount;
            List<BackfillTask> comicTasks;

            // Handle indexed comics differently
            if (downloaderFacade.isIndexedSource(source)) {
                comicTasks = scanIndexedComicForMissingStrips(comic, remainingQuota);
            } else {
                // Calculate the effective scan range for this specific comic
                DateRange scanRange = calculateScanRange(comic);

                if (scanRange == null) {
                    log.debug("No valid scan range for comic '{}'", comic.getName());
                    continue;
                }

                log.debug("Scanning {} ({} to {})",
                        comic.getName(), scanRange.start(), scanRange.end());

                // Scan for missing strips, respecting remaining quota
                comicTasks = scanComicForMissingStrips(
                        comic, scanRange.start(), scanRange.end(), remainingQuota);
            }

            allTasks.addAll(comicTasks);
            taskCountBySource.put(source, currentCount + comicTasks.size());

            if (!comicTasks.isEmpty()) {
                log.info("Found {} missing strips for {} (source '{}': {}/{})",
                        comicTasks.size(), comic.getName(), source,
                        currentCount + comicTasks.size(), maxPerDay);
            }
        }

        log.info("Total missing strips found: {} (across {} sources)",
                allTasks.size(), taskCountBySource.size());
        return allTasks;
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
     * Scans a single comic for missing strips in the date range.
     * <p>
     * Scans backwards in time from start to end.
     * Respects the comic's publication day schedule and quota limit.
     * Stops early if too many consecutive missing strips are found.
     *
     * @param comic    the comic to scan
     * @param start    the starting date (most recent)
     * @param end      the ending date (oldest)
     * @param maxTasks maximum number of tasks to return
     * @return list of backfill tasks for missing strips
     */
    private List<BackfillTask> scanComicForMissingStrips(
            ComicItem comic,
            LocalDate start,
            LocalDate end,
            int maxTasks) {

        List<BackfillTask> tasks = new ArrayList<>();
        int consecutiveMissing = 0;
        LocalDate date = start;
        int maxConsecutive = config.getMaxConsecutiveFailures();

        // Scan backwards in time
        while (!date.isBefore(end) && tasks.size() < maxTasks) {
            // Check if this comic publishes on this day of week
            if (shouldCheckDate(comic, date)) {
                boolean exists = storageFacade.comicStripExists(
                        ComicIdentifier.from(comic),
                        date);

                if (!exists) {
                    tasks.add(new DateBackfillTask(comic, date));
                    consecutiveMissing++;

                    // Stop if we've hit too many consecutive missing strips
                    // This likely means the comic didn't exist this far back
                    if (consecutiveMissing >= maxConsecutive) {
                        log.debug("Stopping scan for {} at {} - {} consecutive missing strips "
                                + "(comic likely didn't exist this far back)",
                                comic.getName(), date, consecutiveMissing);
                        break;
                    }
                } else {
                    consecutiveMissing = 0; // Reset counter when we find a strip
                }
            }

            date = date.minusDays(1);
        }

        return tasks;
    }

    /**
     * Scans an indexed comic for missing strips by iterating strip numbers
     * backwards from the last known strip number.
     *
     * @param comic    the indexed comic to scan
     * @param maxTasks maximum number of tasks to return
     * @return list of backfill tasks for missing strips
     */
    private List<BackfillTask> scanIndexedComicForMissingStrips(ComicItem comic, int maxTasks) {
        List<BackfillTask> tasks = new ArrayList<>();

        Integer lastStrip = comic.getLastStripNumber();
        Integer firstStrip = comic.getFirstStripNumber();

        if (lastStrip == null || lastStrip <= 0) {
            log.debug("No last strip number known for indexed comic '{}'", comic.getName());
            return tasks;
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
