package org.stapledon.utils;

import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.dto.ComicItem;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CacheUtils
{
    private static final int WARNING_TIME_MS = 100;
    public static final String COMBINE_PATH = "%s/%s";
    private final String cacheHome;

    public CacheUtils(String cacheHome)
    {
        Objects.requireNonNull(cacheHome, "cacheHome must be specified");

        this.cacheHome = cacheHome;
    }

    private File getComicHome(ComicItem comic)
    {
        String comicNameParsed = comic.name.replace(" ", "");
        var path = String.format(COMBINE_PATH, this.cacheHome, comicNameParsed);
        return new File(path);
    }

    public File findOldest(ComicItem comic)
    {
        return this.findFirst(comic, Direction.FORWARD);
    }

    public File findNewest(ComicItem comic)
    {
        return this.findFirst(comic, Direction.BACKWARD);
    }


    public File findFirst(ComicItem comic, Direction which)
    {
        var timer = Stopwatch.createStarted();
        var root = getComicHome(comic);

        // Comics are stored by year, find the smallest year
        String[] yearFolders = root.list((dir, name) -> new File(dir, name).isDirectory());
        if (yearFolders == null || yearFolders.length == 0)
            return null;
        Arrays.sort(yearFolders, Comparator.comparing(Integer::valueOf));

        // Comics are stored with filename that is sortable.
        var folder = new File(String.format(COMBINE_PATH, root.getAbsolutePath(), which == Direction.FORWARD ? yearFolders[0] : yearFolders[yearFolders.length - 1]));
        String[] cachedStrips = folder.list();
        if (cachedStrips == null || cachedStrips.length == 0)
            return null;
        Arrays.sort(cachedStrips, String::compareTo);

        timer.stop();
        if (timer.elapsed(TimeUnit.MILLISECONDS) > WARNING_TIME_MS && log.isInfoEnabled())
                log.info(String.format("findFirst took: %s for %s, Direction=%s", timer.toString(), comic.name, which));

        return new File(String.format(COMBINE_PATH, folder.getAbsolutePath(), which == Direction.FORWARD ? cachedStrips[0] : cachedStrips[cachedStrips.length - 1]));
    }

    public File findNext(ComicItem comic, LocalDate from)
    {
        var timer = Stopwatch.createStarted();
        var root = getComicHome(comic);

        var findFirstResult = findNewest(comic);
        if (findFirstResult == null)
            return null;

        var limit = LocalDate.parse(Files.getNameWithoutExtension(findFirstResult.getName()), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate nextCandidate = from.plusDays(1);
        while (from.isBefore(limit)) {
            var folder = new File(String.format("%s/%s.png", root.getAbsolutePath(), nextCandidate.format(DateTimeFormatter.ofPattern("yyyy/yyyy-MM-dd"))));
            if (folder.exists()) {

                timer.stop();
                if (timer.elapsed(TimeUnit.MILLISECONDS) > WARNING_TIME_MS && log.isInfoEnabled())
                    log.info(String.format("findNext took: %s for %s", timer.toString(), comic.name));

                return folder;
            }
            nextCandidate = nextCandidate.plusDays(1);
        }
        return null;
    }

    public File findPrevious(ComicItem comic, LocalDate from)
    {
        var timer = Stopwatch.createStarted();
        var root = getComicHome(comic);

        var findFirstResult = findOldest(comic);
        if (findFirstResult == null)
            return null;

        var limit = LocalDate.parse(Files.getNameWithoutExtension(findFirstResult.getName()), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate nextCandidate = from.minusDays(1);
        while (from.isAfter(limit)) {
            var folder = new File(String.format("%s/%s.png", root.getAbsolutePath(), nextCandidate.format(DateTimeFormatter.ofPattern("yyyy/yyyy-MM-dd"))));
            if (folder.exists()) {

                timer.stop();
                if (timer.elapsed(TimeUnit.MILLISECONDS) > WARNING_TIME_MS && log.isInfoEnabled())
                    log.info(String.format("findPrevious took: %s for %s", timer.toString(), comic.name));

                return folder;
            }
            nextCandidate = nextCandidate.minusDays(1);
        }
        return null;
    }

}
