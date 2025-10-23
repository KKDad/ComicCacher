package org.stapledon.infrastructure.scheduling;

/**
 * Service for reconciling the bootstrap configuration with available comics.
 * Runs on a daily schedule (by default at 6:00 AM) to ensure comics are synchronized.
 */
public interface StartupReconciler {

    /**
     * Perform the reconciliation between bootstrap config and comic list
     * @return true if reconciliation was successful, false otherwise
     */
    boolean reconcile();
    
    /**
     * Schedule the reconciliation to run on a daily basis
     */
    void scheduleReconciliation();
}
