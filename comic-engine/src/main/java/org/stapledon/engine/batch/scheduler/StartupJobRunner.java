package org.stapledon.engine.batch.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executor;

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
 *
 * <p>
 * The makeup runs happen one after another on a background thread. Run inline,
 * they held up the listener, and with it the readiness state, so
 * {@code /actuator/health} reported 503 until every missed job had finished.
 *
 * <p>
 * Set {@code batch.startup-catch-up.enabled=false} to skip the makeup runs (the
 * integration tests do, so a background run can't race the jobs they start).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "batch.startup-catch-up.enabled", havingValue = "true", matchIfMissing = true)
public class StartupJobRunner {

    private final Map<String, DailyJobScheduler> dailySchedulers;
    private final Executor executor;

    @Autowired
    public StartupJobRunner(Map<String, DailyJobScheduler> dailySchedulers) {
        this(dailySchedulers, task -> Thread.ofVirtual().name("startup-catch-up").start(task));
    }

    StartupJobRunner(Map<String, DailyJobScheduler> dailySchedulers, Executor executor) {
        this.dailySchedulers = dailySchedulers;
        this.executor = executor;
    }

    /**
     * Handles application ready event by starting the missed-execution checks in the background, and returns straight away.
     * Runs after all beans are initialized and the application is ready to serve
     * requests.
     *
     * @param event the application ready event
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (dailySchedulers == null || dailySchedulers.isEmpty()) {
            log.info("No daily schedulers registered");
            return;
        }

        executor.execute(this::runMissedExecutions);
    }

    private void runMissedExecutions() {
        log.info("======== CHECKING FOR MISSED JOB EXECUTIONS ========");
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
