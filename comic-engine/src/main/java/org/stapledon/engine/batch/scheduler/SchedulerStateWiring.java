package org.stapledon.engine.batch.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wires the SchedulerStateService into all DailyJobScheduler beans after construction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerStateWiring {

    private final SchedulerStateService schedulerStateService;

    @Autowired(required = false)
    private List<DailyJobScheduler> schedulers = List.of();

    /**
     * Injects the state service into all daily job schedulers.
     */
    @PostConstruct
    public void wireStateService() {
        for (DailyJobScheduler scheduler : schedulers) {
            scheduler.setSchedulerStateService(schedulerStateService);
            log.debug("Wired SchedulerStateService into {}", scheduler.getJobName());
        }
    }
}
