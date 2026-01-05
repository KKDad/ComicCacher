package org.stapledon.engine.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.stapledon.engine.batch.BatchJobBaseConfig;
import org.stapledon.engine.batch.scheduler.AbstractJobScheduler;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Health check component to verify all scheduler beans are loaded correctly.
 * Implements Spring Actuator HealthIndicator for external monitoring.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Dynamically discovers all AbstractJobScheduler beans</li>
 * <li>Compares against BatchJobBaseConfig.KNOWN_JOBS</li>
 * <li>Reports missing or unexpected schedulers</li>
 * <li>Exposes health status via /actuator/health endpoint</li>
 * </ul>
 */
@Slf4j
@Getter
@Component
public class SchedulerHealthCheck implements HealthIndicator {

    private final Map<String, AbstractJobScheduler> schedulers;
    private Set<String> missingJobs;
    private Set<String> unexpectedJobs;

    /**
     * Spring automatically injects all beans of type AbstractJobScheduler.
     * The map keys are the bean names, values are the scheduler instances.
     * May be null or empty if no scheduler beans are present.
     */
    public SchedulerHealthCheck(Map<String, AbstractJobScheduler> schedulers) {
        this.schedulers = schedulers != null ? schedulers : Collections.emptyMap();
    }

    /**
     * Verify all scheduler beans exist at startup.
     * Logs warnings for missing or unexpected schedulers.
     */
    @PostConstruct
    public void verifySchedulers() {
        log.warn("======== VERIFYING SCHEDULER BEANS ========");

        Set<String> registeredJobNames = getRegisteredJobNames();
        Set<String> expectedJobNames = BatchJobBaseConfig.KNOWN_JOBS;

        this.missingJobs = getMissingJobs(expectedJobNames, registeredJobNames);
        this.unexpectedJobs = getUnexpectedJobs(expectedJobNames, registeredJobNames);

        // Log each registered scheduler
        if (schedulers.isEmpty()) {
            log.warn("No scheduler beans found - all batch jobs may be disabled");
        } else {
            schedulers.forEach((beanName, scheduler) -> log.info("✓ {} ({}) - bean: {}",
                    scheduler.getJobName(),
                    scheduler.getScheduleType(),
                    beanName));
        }

        // Log warnings for missing schedulers
        if (!missingJobs.isEmpty()) {
            log.error("✗ MISSING SCHEDULERS: {} - Jobs defined in KNOWN_JOBS but no scheduler found",
                    missingJobs);
        }

        // Log warnings for unexpected schedulers
        if (!unexpectedJobs.isEmpty()) {
            log.warn("⚠ UNEXPECTED SCHEDULERS: {} - Schedulers found but not in KNOWN_JOBS", unexpectedJobs);
        }

        log.warn("======== SCHEDULER VERIFICATION COMPLETE: {} registered, {} expected ========",
                registeredJobNames.size(), expectedJobNames.size());
    }

    /**
     * Spring Actuator health check.
     * Reports UP if all expected schedulers are present, DOWN otherwise.
     */
    @Override
    public Health health() {
        Health.Builder builder = isHealthy() ? Health.up() : Health.down();

        return builder
                .withDetail("registeredSchedulers", getRegisteredJobNames())
                .withDetail("expectedSchedulers", BatchJobBaseConfig.KNOWN_JOBS)
                .withDetail("missingSchedulers", missingJobs != null ? missingJobs : Collections.emptySet())
                .withDetail("unexpectedSchedulers", unexpectedJobs != null ? unexpectedJobs : Collections.emptySet())
                .build();
    }

    /**
     * Checks if all expected schedulers are present.
     *
     * @return true if all expected jobs have schedulers, false otherwise
     */
    public boolean isHealthy() {
        return missingJobs == null || missingJobs.isEmpty();
    }

    /**
     * Gets the job names of all registered schedulers.
     */
    public Set<String> getRegisteredJobNames() {
        if (schedulers == null || schedulers.isEmpty()) {
            return Collections.emptySet();
        }
        return schedulers.values().stream()
                .map(AbstractJobScheduler::getJobName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Finds jobs that are expected but not registered.
     */
    private Set<String> getMissingJobs(Set<String> expected, Set<String> registered) {
        if (expected == null) {
            return Collections.emptySet();
        }
        return expected.stream()
                .filter(job -> !registered.contains(job))
                .collect(Collectors.toSet());
    }

    /**
     * Finds jobs that are registered but not expected.
     */
    private Set<String> getUnexpectedJobs(Set<String> expected, Set<String> registered) {
        if (expected == null || registered == null) {
            return Collections.emptySet();
        }
        return registered.stream()
                .filter(job -> !expected.contains(job))
                .collect(Collectors.toSet());
    }
}
