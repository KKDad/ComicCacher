package org.stapledon.engine.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
@DisplayName("StartupJobRunner")
class StartupJobRunnerTest {

    private static final Executor INLINE = Runnable::run;

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

        startupJobRunner = new StartupJobRunner(schedulers, INLINE);
        startupJobRunner.onApplicationReady(event);

        verify(scheduler1).runMissedExecutionIfNeeded();
        verify(scheduler2).runMissedExecutionIfNeeded();
    }

    @Test
    @DisplayName("should return straight away and run the makeup checks in order in the background")
    void shouldReturnBeforeMakeupRunsFinish() throws InterruptedException {
        lenient().when(scheduler1.getJobName()).thenReturn("TestJob1");
        lenient().when(scheduler2.getJobName()).thenReturn("TestJob2");

        CountDownLatch release = new CountDownLatch(1);
        List<String> threads = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            threads.add(Thread.currentThread().getName());
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(scheduler1).runMissedExecutionIfNeeded();

        Map<String, DailyJobScheduler> schedulers = new LinkedHashMap<>();
        schedulers.put("testJob1Scheduler", scheduler1);
        schedulers.put("testJob2Scheduler", scheduler2);

        startupJobRunner = new StartupJobRunner(schedulers);
        // scheduler1 blocks until released, so this only returns in time if the checks run on another thread
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> startupJobRunner.onApplicationReady(event));

        verify(scheduler1, timeout(2000)).runMissedExecutionIfNeeded();
        verify(scheduler2, never()).runMissedExecutionIfNeeded();

        release.countDown();

        verify(scheduler2, timeout(2000)).runMissedExecutionIfNeeded();
        InOrder order = inOrder(scheduler1, scheduler2);
        order.verify(scheduler1).runMissedExecutionIfNeeded();
        order.verify(scheduler2).runMissedExecutionIfNeeded();
        assertThat(threads).containsExactly("startup-catch-up");
    }

    @Test
    @DisplayName("should run PromoteFromDevJob's makeup run before the others")
    void shouldRunPromoteFromDevFirst() {
        DailyJobScheduler promote = mock(DailyJobScheduler.class);
        lenient().when(scheduler1.getJobName()).thenReturn("ComicDownloadJob");
        lenient().when(scheduler2.getJobName()).thenReturn("SourceCatalogJob");
        lenient().when(promote.getJobName()).thenReturn("PromoteFromDevJob");

        Map<String, DailyJobScheduler> schedulers = new LinkedHashMap<>();
        schedulers.put("comicDownloadJobScheduler", scheduler1);
        schedulers.put("sourceCatalogJobScheduler", scheduler2);
        schedulers.put("promoteFromDevJobScheduler", promote);

        startupJobRunner = new StartupJobRunner(schedulers, INLINE);
        startupJobRunner.onApplicationReady(event);

        InOrder order = inOrder(promote, scheduler1, scheduler2);
        order.verify(promote).runMissedExecutionIfNeeded();
        order.verify(scheduler1).runMissedExecutionIfNeeded();
        order.verify(scheduler2).runMissedExecutionIfNeeded();
    }

    @Test
    @DisplayName("should handle empty scheduler map gracefully")
    void shouldHandleEmptySchedulerMapGracefully() {
        startupJobRunner = new StartupJobRunner(Collections.emptyMap(), INLINE);
        startupJobRunner.onApplicationReady(event);

        // No exception should be thrown
    }

    @Test
    @DisplayName("should handle null scheduler map gracefully")
    void shouldHandleNullSchedulerMapGracefully() {
        startupJobRunner = new StartupJobRunner(null, INLINE);
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

        startupJobRunner = new StartupJobRunner(schedulers, INLINE);
        startupJobRunner.onApplicationReady(event);

        // Both schedulers should be called, even if one throws
        verify(scheduler1).runMissedExecutionIfNeeded();
        verify(scheduler2).runMissedExecutionIfNeeded();
    }

    @Test
    @DisplayName("should not call any methods when no schedulers present")
    void shouldNotCallMethodsWhenNoSchedulersPresent() {
        DailyJobScheduler unusedScheduler = mock(DailyJobScheduler.class);

        startupJobRunner = new StartupJobRunner(Collections.emptyMap(), INLINE);
        startupJobRunner.onApplicationReady(event);

        verify(unusedScheduler, never()).runMissedExecutionIfNeeded();
    }
}
