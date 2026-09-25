package org.stapledon.engine.batch.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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

import org.stapledon.engine.batch.JsonBatchExecutionTracker;

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
}
