package org.stapledon.engine.batch.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles startup behavior for batch jobs.
 *
 * <p>
 * This component listens for {@link ApplicationReadyEvent} to trigger missed
 * execution
 * checks AFTER all beans are fully initialized. This ensures:
 * <ul>
 * <li>All downloader strategies are registered</li>
 * <li>All schedulers are initialized</li>
 * <li>All health checks are ready</li>
 * <li>Web server is accepting requests</li>
 * </ul>
 *
 * <p>
 * This replaces the previous approach of running makeup jobs in @PostConstruct,
 * which caused race conditions when job configs ran before strategy
 * registrations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupJobRunner {

    private final Map<String, DailyJobScheduler> dailySchedulers;

    /**
     * Handles application ready event by checking for missed job executions.
     * Runs after all beans are initialized and the application is ready to serve
     * requests.
     *
     * @param event the application ready event
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("======== CHECKING FOR MISSED JOB EXECUTIONS ========");

        if (dailySchedulers == null || dailySchedulers.isEmpty()) {
            log.info("No daily schedulers registered");
            return;
        }

        log.info("Found {} daily scheduler(s) to check", dailySchedulers.size());

        dailySchedulers.values().forEach(scheduler -> {
            try {
                log.debug("Checking missed execution for: {}", scheduler.getJobName());
                scheduler.runMissedExecutionIfNeeded();
            } catch (Exception e) {
                log.error("Failed to check missed execution for {}: {}",
                        scheduler.getJobName(), e.getMessage(), e);
            }
        });

        log.info("======== STARTUP JOB CHECK COMPLETE ========");
    }
}
