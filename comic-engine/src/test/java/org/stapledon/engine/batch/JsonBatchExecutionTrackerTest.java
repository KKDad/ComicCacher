package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.engine.batch.dto.BatchExecutionSummary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class JsonBatchExecutionTrackerTest {

    @TempDir
    Path tempDir;

    private JsonBatchExecutionTracker tracker;
    private CacheProperties cacheProperties;
    private Gson gson;

    @BeforeEach
    void setUp() {
        cacheProperties = mock(CacheProperties.class);
        when(cacheProperties.getLocation()).thenReturn(tempDir.toString());

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        tracker = new JsonBatchExecutionTracker(cacheProperties, gson, 5);
    }

    @Test
    void afterJobPersistsExecutionToJson() {
        JobExecution execution = createJobExecution("TestJob", 1L, BatchStatus.COMPLETED);

        tracker.afterJob(execution);

        Optional<BatchExecutionSummary> result = tracker.getLastExecution("TestJob");
        assertThat(result).isPresent();
        assertThat(result.get().getJobName()).isEqualTo("TestJob");
        assertThat(result.get().getStatus()).isEqualTo("COMPLETED");
        assertThat(result.get().getExecutionId()).isEqualTo(1L);
    }

    @Test
    void afterJobPrependsNewExecutionsToHistory() {
        tracker.afterJob(createJobExecution("TestJob", 1L, BatchStatus.COMPLETED));
        tracker.afterJob(createJobExecution("TestJob", 2L, BatchStatus.FAILED));
        tracker.afterJob(createJobExecution("TestJob", 3L, BatchStatus.COMPLETED));

        List<BatchExecutionSummary> history = tracker.getExecutionHistory("TestJob", 10);
        assertThat(history).hasSize(3);
        assertThat(history.get(0).getExecutionId()).isEqualTo(3L);
        assertThat(history.get(1).getExecutionId()).isEqualTo(2L);
        assertThat(history.get(2).getExecutionId()).isEqualTo(1L);
    }

    @Test
    void afterJobTrimsHistoryToCap() {
        for (long i = 1; i <= 8; i++) {
            tracker.afterJob(createJobExecution("TestJob", i, BatchStatus.COMPLETED));
        }

        List<BatchExecutionSummary> history = tracker.getExecutionHistory("TestJob", 10);
        assertThat(history).hasSize(5);
        assertThat(history.get(0).getExecutionId()).isEqualTo(8L);
        assertThat(history.get(4).getExecutionId()).isEqualTo(4L);
    }

    @Test
    void afterJobCapturesStepDetails() {
        JobExecution execution = createJobExecution("TestJob", 1L, BatchStatus.COMPLETED);
        StepExecution step = new StepExecution("downloadStep", execution);
        step.setStatus(BatchStatus.COMPLETED);
        step.setReadCount(100);
        step.setWriteCount(95);
        step.setFilterCount(5);
        step.setStartTime(LocalDateTime.of(2026, 3, 17, 6, 0, 0));
        step.setEndTime(LocalDateTime.of(2026, 3, 17, 6, 10, 0));
        execution.addStepExecutions(List.of(step));

        tracker.afterJob(execution);

        BatchExecutionSummary summary = tracker.getLastExecution("TestJob").orElseThrow();
        assertThat(summary.getSteps()).hasSize(1);
        assertThat(summary.getSteps().getFirst().stepName()).isEqualTo("downloadStep");
        assertThat(summary.getSteps().getFirst().readCount()).isEqualTo(100);
        assertThat(summary.getSteps().getFirst().writeCount()).isEqualTo(95);
    }

    @Test
    void afterJobCapturesParameters() {
        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", "2026-03-17")
                .toJobParameters();

        JobExecution execution = createJobExecution("TestJob", 1L, BatchStatus.COMPLETED, params);

        tracker.afterJob(execution);

        BatchExecutionSummary summary = tracker.getLastExecution("TestJob").orElseThrow();
        assertThat(summary.getParameters()).containsEntry("targetDate", "2026-03-17");
    }

    @Test
    void afterJobCapturesErrorMessageOnFailure() {
        JobExecution execution = createJobExecution("TestJob", 1L, BatchStatus.FAILED);
        execution.addFailureException(new RuntimeException("Download timeout"));

        tracker.afterJob(execution);

        BatchExecutionSummary summary = tracker.getLastExecution("TestJob").orElseThrow();
        assertThat(summary.getErrorMessage()).isEqualTo("Download timeout");
    }

    @Test
    void getLastExecutionReturnsEmptyWhenNoHistory() {
        assertThat(tracker.getLastExecution("NonExistentJob")).isEmpty();
    }

    @Test
    void getExecutionHistoryReturnsEmptyListForUnknownJob() {
        assertThat(tracker.getExecutionHistory("NonExistentJob", 10)).isEmpty();
    }

    @Test
    void getExecutionHistoryRespectsCountLimit() {
        for (long i = 1; i <= 5; i++) {
            tracker.afterJob(createJobExecution("TestJob", i, BatchStatus.COMPLETED));
        }

        assertThat(tracker.getExecutionHistory("TestJob", 3)).hasSize(3);
    }

    @Test
    void getExecutionHistoryForDateRangeFiltersCorrectly() {
        tracker.afterJob(createJobExecutionWithTimes("TestJob", 1L,
                LocalDateTime.of(2026, 3, 15, 6, 0), LocalDateTime.of(2026, 3, 15, 6, 30)));
        tracker.afterJob(createJobExecutionWithTimes("TestJob", 2L,
                LocalDateTime.of(2026, 3, 16, 6, 0), LocalDateTime.of(2026, 3, 16, 6, 30)));
        tracker.afterJob(createJobExecutionWithTimes("TestJob", 3L,
                LocalDateTime.of(2026, 3, 17, 6, 0), LocalDateTime.of(2026, 3, 17, 6, 30)));

        List<BatchExecutionSummary> result = tracker.getExecutionHistoryForDateRange(
                "TestJob", LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 17));
        assertThat(result).hasSize(2);
    }

    @Test
    void getExecutionFindsById() {
        tracker.afterJob(createJobExecution("JobA", 1L, BatchStatus.COMPLETED));
        tracker.afterJob(createJobExecution("JobB", 2L, BatchStatus.FAILED));

        Optional<BatchExecutionSummary> result = tracker.getExecution(2L);
        assertThat(result).isPresent();
        assertThat(result.get().getJobName()).isEqualTo("JobB");
    }

    @Test
    void getExecutionReturnsEmptyForUnknownId() {
        tracker.afterJob(createJobExecution("TestJob", 1L, BatchStatus.COMPLETED));
        assertThat(tracker.getExecution(999L)).isEmpty();
    }

    @Test
    void hasJobRunTodayReturnsTrueWhenJobRanToday() {
        JobExecution execution = createJobExecutionWithTimes("TestJob", 1L,
                LocalDateTime.now().minusHours(1), LocalDateTime.now());

        tracker.afterJob(execution);

        assertThat(tracker.hasJobRunToday("TestJob")).isTrue();
    }

    @Test
    void hasJobRunTodayReturnsFalseWhenJobRanYesterday() {
        JobExecution execution = createJobExecutionWithTimes("TestJob", 1L,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusMinutes(30));

        tracker.afterJob(execution);

        assertThat(tracker.hasJobRunToday("TestJob")).isFalse();
    }

    @Test
    void hasJobRunSinceReturnsTrueWhenJobRanAfterThreshold() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        JobExecution execution = createJobExecutionWithTimes("TestJob", 1L,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusMinutes(30));

        tracker.afterJob(execution);

        assertThat(tracker.hasJobRunSince("TestJob", threshold)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "COMPLETED, COMPLETED",
            "FAILED, FAILED",
            "STARTED, STARTED"
    })
    void afterJobCapturesCorrectStatus(String batchStatusName, String expectedStatus) {
        BatchStatus batchStatus = BatchStatus.valueOf(batchStatusName);
        tracker.afterJob(createJobExecution("TestJob", 1L, batchStatus));

        BatchExecutionSummary summary = tracker.getLastExecution("TestJob").orElseThrow();
        assertThat(summary.getStatus()).isEqualTo(expectedStatus);
    }

    @Test
    void parseWithMigrationHandlesLegacySingleEntryFormat() {
        String legacyJson = """
                {
                  "ComicDownloadJob": {
                    "executionId": 42,
                    "status": "COMPLETED",
                    "startTime": "2026-03-16T06:00:00",
                    "endTime": "2026-03-16T06:30:00",
                    "exitCode": "COMPLETED"
                  }
                }
                """;

        Map<String, List<BatchExecutionSummary>> result = tracker.parseWithMigration(legacyJson);
        assertThat(result).containsKey("ComicDownloadJob");
        assertThat(result.get("ComicDownloadJob")).hasSize(1);
        assertThat(result.get("ComicDownloadJob").getFirst().getStatus()).isEqualTo("COMPLETED");
        assertThat(result.get("ComicDownloadJob").getFirst().getJobName()).isEqualTo("ComicDownloadJob");
    }

    @Test
    void parseWithMigrationHandlesNewListFormat() {
        String newJson = """
                {
                  "ComicDownloadJob": [
                    {
                      "executionId": 2,
                      "jobName": "ComicDownloadJob",
                      "status": "COMPLETED",
                      "startTime": "2026-03-17T06:00:00",
                      "endTime": "2026-03-17T06:30:00"
                    },
                    {
                      "executionId": 1,
                      "jobName": "ComicDownloadJob",
                      "status": "FAILED",
                      "startTime": "2026-03-16T06:00:00",
                      "endTime": "2026-03-16T06:30:00"
                    }
                  ]
                }
                """;

        Map<String, List<BatchExecutionSummary>> result = tracker.parseWithMigration(newJson);
        assertThat(result.get("ComicDownloadJob")).hasSize(2);
        assertThat(result.get("ComicDownloadJob").get(0).getExecutionId()).isEqualTo(2L);
    }

    @Test
    void parseWithMigrationHandlesEmptyJson() {
        assertThat(tracker.parseWithMigration("{}")).isEmpty();
        assertThat(tracker.parseWithMigration("null")).isEmpty();
    }

    @Test
    void multipleJobsStoredIndependently() {
        tracker.afterJob(createJobExecution("JobA", 1L, BatchStatus.COMPLETED));
        tracker.afterJob(createJobExecution("JobB", 2L, BatchStatus.FAILED));
        tracker.afterJob(createJobExecution("JobA", 3L, BatchStatus.COMPLETED));

        assertThat(tracker.getExecutionHistory("JobA", 10)).hasSize(2);
        assertThat(tracker.getExecutionHistory("JobB", 10)).hasSize(1);
    }

    @Test
    void getAllExecutionHistorySortsByStartTimeDescending() {
        tracker.afterJob(createJobExecutionWithTimes("JobA", 1L,
                LocalDateTime.of(2026, 3, 15, 6, 0), LocalDateTime.of(2026, 3, 15, 6, 30)));
        tracker.afterJob(createJobExecutionWithTimes("JobB", 2L,
                LocalDateTime.of(2026, 3, 17, 6, 0), LocalDateTime.of(2026, 3, 17, 6, 30)));
        tracker.afterJob(createJobExecutionWithTimes("JobA", 3L,
                LocalDateTime.of(2026, 3, 16, 6, 0), LocalDateTime.of(2026, 3, 16, 6, 30)));

        List<BatchExecutionSummary> all = tracker.getAllExecutionHistory(10);
        assertThat(all).hasSize(3);
        assertThat(all.get(0).getExecutionId()).isEqualTo(2L);
        assertThat(all.get(1).getExecutionId()).isEqualTo(3L);
        assertThat(all.get(2).getExecutionId()).isEqualTo(1L);
    }

    @Test
    void atomicWriteCreatesFileOnFirstExecution() {
        tracker.afterJob(createJobExecution("TestJob", 1L, BatchStatus.COMPLETED));

        Path jsonFile = tempDir.resolve("batch-executions.json");
        assertThat(Files.exists(jsonFile)).isTrue();
    }

    // =========================================================================
    // Test Helpers
    // =========================================================================

    private JobExecution createJobExecution(String jobName, long executionId, BatchStatus status) {
        return createJobExecution(jobName, executionId, status, new JobParameters());
    }

    private JobExecution createJobExecution(String jobName, long executionId, BatchStatus status, JobParameters params) {
        JobInstance jobInstance = new JobInstance(executionId, jobName);
        JobExecution execution = new JobExecution(executionId, jobInstance, params);
        execution.setStatus(status);
        execution.setExitStatus(new ExitStatus(status.name()));
        execution.setStartTime(LocalDateTime.of(2026, 3, 17, 6, 0, 0));
        execution.setEndTime(LocalDateTime.of(2026, 3, 17, 6, 30, 0));
        return execution;
    }

    private JobExecution createJobExecutionWithTimes(String jobName, long executionId,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        JobInstance jobInstance = new JobInstance(executionId, jobName);
        JobExecution execution = new JobExecution(executionId, jobInstance, new JobParameters());
        execution.setStatus(BatchStatus.COMPLETED);
        execution.setExitStatus(ExitStatus.COMPLETED);
        execution.setStartTime(startTime);
        execution.setEndTime(endTime);
        return execution;
    }

    /**
     * LocalDateTime adapter matching the production GsonProvider configuration.
     */
    static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(JsonWriter jsonWriter, LocalDateTime localDateTime) throws IOException {
            if (localDateTime == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(FORMATTER.format(localDateTime));
            }
        }

        @Override
        public LocalDateTime read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return LocalDateTime.parse(jsonReader.nextString(), FORMATTER);
        }
    }
}
