package org.stapledon.engine.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * What the comic backfill has learned between runs, persisted to {@code backfill-state.json} in the cache root.
 * <ul>
 * <li>Per source: the learned history horizon (how many days back the source still serves strips) and today's attempt count.</li>
 * <li>Per comic: a learned horizon date, and dates that keep coming back unavailable.</li>
 * </ul>
 * Map keys are strings (source name, comic id, ISO date) so the file stays readable and Gson needs no key adapters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackfillState {

    @Builder.Default
    private Map<String, SourceState> sources = new HashMap<>();

    @Builder.Default
    private Map<String, ComicState> comics = new HashMap<>();

    /**
     * Learned facts about one source.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceState {
        /** Days back the source still serves strips; null when not learned. */
        private Integer horizonDays;

        /** When the horizon was learned. */
        private OffsetDateTime horizonDetectedAt;

        /** The day {@link #attemptsOnDate} counts. */
        private LocalDate attemptDate;

        /** Backfill downloads attempted against this source on {@link #attemptDate}. */
        private int attemptsOnDate;
    }

    /**
     * Learned facts about one comic.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComicState {
        private String name;

        private String source;

        /** Dates on or before this are not scanned; null when not learned. */
        private LocalDate horizonDate;

        /** When the horizon was learned. */
        private OffsetDateTime horizonDetectedAt;

        /** Different dates in a row, older than the recent window, that came back unavailable. */
        private int consecutiveOldFailures;

        /** Newest date in the current run of {@link #consecutiveOldFailures}. */
        private LocalDate consecutiveOldFailuresNewest;

        /** Failures by ISO date. */
        @Builder.Default
        private Map<String, DateFailure> failures = new HashMap<>();
    }

    /**
     * Repeated failures for one comic on one date.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DateFailure {
        private int count;

        /** UNAVAILABLE or DUPLICATE. */
        private String lastOutcome;

        private OffsetDateTime lastAttempt;
    }
}
