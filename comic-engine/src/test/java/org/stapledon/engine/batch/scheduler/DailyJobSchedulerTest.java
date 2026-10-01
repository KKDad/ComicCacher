package org.stapledon.engine.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.stapledon.engine.batch.JsonBatchExecutionTracker;
import org.stapledon.engine.batch.ManualJobLauncher;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyJobScheduler")
class DailyJobSchedulerTest {

    // Every minute, so a startup check is always past today's scheduled time
    private static final String CRON = "0 * * * * ?";

    @Mock
    private Job job;

    @Mock
    private JobOperator jobOperator;

    @Mock
    private JsonBatchExecutionTracker tracker;

    @Mock
    private JobExecution execution;

    @Mock
    private SchedulerStateService schedulerStateService;

    private DailyJobScheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(job.getName()).thenReturn("TestJob");
        lenient().when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(execution);
        scheduler = new DailyJobScheduler(job, CRON, "America/Toronto", jobOperator, tracker);
    }

    @Test
    @DisplayName("runs once a day by default")
    void skipsSecondRunInADayByDefault() throws Exception {
        when(tracker.hasJobRunToday("TestJob")).thenReturn(true);

        scheduler.executeScheduled();

        verify(jobOperator, never()).start(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("runs again the same day when multiple runs are allowed")
    void allowsSecondRunWhenMultipleRunsPerDay() throws Exception {
        scheduler.setMultipleRunsPerDay(true);

        scheduler.executeScheduled();

        verify(jobOperator).start(eq(job), any(JobParameters.class));
    }

    @Test
    @DisplayName("does not launch when the precondition is false")
    void skipsRunWhenPreconditionFalse() throws Exception {
        scheduler.setMultipleRunsPerDay(true);
        scheduler.setPrecondition(() -> false, "nothing to do");

        scheduler.executeScheduled();

        verify(jobOperator, never()).start(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("runs when the precondition itself fails")
    void runsWhenPreconditionThrows() throws Exception {
        scheduler.setMultipleRunsPerDay(true);
        scheduler.setPrecondition(() -> {
            throw new IllegalStateException("boom");
        }, "nothing to do");

        scheduler.executeScheduled();

        verify(jobOperator).start(eq(job), any(JobParameters.class));
    }

    @Test
    @DisplayName("manual triggers ignore the precondition")
    void manualTriggerIgnoresPrecondition() throws Exception {
        scheduler.setPrecondition(() -> false, "nothing to do");

        scheduler.triggerManually();

        verify(jobOperator).start(eq(job), any(JobParameters.class));
    }

    @Test
    @DisplayName("does not start a second run while one is in progress")
    void skipsLaunchWhileRunning() throws Exception {
        scheduler.setMultipleRunsPerDay(true);
        AtomicReference<Long> overlapping = new AtomicReference<>(-1L);
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenAnswer(inv -> {
            // A manual trigger arriving while the scheduled run is still going
            overlapping.set(scheduler.triggerManually());
            return execution;
        });

        scheduler.executeScheduled();

        assertThat(overlapping.get()).isNull();
        verify(jobOperator, times(1)).start(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("can run again once the previous run has finished, even if it failed")
    void runsAgainAfterPreviousRunEnds() throws Exception {
        when(jobOperator.start(eq(job), any(JobParameters.class)))
                .thenThrow(new IllegalStateException("boom"))
                .thenReturn(execution);

        assertThat(scheduler.triggerManually()).isNull();
        scheduler.triggerManually();

        verify(jobOperator, times(2)).start(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("makes up a missed run at startup by default")
    void runsMissedExecutionByDefault() throws Exception {
        when(tracker.hasJobRunToday("TestJob")).thenReturn(false);

        scheduler.runMissedExecutionIfNeeded();

        verify(jobOperator).start(eq(job), any(JobParameters.class));
    }

    @Test
    @DisplayName("no startup makeup run when multiple runs are allowed")
    void noMakeupRunWhenMultipleRunsPerDay() throws Exception {
        scheduler.setMultipleRunsPerDay(true);

        scheduler.runMissedExecutionIfNeeded();

        verify(jobOperator, never()).start(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("no startup makeup run when the job is paused")
    void noMakeupRunWhenPaused() throws Exception {
        when(schedulerStateService.isPaused("TestJob")).thenReturn(true);
        scheduler.setSchedulerStateService(schedulerStateService);

        scheduler.runMissedExecutionIfNeeded();

        verify(jobOperator, never()).start(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("makes up a missed run at startup when the job is not paused")
    void runsMissedExecutionWhenNotPaused() throws Exception {
        when(schedulerStateService.isPaused("TestJob")).thenReturn(false);
        when(tracker.hasJobRunToday("TestJob")).thenReturn(false);
        scheduler.setSchedulerStateService(schedulerStateService);

        scheduler.runMissedExecutionIfNeeded();

        verify(jobOperator).start(eq(job), any(JobParameters.class));
    }

    @Test
    @DisplayName("runs a manual trigger in the background and holds the lock until the run ends")
    void runsManualTriggersInTheBackground() throws Exception {
        ManualJobLauncher launcher = mock(ManualJobLauncher.class);
        scheduler.setManualJobLauncher(launcher);
        AtomicReference<Runnable> onEnd = new AtomicReference<>();
        when(execution.getId()).thenReturn(466L);
        when(launcher.start(eq(job), any(JobParameters.class), any(Runnable.class))).thenAnswer(inv -> {
            onEnd.set(inv.getArgument(2));
            return execution;
        });

        assertThat(scheduler.triggerManually(Map.of("source", "ALL"))).isEqualTo(466L);
        verify(jobOperator, never()).start(any(Job.class), any(JobParameters.class));

        // Still running: neither another manual run nor a scheduled one starts
        assertThat(scheduler.triggerManually()).isNull();
        scheduler.setMultipleRunsPerDay(true);
        scheduler.executeScheduled();
        verify(jobOperator, never()).start(any(Job.class), any(JobParameters.class));

        onEnd.get().run();
        assertThat(scheduler.triggerManually()).isEqualTo(466L);
        verify(launcher, times(2)).start(eq(job), any(JobParameters.class), any(Runnable.class));
    }

    @Test
    @DisplayName("keeps scheduled runs on the scheduler thread")
    void runsScheduledTriggersSynchronously() throws Exception {
        ManualJobLauncher launcher = mock(ManualJobLauncher.class);
        scheduler.setManualJobLauncher(launcher);
        scheduler.setMultipleRunsPerDay(true);

        scheduler.executeScheduled();

        verify(jobOperator).start(eq(job), any(JobParameters.class));
        verify(launcher, never()).start(any(Job.class), any(JobParameters.class), any(Runnable.class));
    }

    @Test
    @DisplayName("releases the lock when a background launch fails")
    void releasesTheLockWhenABackgroundLaunchFails() throws Exception {
        ManualJobLauncher launcher = mock(ManualJobLauncher.class);
        scheduler.setManualJobLauncher(launcher);
        when(launcher.start(eq(job), any(JobParameters.class), any(Runnable.class)))
                .thenThrow(new IllegalStateException("boom"))
                .thenReturn(execution);

        assertThat(scheduler.triggerManually()).isNull();
        scheduler.triggerManually();

        verify(launcher, times(2)).start(eq(job), any(JobParameters.class), any(Runnable.class));
    }
}
