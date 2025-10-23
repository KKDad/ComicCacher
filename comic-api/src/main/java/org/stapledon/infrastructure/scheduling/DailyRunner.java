package org.stapledon.infrastructure.scheduling;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.stapledon.common.config.properties.DailyRunnerProperties;
import org.stapledon.common.infrastructure.config.TaskExecutionTracker;
import org.stapledon.engine.management.ComicManagementFacade;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class ensures that all comics are fetched once a day.
 * Uses TaskExecutionTracker to ensure it only runs once per day even if the application is restarted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRunner implements CommandLineRunner {

    private final DailyRunnerProperties dailyRunnerProperties;
    private final ComicManagementFacade comicManagementFacade;
    private final TaskExecutionTracker taskExecutionTracker;
    
    private static final String TASK_NAME = "DailyComicCacher";

    /**
     * Schedule a task to download the comics once a day at 7:00am.
     * Will only execute if it hasn't already run today.
     */
    public void ensureDailyCaching() {
        log.info("Configuring daily update");
        var localNow = LocalDateTime.now();
        var currentZone = ZoneId.of("America/New_York");
        var zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZonedDateTime zonedNext5;
        zonedNext5 = zonedNow.withHour(7).withMinute(0).withSecond(0);
        if (zonedNow.compareTo(zonedNext5) > 0)
            zonedNext5 = zonedNext5.plusDays(1);

        var duration = Duration.between(zonedNow, zonedNext5);
        long initialDelay = duration.getSeconds();

        // If already run today, adjust the initial delay to tomorrow at 7 AM
        if (!taskExecutionTracker.canRunToday(TASK_NAME)) {
            LocalDate lastRun = taskExecutionTracker.getLastExecutionDate(TASK_NAME);
            log.info("Daily comic caching already ran today ({}), scheduling for tomorrow", lastRun);
            initialDelay = Duration.between(zonedNow, zonedNext5.plusDays(1)).getSeconds();
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                new RunComicCacher(comicManagementFacade, taskExecutionTracker),
                initialDelay,
                24 * 60 * 60L,
                TimeUnit.SECONDS);
        log.info("Daily update configured, Initial delay is {}", 
                (initialDelay > 60) ? String.format("%d minutes", initialDelay/60) : " < 1 minute");
    }

    @Override
    public void run(String... args) {
        if (dailyRunnerProperties.isEnabled()) {
            // Immediately run the caching if it hasn't run today
            if (taskExecutionTracker.canRunToday(TASK_NAME)) {
                log.info("Daily caching has not run today, executing immediately");
                comicManagementFacade.updateAllComics();
                taskExecutionTracker.markTaskExecuted(TASK_NAME);
            } else {
                log.info("Daily caching already ran today ({}), skipping immediate execution",
                         taskExecutionTracker.getLastExecutionDate(TASK_NAME));
            }
            
            // Configure the scheduler for future runs
            ensureDailyCaching();
        } else {
            log.warn("Daily Runner is disabled");
        }
    }

    @RequiredArgsConstructor
    private static class RunComicCacher implements Runnable {

        private final ComicManagementFacade comicManagementFacade;
        private final TaskExecutionTracker taskExecutionTracker;

        @Override
        public void run() {
            // Only run if it hasn't run today
            if (taskExecutionTracker.canRunToday(TASK_NAME)) {
                log.info("Running scheduled daily comic caching");
                comicManagementFacade.updateAllComics();
                taskExecutionTracker.markTaskExecuted(TASK_NAME);
            } else {
                log.info("Scheduled daily comic caching already ran today, skipping");
            }
        }
    }
}
