package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import java.io.File;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.service.AnalysisService;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.common.service.ValidationService;
import org.stapledon.engine.batch.config.ImageMetadataBackfillJobConfig;
import org.stapledon.engine.storage.ImageMetadataRepository;

@ExtendWith(MockitoExtension.class)
class ImageMetadataBackfillJobConfigTest {

    @Mock
    private CacheProperties cacheProperties;

    @Mock
    private ValidationService imageValidationService;

    @Mock
    private AnalysisService imageAnalysisService;

    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @Mock
    private ComicConfigurationService comicConfigurationService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JsonBatchExecutionTracker jsonBatchExecutionTracker;

    @TempDir
    File tempDir;

    private ImageMetadataBackfillJobConfig config;

    @BeforeEach
    void setUp() {
        config = new ImageMetadataBackfillJobConfig(cacheProperties, imageValidationService, imageAnalysisService, imageMetadataRepository, comicConfigurationService);
        setField(config, "batchSize", 100);
    }

    @Test
    void imageMetadataBackfillJob_shouldBeCreated() {
        Step mockStep = mock(Step.class);

        Job job = config.imageMetadataBackfillJob(jobRepository, mockStep, jsonBatchExecutionTracker);

        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("ImageMetadataBackfillJob");
    }

    @Test
    void imageBackfillTasklet_withNullParam_usesPropertyDefault() throws Exception {
        when(cacheProperties.getLocation()).thenReturn(tempDir.getAbsolutePath());

        Tasklet tasklet = config.imageBackfillTasklet(null);
        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    void imageBackfillTasklet_withParam_usesOverride() throws Exception {
        when(cacheProperties.getLocation()).thenReturn(tempDir.getAbsolutePath());

        Tasklet tasklet = config.imageBackfillTasklet("50");
        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    void imageBackfillTasklet_withNonExistentCacheDir_finishesGracefully() throws Exception {
        when(cacheProperties.getLocation()).thenReturn("/nonexistent/path");

        Tasklet tasklet = config.imageBackfillTasklet(null);
        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
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
