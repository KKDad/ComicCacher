package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.time.LocalDate;
import java.util.List;

import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.engine.batch.config.ComicRetrievalJobConfig;
import org.stapledon.engine.management.ManagementFacade;

@ExtendWith(MockitoExtension.class)
class ComicRetrievalJobConfigTest {

    @Mock
    private ManagementFacade managementFacade;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JsonBatchExecutionTracker jsonBatchExecutionTracker;

    private ComicRetrievalJobConfig config;

    @BeforeEach
    void setUp() {
        config = new ComicRetrievalJobConfig(managementFacade);
    }

    @Test
    void comicDownloadJob_shouldBeCreated() {
        Step mockStep = mock(Step.class);

        Job job = config.comicDownloadJob(jobRepository, mockStep, jsonBatchExecutionTracker);

        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("ComicDownloadJob");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ALL"})
    void comicProcessor_withNoSourceFilter_downloadsAllSources(String sourceFilter) throws Exception {
        LocalDate date = LocalDate.of(2026, 3, 23);
        when(managementFacade.updateComicsForDate(date, sourceFilter)).thenReturn(List.of());

        ItemProcessor<LocalDate, List<ComicDownloadResult>> processor = config.comicProcessor(sourceFilter);
        List<ComicDownloadResult> results = processor.process(date);

        assertThat(results).isEmpty();
        verify(managementFacade).updateComicsForDate(date, sourceFilter);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gocomics", "comicskingdom", "freefall"})
    void comicProcessor_withSourceFilter_passesFilterToFacade(String sourceFilter) throws Exception {
        LocalDate date = LocalDate.of(2026, 3, 23);
        when(managementFacade.updateComicsForDate(date, sourceFilter)).thenReturn(List.of());

        ItemProcessor<LocalDate, List<ComicDownloadResult>> processor = config.comicProcessor(sourceFilter);
        List<ComicDownloadResult> results = processor.process(date);

        assertThat(results).isEmpty();
        verify(managementFacade).updateComicsForDate(date, sourceFilter);
    }
}
