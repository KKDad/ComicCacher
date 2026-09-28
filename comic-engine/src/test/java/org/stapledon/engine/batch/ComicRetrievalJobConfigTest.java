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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
        config = new ComicRetrievalJobConfig(managementFacade, Clock.systemDefaultZone());
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

    @Test
    void dateReader_readsTodayInTheBatchTimezone() throws Exception {
        // 22:00 in Toronto on the 28th is already the 29th in UTC
        Clock clock = Clock.fixed(Instant.parse("2026-09-29T02:00:00Z"), ZoneId.of("America/Toronto"));
        ComicRetrievalJobConfig torontoConfig = new ComicRetrievalJobConfig(managementFacade, clock);

        assertThat(torontoConfig.dateReader().read()).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    @Test
    void dateReader_isStepScoped() throws NoSuchMethodException {
        Method method = ComicRetrievalJobConfig.class.getDeclaredMethod("dateReader");

        assertThat(method.isAnnotationPresent(StepScope.class))
                .as("dateReader must be @StepScope: without it the singleton ListItemReader is exhausted after the first job run, "
                        + "and every subsequent ComicDownloadJob silently completes in 1ms with zero downloads")
                .isTrue();
    }
}
