package org.stapledon.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.repository.RetrievalStatusRepository;

import java.io.File;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("integration")
class JsonRetrievalStatusRepositoryIT extends AbstractIntegrationTest {

    @Autowired private RetrievalStatusRepository repository;

    @Autowired private CacheProperties cacheProperties;

    @Autowired private Clock clock;

    private File storageFile;
    private ComicRetrievalRecord testRecord;

    @BeforeEach
    void setUp() {
        storageFile = new File(cacheProperties.getLocation(), "retrieval-status.json");

        // Reset repository state for each test
        repository.resetRecords();

        // Create test record
        testRecord = ComicRetrievalRecord.success(
                1, "TestComic",
                LocalDate.now(clock),
                "gocomics",
                500,
                20000L
        );
    }

    @AfterEach
    void tearDown() {
        // Clean up the test file
        if (storageFile.exists()) {
            storageFile.delete();
        }
    }

    @Test
    void saveAndRetrieveRecordShouldWork() {
        // Act
        repository.saveRecord(testRecord);

        // Assert file was created
        assertThat(storageFile.exists()).isTrue();

        // Retrieve and verify
        Optional<ComicRetrievalRecord> retrieved = repository.getRecord(testRecord.getId());
        assertThat(retrieved.isPresent()).isTrue();
        assertThat(retrieved.get().getComicName()).isEqualTo(testRecord.getComicName());
        assertThat(retrieved.get().getStatus()).isEqualTo(testRecord.getStatus());
    }

    @Test
    void getRecordsWithFilteringShouldWork() {
        // Arrange - Create several records
        ComicRetrievalRecord record1 = ComicRetrievalRecord.success(
                1, "Comic1",
                LocalDate.now(clock),
                "gocomics",
                500,
                20000L
        );

        ComicRetrievalRecord record2 = ComicRetrievalRecord.failure(
                1, "Comic1",
                LocalDate.now(clock).minusDays(1),
                "gocomics",
                ComicRetrievalStatus.NETWORK_ERROR,
                "Error",
                200,
                null
        );

        ComicRetrievalRecord record3 = ComicRetrievalRecord.success(
                2, "Comic2",
                LocalDate.now(clock),
                "gocomics",
                300,
                30000L
        );

        repository.saveRecord(record1);
        repository.saveRecord(record2);
        repository.saveRecord(record3);

        // Act & Assert - Filter by comic name
        List<ComicRetrievalRecord> comicNameResults = repository.getRecords(
                "Comic1", null, null, null, 10);
        assertThat(comicNameResults.size()).isEqualTo(2);

        // Filter by status
        List<ComicRetrievalRecord> statusResults = repository.getRecords(
                null, ComicRetrievalStatus.SUCCESS, null, null, 10);
        assertThat(statusResults.size()).isEqualTo(2);

        // Filter by date
        List<ComicRetrievalRecord> dateResults = repository.getRecords(
                null, null, LocalDate.now(clock), LocalDate.now(clock), 10);
        assertThat(dateResults.size()).isEqualTo(2);

        // Combined filters
        List<ComicRetrievalRecord> combinedResults = repository.getRecords(
                "Comic1", ComicRetrievalStatus.SUCCESS, LocalDate.now(clock), LocalDate.now(clock), 10);
        assertThat(combinedResults.size()).isEqualTo(1);
    }

    @Test
    void purgeOldRecordsShouldRemoveExpiredRecords() {
        // Arrange - Create both recent and old records
        // This one should be kept
        ComicRetrievalRecord recentRecord = ComicRetrievalRecord.success(
                1, "Comic1",
                LocalDate.now(clock),
                "gocomics",
                500,
                20000L
        );

        // This one should be removed by the 1-day purge
        ComicRetrievalRecord oldRecord = ComicRetrievalRecord.builder()
                .id("Comic2_" + LocalDate.now(clock).minusDays(5))
                .comicName("Comic2")
                .comicDate(LocalDate.now(clock).minusDays(5))
                .source("gocomics")
                .status(ComicRetrievalStatus.SUCCESS)
                .retrievalDurationMs(300)
                .imageSize(30000L)
                .attemptedAt(OffsetDateTime.now(clock).minusDays(5))
                .build();

        repository.saveRecord(recentRecord);
        repository.saveRecord(oldRecord);

        // Act
        int purgedCount = repository.purgeOldRecords(1);

        // Assert
        assertThat(purgedCount).isEqualTo(1);

        List<ComicRetrievalRecord> remainingRecords = repository.getRecords(
                null, null, null, null, 10);
        assertThat(remainingRecords.size()).isEqualTo(1);
        assertThat(remainingRecords.get(0).getId()).isEqualTo(recentRecord.getId());
    }

    @Test
    void saveStampsTheAttemptTimeInUtc() {
        repository.saveRecord(testRecord);

        ComicRetrievalRecord saved = repository.getRecord(testRecord.getId()).orElseThrow();
        assertThat(saved.getAttemptedAt()).isNotNull();
        assertThat(saved.getAttemptedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(saved.getId()).isEqualTo("1_" + LocalDate.now(clock));
        assertThat(saved.getComicId()).isEqualTo(1);
    }

    @Test
    void purgeKeepsARecentAttemptForAnOldStripAndDropsAnOldAttempt() {
        // A backfill today of a strip from last year
        repository.saveRecord(ComicRetrievalRecord.success(1, "Comic1", LocalDate.now(clock).minusDays(400), "gocomics", 500, 20000L));
        // Today's strip, attempted 40 days ago (an impossible record, but it shows the purge goes by attempt time)
        repository.saveRecord(ComicRetrievalRecord.success(2, "Comic2", LocalDate.now(clock), "gocomics", 500, 20000L)
                .toBuilder().attemptedAt(OffsetDateTime.now(clock).minusDays(40)).build());

        int purged = repository.purgeOldRecords(30);

        assertThat(purged).isEqualTo(1);
        assertThat(repository.getRecords(null, null, null, null, 10)).extracting(ComicRetrievalRecord::getComicName).containsExactly("Comic1");
    }

    @Test
    void aNewAttemptReplacesARecordKeyedByName() {
        LocalDate date = LocalDate.now(clock);
        repository.saveRecord(ComicRetrievalRecord.builder()
                .id("TestComic_" + date)
                .comicName("TestComic")
                .comicDate(date)
                .source("gocomics")
                .status(ComicRetrievalStatus.COMIC_UNAVAILABLE)
                .build());

        repository.saveRecord(testRecord);

        List<ComicRetrievalRecord> records = repository.getRecords(null, null, null, null, 10);
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getId()).isEqualTo("1_" + date);
        assertThat(records.getFirst().getStatus()).isEqualTo(ComicRetrievalStatus.SUCCESS);
    }
}
