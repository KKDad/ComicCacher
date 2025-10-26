package org.stapledon.engine.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.PlatformTransactionManager;
import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.batch.ComicBackfillService.BackfillTask;
import org.stapledon.engine.management.ManagementFacade;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private ComicBackfillJobConfig config;

    @BeforeEach
    void setUp() {
        config = new ComicBackfillJobConfig(managementFacade, backfillService);
        setField(config, "chunkSize", 10);
        setField(config, "delayBetweenComics", 0L); // No delay for tests
    }

    @Test
    void comicBackfillJob_shouldBeCreated() {
        Step mockStep = mock(Step.class);

        Job job = config.comicBackfillJob(jobRepository, mockStep, jsonBatchExecutionTracker);

        assertNotNull(job);
        assertEquals("ComicBackfillJob", job.getName());
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
            mockWriter
        );

        assertNotNull(step);
        assertEquals("comicBackfillStep", step.getName());
    }

    @Test
    void backfillTaskReader_shouldReturnTasks() throws Exception {
        ComicItem comic = createComic(1, "Test Comic");
        BackfillTask task = new BackfillTask(comic, LocalDate.of(2025, 1, 1));

        when(backfillService.findMissingStrips()).thenReturn(List.of(task));

        ItemReader<BackfillTask> reader = config.backfillTaskReader();

        BackfillTask result = reader.read();
        assertNotNull(result);
        assertEquals(comic, result.comic());
        assertEquals(LocalDate.of(2025, 1, 1), result.date());

        // Second read should return null (end of list)
        assertNull(reader.read());
    }

    @Test
    void backfillTaskProcessor_shouldProcessTask() throws Exception {
        ComicItem comic = createComic(1, "Test Comic");
        BackfillTask task = new BackfillTask(comic, LocalDate.of(2025, 1, 1));

        ComicDownloadRequest request = ComicDownloadRequest.builder()
            .comicId(comic.getId())
            .comicName(comic.getName())
            .date(task.date())
            .build();

        ComicDownloadResult result = ComicDownloadResult.success(request, new byte[0]);

        when(managementFacade.updateComicsForDate(any(LocalDate.class)))
            .thenReturn(List.of(result));

        ItemProcessor<BackfillTask, ComicDownloadResult> processor = config.backfillTaskProcessor();

        ComicDownloadResult processedResult = processor.process(task);

        assertNotNull(processedResult);
        assertTrue(processedResult.isSuccessful());
        verify(managementFacade).updateComicsForDate(task.date());
    }

    @Test
    void backfillTaskProcessor_handlesNoMatchingResult() throws Exception {
        ComicItem comic = createComic(1, "Test Comic");
        BackfillTask task = new BackfillTask(comic, LocalDate.of(2025, 1, 1));

        // Return result for a different comic
        ComicDownloadRequest otherRequest = ComicDownloadRequest.builder()
            .comicId(2)
            .comicName("Other Comic")
            .date(task.date())
            .build();

        ComicDownloadResult otherResult = ComicDownloadResult.success(otherRequest, new byte[0]);

        when(managementFacade.updateComicsForDate(any(LocalDate.class)))
            .thenReturn(List.of(otherResult));

        ItemProcessor<BackfillTask, ComicDownloadResult> processor = config.backfillTaskProcessor();

        ComicDownloadResult result = processor.process(task);

        assertNull(result); // No matching result for this comic
    }

    @Test
    void backfillTaskProcessor_handlesException() throws Exception {
        ComicItem comic = createComic(1, "Test Comic");
        BackfillTask task = new BackfillTask(comic, LocalDate.of(2025, 1, 1));

        when(managementFacade.updateComicsForDate(any(LocalDate.class)))
            .thenThrow(new RuntimeException("Test exception"));

        ItemProcessor<BackfillTask, ComicDownloadResult> processor = config.backfillTaskProcessor();

        ComicDownloadResult result = processor.process(task);

        assertNull(result); // Should return null on exception
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
        assertDoesNotThrow(() -> writer.write(org.springframework.batch.item.Chunk.of(success, failure)));
    }

    @Test
    void backfillTaskWriter_handlesNullResults() throws Exception {
        ItemWriter<ComicDownloadResult> writer = config.backfillTaskWriter();

        // Should handle nulls gracefully
        assertDoesNotThrow(() -> writer.write(org.springframework.batch.item.Chunk.of((ComicDownloadResult) null)));
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
