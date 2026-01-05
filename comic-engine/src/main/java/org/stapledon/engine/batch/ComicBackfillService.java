package org.stapledon.engine.batch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.engine.management.ManagementFacade;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for identifying missing comic strips that need to be backfilled.
 * Scans a target year for each comic and builds a list of dates with missing
 * strips.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComicBackfillService {

    private final ManagementFacade managementFacade;
    private final ComicStorageFacade storageFacade;

    @Value("${batch.comic-backfill.target-year:2025}")
    private int targetYear;

    @Value("${batch.comic-backfill.max-consecutive-failures:3}")
    private int maxConsecutiveFailures;

    /**
     * Represents a date that needs to be backfilled for a specific comic.
     */
    public record BackfillTask(ComicItem comic, LocalDate date) {
    }

    /**
     * Scans all enabled comics for the target year and identifies missing strips.
     * Scans backwards in time from today (or end of year) to the start of the year.
     *
     * @return List of backfill tasks (comic + date pairs) in reverse chronological
     *         order
     */
    public List<BackfillTask> findMissingStrips() {
        log.info("Scanning for missing comic strips in year {}", targetYear);

        List<ComicItem> comics = managementFacade.getAllComics();
        List<BackfillTask> backfillTasks = new ArrayList<>();

        LocalDate yearStart = LocalDate.of(targetYear, 1, 1);
        LocalDate yearEnd = LocalDate.of(targetYear, 12, 31);
        LocalDate today = LocalDate.now();

        // Don't scan future dates - start from today or end of year, whichever is
        // earlier
        LocalDate scanStart = yearEnd.isAfter(today) ? today : yearEnd;

        log.info("Backfill date range: scanning from {} backwards to {} (today: {}, yearEnd: {})",
                scanStart, yearStart, today, yearEnd);

        if (scanStart.isAfter(today)) {
            log.error("⚠️ FUTURE DATE BUG: scanStart ({}) is AFTER today ({})", scanStart, today);
        }

        for (ComicItem comic : comics) {
            if (!comic.isActive()) {
                log.debug("Skipping inactive comic: {}", comic.getName());
                continue;
            }

            if (comic.getSource() == null || comic.getSource().isEmpty()) {
                log.debug("Skipping comic with no source: {}", comic.getName());
                continue;
            }

            log.info("Scanning {} for missing strips (backwards from {} to {})",
                    comic.getName(), scanStart, yearStart);

            List<BackfillTask> comicTasks = scanComicForMissingStrips(comic, scanStart, yearStart);
            backfillTasks.addAll(comicTasks);

            log.info("Found {} missing strips for {}", comicTasks.size(), comic.getName());
        }

        log.info("Total missing strips found: {}", backfillTasks.size());
        return backfillTasks;
    }

    /**
     * Scans a single comic for missing strips in the date range.
     * Scans backwards in time from start to end.
     * Stops early if too many consecutive missing strips are found (comic didn't
     * exist yet).
     */
    private List<BackfillTask> scanComicForMissingStrips(
            ComicItem comic,
            LocalDate start,
            LocalDate end) {

        List<BackfillTask> tasks = new ArrayList<>();
        int consecutiveMissing = 0;
        LocalDate date = start;

        // Scan backwards in time
        while (!date.isBefore(end)) {
            // Check if this comic publishes on this day of week
            if (shouldCheckDate(comic, date)) {
                boolean exists = storageFacade.comicStripExists(
                        comic.getId(),
                        comic.getName(),
                        date);

                if (!exists) {
                    tasks.add(new BackfillTask(comic, date));
                    consecutiveMissing++;

                    // Stop if we've hit too many consecutive missing strips
                    // This likely means the comic didn't exist this far back
                    if (consecutiveMissing >= maxConsecutiveFailures) {
                        log.info("Stopping scan for {} at {} - {} consecutive missing strips "
                                + "(comic likely didn't exist this far back)",
                                comic.getName(), date, consecutiveMissing);
                        break;
                    }
                } else {
                    consecutiveMissing = 0; // Reset counter when we find a strip
                }
            }

            date = date.minusDays(1); // Go backwards in time
        }

        return tasks;
    }

    /**
     * Determines if we should check for a comic on this date based on publication
     * schedule.
     */
    private boolean shouldCheckDate(ComicItem comic, LocalDate date) {
        // If no publication days specified, check every day
        if (comic.getPublicationDays() == null || comic.getPublicationDays().isEmpty()) {
            return true;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return comic.getPublicationDays().contains(dayOfWeek);
    }
}
