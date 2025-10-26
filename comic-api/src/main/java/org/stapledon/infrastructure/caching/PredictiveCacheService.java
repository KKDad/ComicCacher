package org.stapledon.infrastructure.caching;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.stapledon.common.config.CaffeineCacheProperties;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.management.ManagementFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service that provides predictive cache warming for comic navigation.
 * When a user navigates to a comic, this service asynchronously prefetches
 * adjacent comics (N±X) to improve navigation performance.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "comics.cache.caffeine.lookahead.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PredictiveCacheService {

    private final ManagementFacade comicManagementFacade;
    private final CaffeineCacheProperties cacheProperties;

    /**
     * Asynchronously prefetches adjacent comics for improved navigation performance.
     *
     * @param comicId the ID of the comic being viewed
     * @param currentDate the date of the comic currently being viewed
     * @param direction the direction of navigation (FORWARD or BACKWARD)
     */
    @Async
    public void prefetchAdjacentComics(int comicId, LocalDate currentDate, Direction direction) {
        if (!cacheProperties.getLookahead().isEnabled()) {
            return;
        }

        int lookaheadCount = cacheProperties.getLookahead().getCount();
        log.debug("Prefetching {} adjacent comics for comic {} from {} in direction {}",
            lookaheadCount, comicId, currentDate, direction);

        try {
            LocalDate searchDate = currentDate;
            int successCount = 0;

            // Prefetch the next N comics in the specified direction
            for (int i = 0; i < lookaheadCount; i++) {
                ComicNavigationResult result = comicManagementFacade.getComicStrip(comicId, direction, searchDate);

                if (result.isFound()) {
                    successCount++;
                    searchDate = result.getCurrentDate();
                    log.trace("Prefetched comic {} for date {} (step {}/{})",
                        comicId, searchDate, i + 1, lookaheadCount);
                } else {
                    // No more comics in this direction, stop prefetching
                    log.debug("Reached end of available comics at step {}/{}, prefetched {} comics",
                        i + 1, lookaheadCount, successCount);
                    break;
                }
            }

            log.debug("Completed prefetch for comic {}: successfully cached {} of {} requested comics",
                comicId, successCount, lookaheadCount);

        } catch (Exception e) {
            log.warn("Error during predictive cache prefetch for comic {}: {}",
                comicId, e.getMessage());
        }
    }

    /**
     * Prefetches comics bidirectionally from the current date.
     * Useful for warming cache when a comic is first accessed.
     *
     * @param comicId the ID of the comic
     * @param currentDate the date of the comic currently being viewed
     */
    @Async
    public void prefetchBidirectional(int comicId, LocalDate currentDate) {
        if (!cacheProperties.getLookahead().isEnabled()) {
            return;
        }

        log.debug("Prefetching comics bidirectionally for comic {} from {}", comicId, currentDate);

        // Prefetch forward
        prefetchAdjacentComics(comicId, currentDate, Direction.FORWARD);

        // Prefetch backward
        prefetchAdjacentComics(comicId, currentDate, Direction.BACKWARD);
    }
}
