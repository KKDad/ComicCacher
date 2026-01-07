package org.stapledon.engine.batch.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StartupJobRunner")
class StartupJobRunnerTest {

    @Mock
    private DailyJobScheduler scheduler1;

    @Mock
    private DailyJobScheduler scheduler2;

    @Mock
    private ApplicationReadyEvent event;

    private StartupJobRunner startupJobRunner;

    @Test
    @DisplayName("should call runMissedExecutionIfNeeded on all schedulers")
    void shouldCallRunMissedExecutionOnAllSchedulers() {
        lenient().when(scheduler1.getJobName()).thenReturn("TestJob1");
        lenient().when(scheduler2.getJobName()).thenReturn("TestJob2");

        Map<String, DailyJobScheduler> schedulers = new HashMap<>();
        schedulers.put("testJob1Scheduler", scheduler1);
        schedulers.put("testJob2Scheduler", scheduler2);

        startupJobRunner = new StartupJobRunner(schedulers);
        startupJobRunner.onApplicationReady(event);

        verify(scheduler1).runMissedExecutionIfNeeded();
        verify(scheduler2).runMissedExecutionIfNeeded();
    }

    @Test
    @DisplayName("should handle empty scheduler map gracefully")
    void shouldHandleEmptySchedulerMapGracefully() {
        startupJobRunner = new StartupJobRunner(Collections.emptyMap());
        startupJobRunner.onApplicationReady(event);

        // No exception should be thrown
    }

    @Test
    @DisplayName("should handle null scheduler map gracefully")
    void shouldHandleNullSchedulerMapGracefully() {
        startupJobRunner = new StartupJobRunner(null);
        startupJobRunner.onApplicationReady(event);

        // No exception should be thrown
    }

    @Test
    @DisplayName("should continue processing other schedulers when one throws exception")
    void shouldContinueProcessingWhenOneSchedulerThrows() {
        lenient().when(scheduler1.getJobName()).thenReturn("TestJob1");
        lenient().when(scheduler2.getJobName()).thenReturn("TestJob2");

        Map<String, DailyJobScheduler> schedulers = new HashMap<>();
        schedulers.put("testJob1Scheduler", scheduler1);
        schedulers.put("testJob2Scheduler", scheduler2);

        doThrow(new RuntimeException("Test exception")).when(scheduler1).runMissedExecutionIfNeeded();

        startupJobRunner = new StartupJobRunner(schedulers);
        startupJobRunner.onApplicationReady(event);

        // Both schedulers should be called, even if one throws
        verify(scheduler1).runMissedExecutionIfNeeded();
        verify(scheduler2).runMissedExecutionIfNeeded();
    }

    @Test
    @DisplayName("should not call any methods when no schedulers present")
    void shouldNotCallMethodsWhenNoSchedulersPresent() {
        DailyJobScheduler unusedScheduler = mock(DailyJobScheduler.class);

        startupJobRunner = new StartupJobRunner(Collections.emptyMap());
        startupJobRunner.onApplicationReady(event);

        verify(unusedScheduler, never()).runMissedExecutionIfNeeded();
    }
}
