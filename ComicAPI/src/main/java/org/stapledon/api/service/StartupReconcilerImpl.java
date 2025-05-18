package org.stapledon.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.stapledon.config.IComicsBootstrap;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.config.TaskExecutionTracker;
import org.stapledon.config.properties.StartupReconcilerProperties;
import org.stapledon.downloader.ComicCacher;
import org.stapledon.dto.Bootstrap;
import org.stapledon.dto.ComicConfig;
import org.stapledon.dto.ComicItem;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for reconciling comic configurations on a daily schedule.
 * Uses TaskExecutionTracker to ensure it only runs once per day even if the application is restarted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StartupReconcilerImpl implements StartupReconciler, CommandLineRunner {

    private final StartupReconcilerProperties startupReconcilerProperties;
    private final JsonConfigWriter jsonConfigWriter;
    private final ComicCacher comicCacher;
    private final TaskExecutionTracker taskExecutionTracker;
    
    private static final String TASK_NAME = "StartupReconciler";

    @Override
    public boolean reconcile() {
        log.info("Running comic configuration reconciliation...");
        try {
            // Always load the comics to ensure the application has data to work with
            var comicConfig = jsonConfigWriter.loadComics();
            
            // Only perform the reconciliation if it hasn't run today
            if (taskExecutionTracker.canRunToday(TASK_NAME)) {
                log.info("Performing reconciliation for today");
                reconcileBootstrapConfig(comicConfig);
                comicConfig = jsonConfigWriter.loadComics(); // Reload after reconciliation
                taskExecutionTracker.markTaskExecuted(TASK_NAME);
            } else {
                log.info("Reconciliation already ran today ({}), skipping", 
                         taskExecutionTracker.getLastExecutionDate(TASK_NAME));
            }

            ComicsServiceImpl.getComics().addAll(comicConfig.getItems().values());
            log.info("Loaded: {} comics.", ComicsServiceImpl.getComics().size());
            return true;

        } catch (IOException fne) {
            log.error("Cannot load ComicList", fne);
        }
        return false;
    }

    /**
     * Schedule the reconciliation task to run at the configured time.
     */
    public void scheduleReconciliation() {
        log.info("Configuring scheduled reconciliation");
        
        // Parse the schedule time
        String scheduleTimeStr = startupReconcilerProperties.getScheduleTime();
        LocalTime scheduleTime = LocalTime.parse(scheduleTimeStr);
        
        var localNow = LocalDateTime.now();
        var currentZone = ZoneId.of("America/New_York");
        var zonedNow = ZonedDateTime.of(localNow, currentZone);
        
        // Set up next run time
        ZonedDateTime nextRun = zonedNow.withHour(scheduleTime.getHour())
                                       .withMinute(scheduleTime.getMinute())
                                       .withSecond(scheduleTime.getSecond());
        
        // If the scheduled time for today has already passed, schedule for tomorrow
        if (zonedNow.compareTo(nextRun) > 0) {
            nextRun = nextRun.plusDays(1);
        }

        // Calculate initial delay
        var duration = Duration.between(zonedNow, nextRun);
        long initialDelay = duration.getSeconds();
        
        // If already run today, adjust the initial delay to tomorrow
        if (!taskExecutionTracker.canRunToday(TASK_NAME)) {
            log.info("Reconciliation already ran today ({}), scheduling for tomorrow", 
                     taskExecutionTracker.getLastExecutionDate(TASK_NAME));
            nextRun = nextRun.plusDays(1);
            initialDelay = Duration.between(zonedNow, nextRun).getSeconds();
        }

        // Create and configure the scheduler
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                new RunReconciliation(this, taskExecutionTracker), 
                initialDelay,
                24 * 60 * 60L, 
                TimeUnit.SECONDS);
        
        log.info("Scheduled reconciliation configured for {} (in {} minutes)", 
                nextRun.toLocalTime(),
                initialDelay / 60);
    }

    /**
     * Reconcile all the entries in the BootstrapConfig with the ComicList.
     * - New entries found in the BootstrapConfig will be added and immediately cached
     * - Entries found in the ComicList, but not in the BootstrapConfig will be removed
     */
    public void reconcileBootstrapConfig(ComicConfig comicConfig) {
        log.info("Begin Reconciliation of CacherBootstrapConfig and ComicConfig");
        Bootstrap config = comicCacher.bootstrapConfig();

        // Check for New GoComics
        for (IComicsBootstrap daily : config.getDailyComics()) {
            var comic = findComicItem(comicConfig, daily);
            if (comic == null) {
                if (log.isInfoEnabled())
                    log.info("Bootstrapping new DailyComic: {}", daily.stripName());
                comicCacher.cacheSingle(true, daily);
            }
        }

        // Check for New KingFeatures
        for (IComicsBootstrap king : config.getKingComics()) {
            var comic = findComicItem(comicConfig, king);
            if (comic == null) {
                if (log.isInfoEnabled())
                    log.info("Bootstrapping new KingFeatures: {}", king.stripName());
                comicCacher.cacheSingle(true, king);
            }
        }

        // Removed entries found in ComicConfig, but not in the CacherBootstrapConfig
        comicConfig.getItems().entrySet().removeIf(integerComicItemEntry -> findBootstrapComic(config, integerComicItemEntry.getValue()) == null);

        log.info("Reconciliation complete");
    }

    /**
     * Giving a ComicItem, Locate the corresponding IComicsBootstrap from the BootStrap configuration
     *
     * @param comic ComicItem to lookup
     * @return IComicsBootstrap or null if none could be located
     */
    IComicsBootstrap findBootstrapComic(Bootstrap config, ComicItem comic) {
        if (!config.getDailyComics().isEmpty()) {
            IComicsBootstrap dailyComics = config.getDailyComics().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(comic.getName()))
                    .findFirst()
                    .orElse(null);
            if (dailyComics != null)
                return dailyComics;
        }
        if (!config.getKingComics().isEmpty()) {
            IComicsBootstrap kingComics = config.getKingComics().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(comic.getName()))
                    .findFirst()
                    .orElse(null);
            if (kingComics != null)
                return kingComics;
        }
        if (log.isWarnEnabled())
            log.warn("{} was not found. Disabling", comic.getName());
        return null;
    }

    /**
     * Giving a IComicsBootstrap, Locate the corresponding ComicItem from the BootStrap configuration
     *
     * @param config List<ComicConfig> to search in
     * @param comic  IComicsBootstrap to lookup
     * @return ComicItem or null if none could be located
     */
    ComicItem findComicItem(ComicConfig config, IComicsBootstrap comic) {
        if (!config.getItems().isEmpty()) {
            Map.Entry<Integer, ComicItem> result = config.getItems().entrySet()
                    .stream()
                    .filter(p -> p.getValue().getName().equalsIgnoreCase(comic.stripName()))
                    .findFirst()
                    .orElse(null);
            if (result != null)
                return result.getValue();
        }
        return null;
    }

    @Override
    public void run(String... args) throws Exception {
        if (startupReconcilerProperties.isEnabled()) {
            // Load comics on startup to ensure the application has data to work with
            try {
                var comicConfig = jsonConfigWriter.loadComics();
                ComicsServiceImpl.getComics().addAll(comicConfig.getItems().values());
                log.info("Loaded: {} comics on startup.", ComicsServiceImpl.getComics().size());
            } catch (IOException fne) {
                log.error("Cannot load ComicList on startup", fne);
            }
            
            // Schedule the reconciliation task
            scheduleReconciliation();
        } else {
            log.warn("Scheduled Reconciler is disabled");
        }
    }
    
    /**
     * Runnable class for scheduled reconciliation executions
     */
    @RequiredArgsConstructor
    private static class RunReconciliation implements Runnable {

        private final StartupReconcilerImpl reconciler;
        private final TaskExecutionTracker taskExecutionTracker;

        @Override
        public void run() {
            // Only run if it hasn't run today
            if (taskExecutionTracker.canRunToday(TASK_NAME)) {
                log.info("Running scheduled reconciliation");
                reconciler.reconcile();
            } else {
                log.info("Scheduled reconciliation already ran today, skipping");
            }
        }
    }
}
