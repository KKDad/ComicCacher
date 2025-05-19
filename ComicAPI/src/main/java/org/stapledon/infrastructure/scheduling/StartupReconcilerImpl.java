package org.stapledon.infrastructure.scheduling;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.stapledon.core.comic.management.ComicManagementFacade;
import org.stapledon.infrastructure.config.properties.StartupReconcilerProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of StartupReconciler that delegates to ComicManagementFacade.
 * Responsible for reconciling comic configurations on a daily schedule.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StartupReconcilerImpl implements StartupReconciler, CommandLineRunner {

    private final StartupReconcilerProperties startupReconcilerProperties;
    private final ComicManagementFacade comicManagementFacade;
    
    @Override
    public boolean reconcile() {
        log.info("Delegating reconciliation to ComicManagementFacade");
        return comicManagementFacade.reconcileWithBootstrap();
    }

    /**
     * Schedule the reconciliation task to run at the configured time.
     */
    public void scheduleReconciliation() {
        log.info("Delegating reconciliation scheduling to ComicManagementFacade");
        comicManagementFacade.scheduleReconciliation(startupReconcilerProperties.getScheduleTime());
    }

    @Override
    public void run(String... args) {
        if (startupReconcilerProperties.isEnabled()) {
            // Delegate loading comics to the facade
            comicManagementFacade.refreshComicList();
            
            // Schedule the reconciliation task
            scheduleReconciliation();
        } else {
            log.warn("Scheduled Reconciler is disabled");
        }
    }
}