package org.stapledon.infrastructure.caching;

import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.common.util.Direction;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class CacheUtils {
    private static final int WARNING_TIME_MS = 100;
    public static final String COMBINE_PATH = "%s/%s";
    private final String cacheHome;

    // Access tracking metrics
    private final ConcurrentHashMap<String, AtomicInteger> accessCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> lastAccessTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> totalAccessTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> cacheHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> cacheMisses = new ConcurrentHashMap<>();

    public CacheUtils(@Qualifier("cacheLocation") String cacheHome) {
        Objects.requireNonNull(cacheHome, "cacheHome must be specified");
        this.cacheHome = cacheHome;
    }

    private File getComicHome(ComicItem comic) {
        String comicNameParsed = comic.getName().replace(" ", "");
        var path = String.format(COMBINE_PATH, this.cacheHome, comicNameParsed);
        var file = new File(path);
        if (!file.exists())
            throw CacheException.directoryNotFound(comic.getName(), path);
        return file;
    }

    /**
     * Track access to a comic
     *
     * @param comicName Name of the comic being accessed
     * @param isHit Whether the access was a cache hit
     * @param accessTime Time taken for the access in milliseconds
     */
    private void trackAccess(String comicName, boolean isHit, long accessTime) {
        accessCounters.computeIfAbsent(comicName, k -> new AtomicInteger(0)).incrementAndGet();
        lastAccessTime.put(comicName, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Track hit/miss statistics
        if (isHit) {
            cacheHits.computeIfAbsent(comicName, k -> new AtomicInteger(0)).incrementAndGet();
        } else {
            cacheMisses.computeIfAbsent(comicName, k -> new AtomicInteger(0)).incrementAndGet();
        }

        // Track timing statistics
        totalAccessTime.compute(comicName, (k, v) -> (v == null) ? accessTime : v + accessTime);
    }

    /**
     * Get access count metrics for all comics
     *
     * @return Map of comic name to access count
     */
    public Map<String, Integer> getAccessCounts() {
        Map<String, Integer> result = new HashMap<>();
        accessCounters.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    /**
     * Get last access time for all comics
     *
     * @return Map of comic name to last access time
     */
    public Map<String, String> getLastAccessTimes() {
        return new HashMap<>(lastAccessTime);
    }

    /**
     * Get average access time for all comics
     *
     * @return Map of comic name to average access time in milliseconds
     */
    public Map<String, Double> getAverageAccessTimes() {
        Map<String, Double> result = new HashMap<>();
        accessCounters.forEach((comic, count) -> {
            Long total = totalAccessTime.getOrDefault(comic, 0L);
            result.put(comic, count.get() > 0 ? (double) total / count.get() : 0.0);
        });
        return result;
    }

    /**
     * Get hit ratio for all comics
     *
     * @return Map of comic name to hit ratio (0.0-1.0)
     */
    public Map<String, Double> getHitRatios() {
        Map<String, Double> result = new HashMap<>();
        accessCounters.forEach((comic, totalCount) -> {
            int hits = cacheHits.getOrDefault(comic, new AtomicInteger(0)).get();
            result.put(comic, totalCount.get() > 0 ? (double) hits / totalCount.get() : 0.0);
        });
        return result;
    }

    public File findOldest(ComicItem comic) {
        var timer = Stopwatch.createStarted();
        File result = this.findFirst(comic, Direction.FORWARD);
        timer.stop();
        trackAccess(comic.getName(), result != null, timer.elapsed(TimeUnit.MILLISECONDS));
        return result;
    }

    public File findNewest(ComicItem comic) {
        var timer = Stopwatch.createStarted();
        File result = this.findFirst(comic, Direction.BACKWARD);
        timer.stop();
        trackAccess(comic.getName(), result != null, timer.elapsed(TimeUnit.MILLISECONDS));
        return result;
    }

    public File findFirst(ComicItem comic, Direction which) {
        var timer = Stopwatch.createStarted();
        var root = getComicHome(comic);

        // Comics are stored by year, find the smallest year folder, excluding and directory called @eaDir
        String[] yearFolders = root.list((dir, name) -> new File(dir, name).isDirectory() && !name.equals("@eaDir"));
        if (yearFolders == null || yearFolders.length == 0)
            return null;
        Arrays.sort(yearFolders, Comparator.comparing(Integer::valueOf));

        // Comics are stored with filename that is sortable.
        var folder = new File(String.format(COMBINE_PATH, root.getAbsolutePath(), which == Direction.FORWARD ? yearFolders[0] : yearFolders[yearFolders.length - 1]));
        String[] cachedStrips = folder.list((dir, name) -> new File(dir, name).isFile() && !name.equals("@eaDir"));
        if (cachedStrips == null || cachedStrips.length == 0)
            return null;
        Arrays.sort(cachedStrips, String::compareTo);

        timer.stop();
        if (timer.elapsed(TimeUnit.MILLISECONDS) > WARNING_TIME_MS && log.isInfoEnabled())
            log.info(String.format("findFirst took: %s for %s, Direction=%s", timer.toString(), comic.getName(), which));

        return new File(String.format(COMBINE_PATH, folder.getAbsolutePath(), which == Direction.FORWARD ? cachedStrips[0] : cachedStrips[cachedStrips.length - 1]));
    }

    public File findNext(ComicItem comic, LocalDate from) {
        var timer = Stopwatch.createStarted();
        var root = getComicHome(comic);

        var findFirstResult = findNewest(comic);
        if (findFirstResult == null)
            return null;

        var limit = LocalDate.parse(Files.getNameWithoutExtension(findFirstResult.getName()), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate nextCandidate = from.plusDays(1);
        boolean found = false;
        File resultFile = null;

        while (from.isBefore(limit)) {
            var folder = new File(String.format("%s/%s.png", root.getAbsolutePath(), nextCandidate.format(DateTimeFormatter.ofPattern("yyyy/yyyy-MM-dd"))));
            if (folder.exists()) {
                found = true;
                resultFile = folder;
                break;
            } else {
                log.info("folder={} does not exist", folder);
            }
            nextCandidate = nextCandidate.plusDays(1);
        }

        timer.stop();
        if (timer.elapsed(TimeUnit.MILLISECONDS) > WARNING_TIME_MS && log.isInfoEnabled())
            log.info(String.format("findNext took: %s for %s", timer.toString(), comic.getName()));

        trackAccess(comic.getName(), found, timer.elapsed(TimeUnit.MILLISECONDS));
        return resultFile;
    }

    public File findPrevious(ComicItem comic, LocalDate from) {
        var timer = Stopwatch.createStarted();
        var root = getComicHome(comic);

        var findFirstResult = findOldest(comic);
        if (findFirstResult == null)
            return null;

        var limit = LocalDate.parse(Files.getNameWithoutExtension(findFirstResult.getName()), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate nextCandidate = from.minusDays(1);
        boolean found = false;
        File resultFile = null;

        while (from.isAfter(limit)) {
            var folder = new File(String.format("%s/%s.png", root.getAbsolutePath(), nextCandidate.format(DateTimeFormatter.ofPattern("yyyy/yyyy-MM-dd"))));
            if (folder.exists()) {
                found = true;
                resultFile = folder;
                break;
            } else {
                log.info("folder={} does not exist", folder);
            }
            nextCandidate = nextCandidate.minusDays(1);
        }

        timer.stop();
        if (timer.elapsed(TimeUnit.MILLISECONDS) > WARNING_TIME_MS && log.isInfoEnabled())
            log.info(String.format("findPrevious took: %s for %s", timer.toString(), comic.getName()));

        trackAccess(comic.getName(), found, timer.elapsed(TimeUnit.MILLISECONDS));
        return resultFile;
    }
}
