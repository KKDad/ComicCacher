package org.stapledon.downloader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class ensures that all comics are fetched once a day
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyRunner {

    private final ComicCacher comicCacher;

    /**
     * Schedule a task to download the comics once a day at 7:00am
     */
    public void ensureDailyCaching() {
        var localNow = LocalDateTime.now();
        var currentZone = ZoneId.of("America/New_York");
        var zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZonedDateTime zonedNext5;
        zonedNext5 = zonedNow.withHour(7).withMinute(0).withSecond(0);
        if (zonedNow.compareTo(zonedNext5) > 0)
            zonedNext5 = zonedNext5.plusDays(1);

        var duration = Duration.between(zonedNow, zonedNext5);
        long initalDelay = duration.getSeconds();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new RunComicCacher(comicCacher), initalDelay,
                24 * 60 * 60L, TimeUnit.SECONDS);
    }

    @RequiredArgsConstructor
    private static class RunComicCacher implements Runnable {

        private final ComicCacher comicCacher;

        @Override
        public void run() {
            comicCacher.cacheAll();
        }
    }

}
