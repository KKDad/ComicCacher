package org.stapledon.engine.batch;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.util.NfsFileOperations;
import org.stapledon.engine.batch.BackfillState.ComicState;
import org.stapledon.engine.batch.BackfillState.DateFailure;
import org.stapledon.engine.batch.BackfillState.SourceState;

/**
 * Remembers what the comic backfill learned between runs, so it stops wasting requests:
 * <ul>
 * <li><b>Given-up dates:</b> a date that comes back unavailable, or as a duplicate of another date's image, {@code give-up-after} times is skipped.</li>
 * <li><b>Comic horizon:</b> {@code horizon-consecutive-failures} different dates in a row coming back unavailable, all older than the recent window mean the source no longer
 * serves that comic's older strips (typically a subscription wall). Dates on or before the newest of those are skipped.</li>
 * <li><b>Source horizon:</b> when {@code horizon-min-comics} comics on one source reach a horizon at about the same age, that age becomes the source's horizon, so
 * every comic on the source stops there.</li>
 * </ul>
 * Given-up dates and horizons expire after {@code retry-given-up-after-days}, so a site that relaxes its paywall is noticed. A successful download older than a
 * horizon clears it.
 * <p>
 * State is kept in memory and written to {@code backfill-state.json} in the cache root by {@link #flush()} (the backfill job calls it after each chunk), atomically
 * and with expired entries dropped.
 */
@Slf4j
@Service
public class BackfillStateService {

    static final String STATE_FILENAME = "backfill-state.json";
    public static final String OUTCOME_UNAVAILABLE = "UNAVAILABLE";
    public static final String OUTCOME_DUPLICATE = "DUPLICATE";

    private final CacheProperties cacheProperties;
    private final Gson gson;
    private final BackfillConfigurationService config;
    private final Clock clock;

    private BackfillState state;
    private boolean dirty;

    @Autowired
    public BackfillStateService(CacheProperties cacheProperties, @Qualifier("gsonWithLocalDate") Gson gson, BackfillConfigurationService config) {
        this(cacheProperties, gson, config, Clock.systemDefaultZone());
    }

    BackfillStateService(CacheProperties cacheProperties, Gson gson, BackfillConfigurationService config, Clock clock) {
        this.cacheProperties = cacheProperties;
        this.gson = gson;
        this.config = config;
        this.clock = clock;
    }

    /**
     * True when this comic's date has failed often enough recently that backfill should leave it alone.
     */
    public synchronized boolean isGivenUp(ComicItem comic, LocalDate date) {
        return Optional.ofNullable(load().getComics().get(key(comic)))
                .map(c -> c.getFailures().get(date.toString()))
                .filter(f -> f.getCount() >= config.getGiveUpAfter())
                .filter(f -> !isExpired(f.getLastAttempt()))
                .isPresent();
    }

    /**
     * The newest date backfill must not scan at or before for this comic, from the comic's or the source's learned horizon, whichever is newer.
     */
    public synchronized Optional<LocalDate> horizonFloor(ComicItem comic) {
        BackfillState s = load();
        Optional<LocalDate> comicFloor = Optional.ofNullable(s.getComics().get(key(comic)))
                .filter(c -> c.getHorizonDate() != null && !isExpired(c.getHorizonDetectedAt()))
                .map(ComicState::getHorizonDate);
        Optional<LocalDate> sourceFloor = Optional.ofNullable(comic.getSource())
                .map(src -> s.getSources().get(src))
                .filter(src -> src.getHorizonDays() != null && !isExpired(src.getHorizonDetectedAt()))
                .map(src -> today().minusDays(src.getHorizonDays()));
        if (comicFloor.isPresent() && sourceFloor.isPresent()) {
            return Optional.of(comicFloor.get().isAfter(sourceFloor.get()) ? comicFloor.get() : sourceFloor.get());
        }
        return comicFloor.or(() -> sourceFloor);
    }

    /**
     * Backfill downloads already attempted today against {@code source}.
     */
    public synchronized int attemptsToday(String source) {
        return Optional.ofNullable(load().getSources().get(source))
                .filter(src -> today().equals(src.getAttemptDate()))
                .map(SourceState::getAttemptsOnDate)
                .orElse(0);
    }

    /**
     * Counts one backfill download attempt against {@code source}.
     */
    public synchronized void recordAttempt(String source) {
        SourceState src = load().getSources().computeIfAbsent(source, k -> new SourceState());
        if (!today().equals(src.getAttemptDate())) {
            src.setAttemptDate(today());
            src.setAttemptsOnDate(0);
        }
        src.setAttemptsOnDate(src.getAttemptsOnDate() + 1);
        dirty = true;
    }

    /**
     * Records a strip the source didn't have ({@link #OUTCOME_UNAVAILABLE}) or served as another date's image ({@link #OUTCOME_DUPLICATE}), and learns a
     * horizon when enough of them line up.
     */
    public synchronized void recordUnavailable(ComicItem comic, LocalDate date, String outcome) {
        ComicState c = comicState(comic);
        DateFailure failure = c.getFailures().computeIfAbsent(date.toString(), k -> new DateFailure());
        if (isExpired(failure.getLastAttempt())) {
            failure.setCount(0);
        }
        failure.setCount(failure.getCount() + 1);
        failure.setLastOutcome(outcome);
        failure.setLastAttempt(now());
        if (failure.getCount() == config.getGiveUpAfter()) {
            log.info("Backfill giving up on {} for {} after {} {} results", comic.getName(), date, failure.getCount(), outcome);
        }

        // Count each date once towards a horizon, so one date failing on several runs isn't mistaken for a wall
        if (failure.getCount() == 1 && isOlderThanRecentWindow(comic, date)) {
            if (c.getConsecutiveOldFailures() == 0 || c.getConsecutiveOldFailuresNewest() == null || date.isAfter(c.getConsecutiveOldFailuresNewest())) {
                c.setConsecutiveOldFailuresNewest(date);
            }
            c.setConsecutiveOldFailures(c.getConsecutiveOldFailures() + 1);
            if (c.getConsecutiveOldFailures() >= config.getHorizonConsecutiveFailures()) {
                setComicHorizon(comic, c, c.getConsecutiveOldFailuresNewest());
            }
        }
        dirty = true;
    }

    /**
     * Records a strip that was downloaded and saved: clears that date's failures, breaks any run of old failures, and drops a horizon the download proves wrong.
     */
    public synchronized void recordSuccess(ComicItem comic, LocalDate date) {
        BackfillState s = load();
        ComicState c = s.getComics().get(key(comic));
        boolean changed = false;
        if (c != null) {
            changed = c.getFailures().remove(date.toString()) != null;
            if (isOlderThanRecentWindow(comic, date) && c.getConsecutiveOldFailures() > 0) {
                c.setConsecutiveOldFailures(0);
                c.setConsecutiveOldFailuresNewest(null);
                changed = true;
            }
            if (c.getHorizonDate() != null && !date.isAfter(c.getHorizonDate())) {
                log.info("Backfill clearing horizon {} for {}: {} downloaded", c.getHorizonDate(), comic.getName(), date);
                c.setHorizonDate(null);
                c.setHorizonDetectedAt(null);
                changed = true;
            }
        }
        SourceState src = comic.getSource() == null ? null : s.getSources().get(comic.getSource());
        if (src != null && src.getHorizonDays() != null && ChronoUnit.DAYS.between(date, today()) >= src.getHorizonDays()) {
            log.info("Backfill clearing {}-day horizon for source {}: {} {} downloaded", src.getHorizonDays(), comic.getSource(), comic.getName(), date);
            src.setHorizonDays(null);
            src.setHorizonDetectedAt(null);
            changed = true;
        }
        if (changed) {
            dirty = true;
        }
    }

    /**
     * Forgets everything learned (given-up dates, horizons and attempt counts).
     */
    public synchronized void reset() {
        state = new BackfillState();
        save();
        log.info("Backfill state reset");
    }

    /**
     * Writes the in-memory state to {@code backfill-state.json} if it changed since the last write, first dropping entries that have expired.
     */
    public synchronized void flush() {
        if (!dirty) {
            return;
        }
        pruneExpired();
        save();
    }

    private void pruneExpired() {
        BackfillState s = load();
        s.getComics().values().forEach(c -> {
            c.getFailures().values().removeIf(f -> isExpired(f.getLastAttempt()));
            if (c.getFailures().isEmpty()) {
                // The dates behind any run of old failures have expired too
                c.setConsecutiveOldFailures(0);
                c.setConsecutiveOldFailuresNewest(null);
            }
            if (c.getHorizonDate() != null && isExpired(c.getHorizonDetectedAt())) {
                c.setHorizonDate(null);
                c.setHorizonDetectedAt(null);
            }
        });
        s.getComics().values().removeIf(c -> c.getFailures().isEmpty() && c.getHorizonDate() == null && c.getConsecutiveOldFailures() == 0);
        s.getSources().values().forEach(src -> {
            if (src.getHorizonDays() != null && isExpired(src.getHorizonDetectedAt())) {
                src.setHorizonDays(null);
                src.setHorizonDetectedAt(null);
            }
        });
    }

    private void setComicHorizon(ComicItem comic, ComicState c, LocalDate horizon) {
        if (horizon.equals(c.getHorizonDate()) && !isExpired(c.getHorizonDetectedAt())) {
            return;
        }
        c.setHorizonDate(horizon);
        c.setHorizonDetectedAt(now());
        log.info("Backfill learned horizon for {}: nothing on or before {} ({} days back)", comic.getName(), horizon, ChronoUnit.DAYS.between(horizon, today()));
        learnSourceHorizon(comic.getSource(), horizon);
    }

    /**
     * Sets the source's horizon when enough of its comics have horizons of about the same age as {@code horizon}.
     */
    private void learnSourceHorizon(String source, LocalDate horizon) {
        if (source == null) {
            return;
        }
        long age = ChronoUnit.DAYS.between(horizon, today());
        List<Long> matchingAges = new ArrayList<>();
        for (ComicState other : load().getComics().values()) {
            if (source.equals(other.getSource()) && other.getHorizonDate() != null && !isExpired(other.getHorizonDetectedAt())) {
                long otherAge = ChronoUnit.DAYS.between(other.getHorizonDate(), today());
                if (Math.abs(otherAge - age) <= config.getHorizonToleranceDays()) {
                    matchingAges.add(otherAge);
                }
            }
        }
        if (matchingAges.size() < config.getHorizonMinComics()) {
            return;
        }
        int horizonDays = (int) matchingAges.stream().mapToLong(Long::longValue).max().orElse(age);
        SourceState src = load().getSources().computeIfAbsent(source, k -> new SourceState());
        if (src.getHorizonDays() == null || src.getHorizonDays() != horizonDays) {
            src.setHorizonDays(horizonDays);
            src.setHorizonDetectedAt(now());
            log.warn("Backfill learned horizon for source {}: {} comics stop at about {} days back; scanning no further than that",
                    source, matchingAges.size(), horizonDays);
        }
    }

    private boolean isOlderThanRecentWindow(ComicItem comic, LocalDate date) {
        return date.isBefore(today().minusDays(config.getRecentDaysForSource(comic.getSource()) - 1L));
    }

    private boolean isExpired(OffsetDateTime at) {
        return at != null && at.isBefore(now().minusDays(config.getRetryGivenUpAfterDays()));
    }

    private ComicState comicState(ComicItem comic) {
        ComicState c = load().getComics().computeIfAbsent(key(comic), k -> new ComicState());
        c.setName(comic.getName());
        c.setSource(comic.getSource());
        return c;
    }

    private static String key(ComicItem comic) {
        return String.valueOf(comic.getId());
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private BackfillState load() {
        if (state != null) {
            return state;
        }
        Path file = stateFile();
        state = new BackfillState();
        if (!NfsFileOperations.exists(file)) {
            return state;
        }
        try {
            BackfillState loaded = gson.fromJson(NfsFileOperations.readAsString(file), BackfillState.class);
            if (loaded != null) {
                if (loaded.getSources() != null) {
                    state.getSources().putAll(loaded.getSources());
                }
                if (loaded.getComics() != null) {
                    loaded.getComics().values().stream()
                            .filter(c -> c.getFailures() == null)
                            .forEach(c -> c.setFailures(new HashMap<>()));
                    state.getComics().putAll(loaded.getComics());
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            log.error("Failed to read {}, starting with empty backfill state: {}", file, e.getMessage(), e);
        }
        return state;
    }

    private void save() {
        Path file = stateFile();
        try {
            NfsFileOperations.atomicWrite(file, gson.toJson(load()));
            dirty = false;
        } catch (IOException e) {
            log.error("Failed to write {}: {}", file, e.getMessage(), e);
        }
    }

    private Path stateFile() {
        return NfsFileOperations.resolvePath(cacheProperties.getLocation(), STATE_FILENAME);
    }
}
