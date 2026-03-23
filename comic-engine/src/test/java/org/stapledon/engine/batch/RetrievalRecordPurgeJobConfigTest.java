package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import org.stapledon.engine.batch.config.RetrievalRecordPurgeJobConfig;
import org.stapledon.engine.batch.logging.BatchJobLogService;
import org.stapledon.engine.management.ManagementFacade;

@ExtendWith(MockitoExtension.class)
class RetrievalRecordPurgeJobConfigTest {

    @Mock
    private BatchJobLogService batchJobLogService;

    @Mock
    private ManagementFacade managementFacade;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JsonBatchExecutionTracker jsonBatchExecutionTracker;

    private RetrievalRecordPurgeJobConfig config;

    @BeforeEach
    void setUp() {
        config = new RetrievalRecordPurgeJobConfig(batchJobLogService, managementFacade);
        setField(config, "daysToKeep", 30);
    }

    @Test
    void retrievalRecordPurgeJob_shouldBeCreated() {
        Step mockStep1 = mock(Step.class);
        Step mockStep2 = mock(Step.class);

        Job job = config.retrievalRecordPurgeJob(jobRepository, mockStep1, mockStep2, jsonBatchExecutionTracker);

        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("RetrievalRecordPurgeJob");
    }

    @Test
    void recordPurgeTasklet_withNullParam_usesPropertyDefault() throws Exception {
        when(managementFacade.purgeOldRetrievalRecords(30)).thenReturn(5);

        Tasklet tasklet = config.recordPurgeTasklet(null);
        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(managementFacade).purgeOldRetrievalRecords(eq(30));
    }

    @Test
    void recordPurgeTasklet_withParam_usesOverride() throws Exception {
        when(managementFacade.purgeOldRetrievalRecords(7)).thenReturn(10);

        Tasklet tasklet = config.recordPurgeTasklet("7");
        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(managementFacade).purgeOldRetrievalRecords(eq(7));
    }

    @Test
    void logPurgeTasklet_withNullParam_usesPropertyDefault() throws Exception {
        when(batchJobLogService.purgeOldLogFiles(30)).thenReturn(3);

        Tasklet tasklet = config.logPurgeTasklet(null);
        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(batchJobLogService).purgeOldLogFiles(eq(30));
    }

    @Test
    void logPurgeTasklet_withParam_usesOverride() throws Exception {
        when(batchJobLogService.purgeOldLogFiles(14)).thenReturn(8);

        Tasklet tasklet = config.logPurgeTasklet("14");
        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(batchJobLogService).purgeOldLogFiles(eq(14));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
