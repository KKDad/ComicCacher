package org.stapledon.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("integration")
class JsonRetrievalStatusRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private RetrievalStatusRepository repository;

    @Autowired
    private CacheProperties cacheProperties;
    
    private File storageFile;
    private ComicRetrievalRecord testRecord;
    
    @BeforeEach
    void setUp() {
        storageFile = new File(cacheProperties.getLocation(), "retrieval-status.json");
        
        // Reset repository state for each test
        repository.resetRecords();
        
        // Create test record
        testRecord = ComicRetrievalRecord.success(
                "TestComic", 
                LocalDate.now(), 
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
        assertTrue(storageFile.exists());
        
        // Retrieve and verify
        Optional<ComicRetrievalRecord> retrieved = repository.getRecord(testRecord.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(testRecord.getComicName(), retrieved.get().getComicName());
        assertEquals(testRecord.getStatus(), retrieved.get().getStatus());
    }
    
    @Test
    void getRecordsWithFilteringShouldWork() {
        // Arrange - Create several records
        ComicRetrievalRecord record1 = ComicRetrievalRecord.success(
                "Comic1", 
                LocalDate.now(), 
                "gocomics", 
                500, 
                20000L
        );
        
        ComicRetrievalRecord record2 = ComicRetrievalRecord.failure(
                "Comic1", 
                LocalDate.now().minusDays(1), 
                "gocomics", 
                ComicRetrievalStatus.NETWORK_ERROR, 
                "Error", 
                200, 
                null
        );
        
        ComicRetrievalRecord record3 = ComicRetrievalRecord.success(
                "Comic2", 
                LocalDate.now(), 
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
        assertEquals(2, comicNameResults.size());
        
        // Filter by status
        List<ComicRetrievalRecord> statusResults = repository.getRecords(
                null, ComicRetrievalStatus.SUCCESS, null, null, 10);
        assertEquals(2, statusResults.size());
        
        // Filter by date
        List<ComicRetrievalRecord> dateResults = repository.getRecords(
                null, null, LocalDate.now(), LocalDate.now(), 10);
        assertEquals(2, dateResults.size());
        
        // Combined filters
        List<ComicRetrievalRecord> combinedResults = repository.getRecords(
                "Comic1", ComicRetrievalStatus.SUCCESS, LocalDate.now(), LocalDate.now(), 10);
        assertEquals(1, combinedResults.size());
    }
    
    @Test
    void purgeOldRecordsShouldRemoveExpiredRecords() {
        // Arrange - Create both recent and old records
        // This one should be kept
        ComicRetrievalRecord recentRecord = ComicRetrievalRecord.success(
                "Comic1", 
                LocalDate.now(), 
                "gocomics", 
                500, 
                20000L
        );
        
        // This one should be removed by the 1-day purge
        ComicRetrievalRecord oldRecord = ComicRetrievalRecord.builder()
                .id("Comic2_" + LocalDate.now().minusDays(5))
                .comicName("Comic2")
                .comicDate(LocalDate.now().minusDays(5))
                .source("gocomics")
                .status(ComicRetrievalStatus.SUCCESS)
                .retrievalDurationMs(300)
                .imageSize(30000L)
                .build();
        
        repository.saveRecord(recentRecord);
        repository.saveRecord(oldRecord);
        
        // Act
        int purgedCount = repository.purgeOldRecords(1);
        
        // Assert
        assertEquals(1, purgedCount);
        
        List<ComicRetrievalRecord> remainingRecords = repository.getRecords(
                null, null, null, null, 10);
        assertEquals(1, remainingRecords.size());
        assertEquals(recentRecord.getId(), remainingRecords.get(0).getId());
    }
}