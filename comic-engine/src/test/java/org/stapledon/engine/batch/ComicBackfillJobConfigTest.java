package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicDownloadResult.FailureKind;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.SaveResult;
import org.stapledon.engine.batch.ComicBackfillService.BackfillTask;
import org.stapledon.engine.batch.ComicBackfillService.DateBackfillTask;
import org.stapledon.engine.batch.ComicBackfillService.StripBackfillTask;
import org.stapledon.engine.batch.config.ComicBackfillJobConfig;
import org.stapledon.engine.management.ManagementFacade;

@ExtendWith(MockitoExtension.class)
class ComicBackfillJobConfigTest {

    @Mock
    private ManagementFacade managementFacade;

    @Mock
    private ComicBackfillService backfillService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private JsonBatchExecutionTracker jsonBatchExecutionTracker;

    @Mock
    private BackfillStateService backfillState;

    private ComicBackfillJobConfig config;

    @BeforeEach
    void setUp() {
        config = new ComicBackfillJobConfig(managementFacade, backfillService, backfillState);
        setField(config, "chunkSize", 10);
        setField(config, "delayBetweenComics", 0L); // No delay for tests
    }

    @Test
    void comicBackfillJob_shouldBeCreated() {
        Step mockStep = mock(Step.class);

        Job job = config.comicBackfillJob(jobRepository, mockStep, jsonBatchExecutionTracker);

        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("ComicBackfillJob");
    }

    @Test
    void comicBackfillStep_shouldBeCreated() {
        ItemReader<BackfillTask> mockReader = mock(ItemReader.class);
        ItemProcessor<BackfillTask, ComicDownloadResult> mockProcessor = mock(ItemProcessor.class);
        ItemWriter<ComicDownloadResult> mockWriter = mock(ItemWriter.class);

        Step step = config.comicBackfillStep(
                jobRepository,
                transactionManager,
                mockReader,
                mockProcessor,
                mockWriter);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("comicBackfillStep");
    }

    @Test
    void backfillTaskReader_shouldReturnTasks() throws Exception {
        ComicItem comic = createComic(1, "Test Comic");
        BackfillTask task = new DateBackfillTask(comic, LocalDate.of(2025, 1, 1));

        when(backfillService.findMissingStrips(null)).thenReturn(List.of(task));

        ItemReader<BackfillTask> reader = config.backfillTaskReader(null, null);

        BackfillTask result = reader.read();
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(DateBackfillTask.class);
        assertThat(result.comic()).isEqualTo(comic);
        assertThat(((DateBackfillTask) result).date()).isEqualTo(LocalDate.of(2025, 1, 1));

        // Second read should return null (end of list)
        assertThat(reader.read()).isNull();
    }

    @Test
    void backfillTaskProcessor_shouldProcessTask() throws Exception {
        ComicItem comic = createComic(1, "Test Comic");
        DateBackfillTask task = new DateBackfillTask(comic, LocalDate.of(2025, 1, 1));

        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(comic.getId())
                .comicName(comic.getName())
                .date(task.date())
                .build();

        ComicDownloadResult result = ComicDownloadResult.success(request, new byte[0]);

        when(managementFacade.downloadComicForDate(any(ComicItem.class), any(LocalDate.class), anyBoolean()))
                .thenReturn(Optional.of(result));

        ItemProcessor<BackfillTask, ComicDownloadResult> processor = config.backfillTaskProcessor();

        ComicDownloadResult processedResult = processor.process(task);

        assertThat(processedResult).isNotNull();
        assertThat(processedResult.isSuccessful()).isTrue();
        verify(managementFacade).downloadComicForDate(comic, task.date(), true);
        verify(backfillState).recordSuccess(comic, task.date());
    }

    @Test
    void backfillTaskProcessor_handlesEmptyResult() throws Exception {
        ComicItem comic = createComic(1, "Test Comic");
        BackfillTask task = new DateBackfillTask(comic, LocalDate.of(2025, 1, 1));

        // Return empty (comic already cached or couldn't be downloaded)
        when(managementFacade.downloadComicForDate(any(ComicItem.class), any(LocalDate.class), anyBoolean()))
                .thenReturn(Optional.empty());

        ItemProcessor<BackfillTask, ComicDownloadResult> processor = config.backfillTaskProcessor();

        ComicDownloadResult result = processor.process(task);

        assertThat(result).isNull(); // Empty Optional returns null
    }

    @Test
    void backfillTaskProcessor_handlesException() throws Exception {
        ComicItem comic = createComic(1, "Test Comic");
        BackfillTask task = new DateBackfillTask(comic, LocalDate.of(2025, 1, 1));

        when(managementFacade.downloadComicForDate(any(ComicItem.class), any(LocalDate.class), anyBoolean()))
                .thenThrow(new RuntimeException("Test exception"));

        ItemProcessor<BackfillTask, ComicDownloadResult> processor = config.backfillTaskProcessor();

        ComicDownloadResult result = processor.process(task);

        // A failed result, not null: null would be counted as filtered and the error would vanish from the step counts
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureKind()).isEqualTo(FailureKind.ERROR);
        assertThat(result.getErrorMessage()).contains("Test exception");
        assertThat(result.getRequest().getDate()).isEqualTo(LocalDate.of(2025, 1, 1));
    }

    @Test
    void backfillTaskWriter_shouldWriteResults() throws Exception {
        ComicDownloadRequest request1 = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Comic 1")
                .date(LocalDate.of(2025, 1, 1))
                .build();

        ComicDownloadRequest request2 = ComicDownloadRequest.builder()
                .comicId(2)
                .comicName("Comic 2")
                .date(LocalDate.of(2025, 1, 2))
                .build();

        ComicDownloadResult success = ComicDownloadResult.success(request1, new byte[0]);
        ComicDownloadResult failure = ComicDownloadResult.failure(request2, "Test error");

        ItemWriter<ComicDownloadResult> writer = config.backfillTaskWriter();

        // Should not throw exception
        Assertions.assertThatCode(() -> writer.write(Chunk.of(success, failure))).doesNotThrowAnyException();
    }

    @Test
    void backfillTaskWriter_flushesBackfillStateAfterEachChunk() throws Exception {
        config.backfillTaskWriter().write(Chunk.of());

        verify(backfillState).flush();
    }

    @Test
    void backfillTaskProcessor_stripRateLimitStopsThatSourceForTheRun() throws Exception {
        ComicItem freefall = createComic(1, "Freefall");
        freefall.setSource("freefall");
        ComicDownloadResult rateLimited = ComicDownloadResult.failure(request(freefall, LocalDate.of(2025, 1, 1)), "429", FailureKind.RATE_LIMITED);
        when(managementFacade.downloadComicByStripNumber(freefall, 100)).thenReturn(Optional.of(rateLimited));

        ItemProcessor<BackfillTask, ComicDownloadResult> processor = config.backfillTaskProcessor();

        assertThat(processor.process(new StripBackfillTask(freefall, 100)).isRateLimited()).isTrue();
        assertThat(processor.process(new StripBackfillTask(freefall, 99))).isNull();
        verify(managementFacade, never()).downloadComicByStripNumber(freefall, 99);
    }

    @Test
    void backfillTaskProcessor_stripExceptionIsCountedAsFailure() throws Exception {
        ComicItem comic = createComic(1, "Freefall");
        when(managementFacade.downloadComicByStripNumber(comic, 42)).thenThrow(new IllegalStateException("boom"));

        ComicDownloadResult result = config.backfillTaskProcessor().process(new StripBackfillTask(comic, 42));

        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureKind()).isEqualTo(FailureKind.ERROR);
        assertThat(result.getErrorMessage()).contains("boom");
    }

    @Test
    void backfillTaskReader_resetStateClearsLearnedState() throws Exception {
        when(backfillService.findMissingStrips(null)).thenReturn(List.of());

        config.backfillTaskReader(null, "true");

        verify(backfillState).reset();
    }

    @Test
    void backfillTaskProcessor_rateLimitStopsThatSourceForTheRun() throws Exception {
        ComicItem first = createComic(1, "First");
        ComicItem second = createComic(2, "Second");
        ComicItem other = createComic(3, "Other");
        other.setSource("other-source");
        LocalDate date = LocalDate.of(2025, 1, 1);

        when(managementFacade.downloadComicForDate(eq(first), any(LocalDate.class), anyBoolean()))
                .thenReturn(Optional.of(ComicDownloadResult.failure(request(first, date), "429", FailureKind.RATE_LIMITED)));
        when(managementFacade.downloadComicForDate(eq(other), any(LocalDate.class), anyBoolean()))
                .thenReturn(Optional.of(ComicDownloadResult.success(request(other, date), new byte[0])));

        ItemProcessor<BackfillTask, ComicDownloadResult> processor = config.backfillTaskProcessor();

        assertThat(processor.process(new DateBackfillTask(first, date)).isRateLimited()).isTrue();
        assertThat(processor.process(new DateBackfillTask(second, date))).isNull();
        assertThat(processor.process(new DateBackfillTask(other, date))).isNotNull();

        verify(managementFacade, never()).downloadComicForDate(eq(second), any(LocalDate.class), anyBoolean());
        verify(backfillState, never()).recordUnavailable(any(), any(), any());
    }

    @Test
    void backfillTaskProcessor_newRunStartsWithNoStoppedSources() throws Exception {
        ComicItem comic = createComic(1, "Comic");
        LocalDate date = LocalDate.of(2025, 1, 1);
        when(managementFacade.downloadComicForDate(eq(comic), any(LocalDate.class), anyBoolean()))
                .thenReturn(Optional.of(ComicDownloadResult.failure(request(comic, date), "429", FailureKind.RATE_LIMITED)));

        config.backfillTaskProcessor().process(new DateBackfillTask(comic, date));
        config.backfillTaskProcessor().process(new DateBackfillTask(comic, date));

        verify(managementFacade, times(2)).downloadComicForDate(eq(comic), any(LocalDate.class), anyBoolean());
    }

    @Test
    void backfillTaskProcessor_recordsUnavailableResults() throws Exception {
        ComicItem comic = createComic(1, "Comic");
        LocalDate date = LocalDate.of(2025, 1, 1);
        when(managementFacade.downloadComicForDate(any(ComicItem.class), any(LocalDate.class), anyBoolean()))
                .thenReturn(Optional.of(ComicDownloadResult.failure(request(comic, date), "empty", FailureKind.UNAVAILABLE)));

        config.backfillTaskProcessor().process(new DateBackfillTask(comic, date));

        verify(backfillState).recordUnavailable(comic, date, BackfillStateService.OUTCOME_UNAVAILABLE);
        verify(backfillState).recordAttempt("test-source");
    }

    @Test
    void backfillTaskProcessor_recordsDuplicatesAsUnavailable() throws Exception {
        ComicItem comic = createComic(1, "Comic");
        LocalDate date = LocalDate.of(2025, 1, 1);
        ComicDownloadResult duplicate = ComicDownloadResult.success(request(comic, date), new byte[0]).toBuilder()
                .saveOutcome(SaveResult.Outcome.DUPLICATE_SKIPPED).build();
        when(managementFacade.downloadComicForDate(any(ComicItem.class), any(LocalDate.class), anyBoolean())).thenReturn(Optional.of(duplicate));

        config.backfillTaskProcessor().process(new DateBackfillTask(comic, date));

        verify(backfillState).recordUnavailable(comic, date, BackfillStateService.OUTCOME_DUPLICATE);
        verify(backfillState, never()).recordSuccess(any(), any());
    }

    @Test
    void backfillTaskProcessor_ignoresTransientErrors() throws Exception {
        ComicItem comic = createComic(1, "Comic");
        LocalDate date = LocalDate.of(2025, 1, 1);
        when(managementFacade.downloadComicForDate(any(ComicItem.class), any(LocalDate.class), anyBoolean()))
                .thenReturn(Optional.of(ComicDownloadResult.failure(request(comic, date), "timeout", FailureKind.ERROR)));

        config.backfillTaskProcessor().process(new DateBackfillTask(comic, date));

        verify(backfillState, never()).recordUnavailable(any(), any(), any());
        verify(backfillState, never()).recordSuccess(any(), any());
    }

    private static ComicDownloadRequest request(ComicItem comic, LocalDate date) {
        return ComicDownloadRequest.builder().comicId(comic.getId()).comicName(comic.getName()).source(comic.getSource()).date(date).build();
    }

    private ComicItem createComic(int id, String name) {
        ComicItem comic = new ComicItem();
        comic.setId(id);
        comic.setName(name);
        comic.setSource("test-source");
        comic.setSourceIdentifier("test-identifier");
        return comic;
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
