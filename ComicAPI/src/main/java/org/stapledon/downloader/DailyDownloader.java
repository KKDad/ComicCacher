package org.stapledon.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.api.ComicApiApplication;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DailyDownloader
{
    private static final Logger logger = LoggerFactory.getLogger(ComicApiApplication.class);

    private DailyDownloader()
    {
        // Sonar: Utility classes should not have public constructors
    }

    /**
     * Schedule a task to download the comics once a day at 7:00am
     */
    public static void ensureDailyCaching()
    {
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.of("America/New_York");
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZonedDateTime zonedNext5 ;
        zonedNext5 = zonedNow.withHour(7).withMinute(0).withSecond(0);
        if(zonedNow.compareTo(zonedNext5) > 0)
            zonedNext5 = zonedNext5.plusDays(1);

        Duration duration = Duration.between(zonedNow, zonedNext5);
        long initalDelay = duration.getSeconds();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new RunComicCacher(), initalDelay,
                24*60*60L, TimeUnit.SECONDS);
    }

    private static class RunComicCacher implements Runnable
    {
        @Override
        public void run() {
            try {
                ComicCacher comicCacher = new ComicCacher();
                comicCacher.cacheAll();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

}
