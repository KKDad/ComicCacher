package org.stapledon.api.resolver;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;
import org.stapledon.engine.batch.scheduler.DailyJobScheduler;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for BatchJobResolver GraphQL operations.
 * Batch jobs are disabled in integration profile, so queries return empty lists.
 */
@Slf4j
class BatchJobResolverIT extends AbstractHttpGraphQlIntegrationTest {

    private static final String QUERY_RECENT_BATCH_JOBS = """
            query RecentBatchJobs($count: Int) {
                recentBatchJobs(count: $count) {
                    executionId
                    jobName
                    status
                    startTime
                }
            }
            """;

    private static final String QUERY_BATCH_JOB_SUMMARY = """
            query BatchJobSummary($days: Int) {
                batchJobSummary(days: $days) {
                    daysIncluded
                    totalExecutions
                    successCount
                    failureCount
                    runningCount
                }
            }
            """;

    private static final String QUERY_BATCH_SCHEDULERS = """
            query BatchSchedulers {
                batchSchedulers {
                    jobName
                    cronExpression
                    timezone
                    nextRunTime
                    enabled
                    paused
                    lastToggled
                    toggledBy
                }
            }
            """;

    private static final String QUERY_BATCH_JOB_LOG = """
            query BatchJobLog($executionId: Int!, $jobName: String!) {
                batchJobLog(executionId: $executionId, jobName: $jobName)
            }
            """;

    private static final String MUTATION_TRIGGER_JOB = """
            mutation TriggerJob($jobName: String!) {
                triggerJob(jobName: $jobName) {
                    batchJob {
                        executionId
                        jobName
                        status
                    }
                    errors {
                        message
                        field
                    }
                }
            }
            """;

    private static final String MUTATION_TOGGLE_SCHEDULER = """
            mutation ToggleScheduler($jobName: String!, $paused: Boolean!) {
                toggleJobScheduler(jobName: $jobName, paused: $paused) {
                    scheduler {
                        jobName
                        paused
                    }
                    errors {
                        message
                        field
                    }
                }
            }
            """;

    @Autowired
    private List<DailyJobScheduler> schedulers;

    @TestConfiguration
    static class LocalConfig {
        @Bean
        @Primary
        public List<DailyJobScheduler> schedulers() {
            var comicDownloadJob = Mockito.mock(DailyJobScheduler.class);
            when(comicDownloadJob.getJobName()).thenReturn("ComicDownloadJob");
            when(comicDownloadJob.getScheduleType()).thenReturn(DailyJobScheduler.ScheduleType.DAILY);
            when(comicDownloadJob.getCronExpression()).thenReturn("0 0 6 * * ?");
            when(comicDownloadJob.getTimezone()).thenReturn("America/Toronto");

            var comicBackfillJob = Mockito.mock(DailyJobScheduler.class);
            when(comicBackfillJob.getJobName()).thenReturn("ComicBackfillJob");
            when(comicBackfillJob.getScheduleType()).thenReturn(DailyJobScheduler.ScheduleType.DAILY);
            when(comicBackfillJob.getCronExpression()).thenReturn("0 0 7 * * ?");
            when(comicBackfillJob.getTimezone()).thenReturn("America/Toronto");

            return List.of(comicDownloadJob, comicBackfillJob);
        }
    }

    @BeforeEach
    void authenticate() {
        authenticateAsAdmin();
    }

    @Test
    void recentBatchJobs_returnsList() {
        getGraphQlTester()
                .document(QUERY_RECENT_BATCH_JOBS)
                .variable("count", 5)
                .execute()
                .errors().verify()
                .path("recentBatchJobs").entityList(Object.class).hasSize(0);
    }

    @Test
    void batchJobSummary_returnsSummary() {
        getGraphQlTester()
                .document(QUERY_BATCH_JOB_SUMMARY)
                .variable("days", 7)
                .execute()
                .errors().verify()
                .path("batchJobSummary.daysIncluded").entity(Integer.class).isEqualTo(7)
                .path("batchJobSummary.totalExecutions").entity(Integer.class).isEqualTo(0);
    }

    @Test
    void batchSchedulers_returnsSchedulerInfo() {
        getGraphQlTester()
                .document(QUERY_BATCH_SCHEDULERS)
                .execute()
                .errors().verify()
                .path("batchSchedulers").entityList(Object.class).hasSize(2)
                .path("batchSchedulers[0].jobName").entity(String.class).isEqualTo("ComicDownloadJob")
                .path("batchSchedulers[0].cronExpression").entity(String.class).isEqualTo("0 0 6 * * ?")
                .path("batchSchedulers[0].enabled").entity(Boolean.class).isEqualTo(true)
                .path("batchSchedulers[1].jobName").entity(String.class).isEqualTo("ComicBackfillJob");
    }

    @Test
    void batchJobLog_returnsNullForMissingExecution() {
        getGraphQlTester()
                .document(QUERY_BATCH_JOB_LOG)
                .variable("executionId", 999)
                .variable("jobName", "ComicDownloadJob")
                .execute()
                .errors().verify()
                .path("batchJobLog").valueIsNull();
    }

    @Test
    void triggerJob_returnsErrorForUnavailableJob() {
        getGraphQlTester()
                .document(MUTATION_TRIGGER_JOB)
                .variable("jobName", "NonexistentJob")
                .execute()
                .errors().verify()
                .path("triggerJob.batchJob").valueIsNull()
                .path("triggerJob.errors[0].message").entity(String.class).matches(msg -> msg.contains("not available"));
    }

    @Test
    void triggerJob_returnsErrorWhenTriggerFails() {
        getGraphQlTester()
                .document(MUTATION_TRIGGER_JOB)
                .variable("jobName", "ComicDownloadJob")
                .execute()
                .errors().verify()
                .path("triggerJob.batchJob").valueIsNull()
                .path("triggerJob.errors").entityList(Object.class).hasSizeGreaterThan(0);
    }

    @Test
    void toggleJobScheduler_returnsErrorForUnavailableJob() {
        getGraphQlTester()
                .document(MUTATION_TOGGLE_SCHEDULER)
                .variable("jobName", "NonexistentJob")
                .variable("paused", true)
                .execute()
                .errors().verify()
                .path("toggleJobScheduler.scheduler").valueIsNull()
                .path("toggleJobScheduler.errors[0].message").entity(String.class).matches(msg -> msg.contains("not available"));
    }

    @Test
    void toggleJobScheduler_pausesAvailableJob() {
        getGraphQlTester()
                .document(MUTATION_TOGGLE_SCHEDULER)
                .variable("jobName", "ComicBackfillJob")
                .variable("paused", true)
                .execute()
                .errors().verify()
                .path("toggleJobScheduler.errors").entityList(Object.class).hasSize(0)
                .path("toggleJobScheduler.scheduler.jobName").entity(String.class).isEqualTo("ComicBackfillJob")
                .path("toggleJobScheduler.scheduler.paused").entity(Boolean.class).isEqualTo(true);
    }
}
