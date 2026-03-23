package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.stapledon.api.dto.batch.BatchSchedulerInfoDto;
import org.stapledon.api.dto.payload.MutationPayloads.ToggleJobSchedulerPayload;
import org.stapledon.api.dto.payload.MutationPayloads.TriggerBatchJobPayload;
import org.stapledon.engine.batch.BatchJobMonitoringService;
import org.stapledon.engine.batch.dto.BatchExecutionSummary;
import org.stapledon.engine.batch.logging.BatchJobLogService;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;
import org.stapledon.engine.batch.scheduler.SchedulerStateService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchJobResolver")
class BatchJobResolverTest {

    @Mock
    private BatchJobMonitoringService monitoringService;

    @Mock
    private SchedulerStateService schedulerStateService;

    @Mock
    private BatchJobLogService batchJobLogService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private BatchJobResolver createResolver(List<DailyJobScheduler> schedulerList) {
        var resolver = new BatchJobResolver(monitoringService, schedulerList, schedulerStateService, batchJobLogService);
        ReflectionTestUtils.setField(resolver, "batchTimezone", "America/Toronto");
        return resolver;
    }

    private JobExecution createJobExecution(Long id, String jobName, BatchStatus status) {
        JobInstance jobInstance = new JobInstance(id, jobName);
        JobExecution execution = new JobExecution(id, jobInstance, new JobParameters());
        execution.setStatus(status);
        execution.setStartTime(LocalDateTime.now());
        execution.setExitStatus(ExitStatus.EXECUTING);
        return execution;
    }

    // =========================================================================
    // batchSchedulers
    // =========================================================================

    @Nested
    @DisplayName("batchSchedulers")
    class BatchSchedulersTests {

        @Mock
        private DailyJobScheduler comicDownloadScheduler;

        @Mock
        private DailyJobScheduler comicBackfillScheduler;

        @Test
        @DisplayName("should return scheduler info for all jobs")
        void shouldReturnSchedulerInfo() {
            when(comicDownloadScheduler.getJobName()).thenReturn("ComicDownloadJob");
            when(comicDownloadScheduler.getCronExpression()).thenReturn("0 0 6 * * ?");
            when(comicDownloadScheduler.getTimezone()).thenReturn("America/Toronto");
            when(comicDownloadScheduler.getParameterDefinitions()).thenReturn(List.of());
            when(comicBackfillScheduler.getJobName()).thenReturn("ComicBackfillJob");
            when(comicBackfillScheduler.getCronExpression()).thenReturn("0 0 7 * * ?");
            when(comicBackfillScheduler.getTimezone()).thenReturn("America/Toronto");
            when(comicBackfillScheduler.getParameterDefinitions()).thenReturn(List.of());

            when(schedulerStateService.isPaused("ComicDownloadJob")).thenReturn(false);
            when(schedulerStateService.isPaused("ComicBackfillJob")).thenReturn(true);
            when(schedulerStateService.getState("ComicDownloadJob")).thenReturn(Optional.empty());
            when(schedulerStateService.getState("ComicBackfillJob"))
                    .thenReturn(Optional.of(new SchedulerStateService.SchedulerState(true, LocalDateTime.of(2026, 3, 17, 10, 0), "admin")));

            var resolver = createResolver(List.of(comicDownloadScheduler, comicBackfillScheduler));
            List<BatchSchedulerInfoDto> result = resolver.batchSchedulers();

            assertThat(result).hasSize(2);

            BatchSchedulerInfoDto download = result.stream().filter(s -> s.jobName().equals("ComicDownloadJob")).findFirst().orElseThrow();
            assertThat(download.paused()).isFalse();
            assertThat(download.enabled()).isTrue();
            assertThat(download.cronExpression()).isEqualTo("0 0 6 * * ?");

            BatchSchedulerInfoDto backfill = result.stream().filter(s -> s.jobName().equals("ComicBackfillJob")).findFirst().orElseThrow();
            assertThat(backfill.paused()).isTrue();
            assertThat(backfill.toggledBy()).isEqualTo("admin");
        }
    }

    // =========================================================================
    // batchJobLog
    // =========================================================================

    @Nested
    @DisplayName("batchJobLog")
    class BatchJobLogTests {

        @Test
        @DisplayName("should return log content when available")
        void shouldReturnLogContent() {
            var summary = BatchExecutionSummary.builder()
                    .executionId(42L)
                    .jobName("ComicDownloadJob")
                    .logFileName("ComicDownloadJob-20260320-a3f7b2c1.log")
                    .build();
            when(monitoringService.getExecutionSummary(42)).thenReturn(Optional.of(summary));
            when(batchJobLogService.getExecutionLog("ComicDownloadJob", "ComicDownloadJob-20260320-a3f7b2c1.log"))
                    .thenReturn(Optional.of("INFO Job started"));

            var resolver = createResolver(List.of());
            String log = resolver.batchJobLog(42, "ComicDownloadJob");

            assertThat(log).isEqualTo("INFO Job started");
        }

        @Test
        @DisplayName("should return null when log not available")
        void shouldReturnNullForMissingLog() {
            when(monitoringService.getExecutionSummary(99)).thenReturn(Optional.empty());

            var resolver = createResolver(List.of());
            String log = resolver.batchJobLog(99, "ComicDownloadJob");

            assertThat(log).isNull();
        }
    }

    // =========================================================================
    // triggerJob
    // =========================================================================

    @Nested
    @DisplayName("triggerJob")
    class TriggerJobTests {

        @Mock
        private DailyJobScheduler comicDownloadScheduler;

        @Test
        @DisplayName("should trigger job and return execution")
        void shouldTriggerJob() {
            when(comicDownloadScheduler.getJobName()).thenReturn("ComicDownloadJob");
            when(comicDownloadScheduler.triggerManually(anyMap())).thenReturn(1L);

            JobExecution execution = createJobExecution(1L, "ComicDownloadJob", BatchStatus.STARTED);
            when(monitoringService.getJobExecution(1L)).thenReturn(execution);

            var resolver = createResolver(List.of(comicDownloadScheduler));
            TriggerBatchJobPayload result = resolver.triggerJob("ComicDownloadJob", null);

            assertThat(result.errors()).isEmpty();
            assertThat(result.batchJob()).isNotNull();
            assertThat(result.batchJob().jobName()).isEqualTo("ComicDownloadJob");
        }

        @Test
        @DisplayName("should return error for unavailable job")
        void shouldReturnErrorForUnavailableJob() {
            var resolver = createResolver(List.of());
            TriggerBatchJobPayload result = resolver.triggerJob("NonexistentJob", null);

            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().getFirst().message()).contains("not available");
            assertThat(result.batchJob()).isNull();
        }

        @Test
        @DisplayName("should return error when trigger returns null")
        void shouldReturnErrorWhenTriggerFails() {
            when(comicDownloadScheduler.getJobName()).thenReturn("ComicDownloadJob");
            when(comicDownloadScheduler.triggerManually(anyMap())).thenReturn(null);

            var resolver = createResolver(List.of(comicDownloadScheduler));
            TriggerBatchJobPayload result = resolver.triggerJob("ComicDownloadJob", null);

            assertThat(result.errors()).hasSize(1);
            assertThat(result.batchJob()).isNull();
        }
    }

    // =========================================================================
    // toggleJobScheduler
    // =========================================================================

    @Nested
    @DisplayName("toggleJobScheduler")
    class ToggleJobSchedulerTests {

        @Mock
        private DailyJobScheduler comicDownloadScheduler;

        @Test
        @DisplayName("should toggle scheduler pause state")
        void shouldTogglePauseState() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("admin", null));

            when(comicDownloadScheduler.getJobName()).thenReturn("ComicDownloadJob");
            when(comicDownloadScheduler.getCronExpression()).thenReturn("0 0 6 * * ?");
            when(comicDownloadScheduler.getTimezone()).thenReturn("America/Toronto");
            when(comicDownloadScheduler.getParameterDefinitions()).thenReturn(List.of());
            when(schedulerStateService.isPaused("ComicDownloadJob")).thenReturn(true);
            when(schedulerStateService.getState("ComicDownloadJob"))
                    .thenReturn(Optional.of(new SchedulerStateService.SchedulerState(true, LocalDateTime.now(), "admin")));

            var resolver = createResolver(List.of(comicDownloadScheduler));
            ToggleJobSchedulerPayload result = resolver.toggleJobScheduler("ComicDownloadJob", true);

            assertThat(result.errors()).isEmpty();
            assertThat(result.scheduler()).isNotNull();
            assertThat(result.scheduler().paused()).isTrue();
            verify(schedulerStateService).setPaused("ComicDownloadJob", true, "admin");
        }

        @Test
        @DisplayName("should return error when toggling unavailable job")
        void shouldReturnErrorForUnavailableJobToggle() {
            var resolver = createResolver(List.of());
            ToggleJobSchedulerPayload result = resolver.toggleJobScheduler("NonexistentJob", true);

            assertThat(result.errors()).hasSize(1);
            assertThat(result.scheduler()).isNull();
        }
    }

    // =========================================================================
    // Backward compatibility
    // =========================================================================

    @Nested
    @DisplayName("backward compatibility")
    class BackwardCompatTests {

        @Mock
        private DailyJobScheduler comicDownloadScheduler;

        @Mock
        private DailyJobScheduler comicBackfillScheduler;

        @Test
        @DisplayName("triggerBatchJob delegates to triggerJob with ComicDownloadJob")
        void triggerBatchJobDelegatesToTriggerJob() {
            when(comicBackfillScheduler.getJobName()).thenReturn("ComicBackfillJob");
            when(comicDownloadScheduler.getJobName()).thenReturn("ComicDownloadJob");
            when(comicDownloadScheduler.triggerManually(anyMap())).thenReturn(1L);

            JobExecution execution = createJobExecution(1L, "ComicDownloadJob", BatchStatus.STARTED);
            when(monitoringService.getJobExecution(1L)).thenReturn(execution);

            // Backfill scheduler listed first forces stream to call getJobName() on it before finding the match
            var resolver = createResolver(List.of(comicBackfillScheduler, comicDownloadScheduler));
            TriggerBatchJobPayload result = resolver.triggerBatchJob();

            assertThat(result.batchJob().jobName()).isEqualTo("ComicDownloadJob");
        }

        @Test
        @DisplayName("triggerBackfillJob delegates to triggerJob with ComicBackfillJob")
        void triggerBackfillJobDelegatesToTriggerJob() {
            when(comicDownloadScheduler.getJobName()).thenReturn("ComicDownloadJob");
            when(comicBackfillScheduler.getJobName()).thenReturn("ComicBackfillJob");
            when(comicBackfillScheduler.triggerManually(anyMap())).thenReturn(2L);

            JobExecution execution = createJobExecution(2L, "ComicBackfillJob", BatchStatus.STARTED);
            when(monitoringService.getJobExecution(2L)).thenReturn(execution);

            // Download scheduler listed first forces stream to call getJobName() on it before finding backfill
            var resolver = createResolver(List.of(comicDownloadScheduler, comicBackfillScheduler));
            TriggerBatchJobPayload result = resolver.triggerBackfillJob();

            assertThat(result.batchJob().jobName()).isEqualTo("ComicBackfillJob");
        }
    }
}
