package org.stapledon.core.comic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.repository.RetrievalStatusRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrievalStatusServiceImplTest {
    @Mock
    private RetrievalStatusRepository repository;
    
    @InjectMocks
    private RetrievalStatusServiceImpl service;
    
    private ComicRetrievalRecord successRecord;
    private ComicRetrievalRecord failureRecord;
    
    @BeforeEach
    void setUp() {
        // Set up test data
        successRecord = ComicRetrievalRecord.success(
                "TestComic", 
                LocalDate.now(), 
                "gocomics", 
                500, 
                20000L
        );
        
        failureRecord = ComicRetrievalRecord.failure(
                "TestComic", 
                LocalDate.now(), 
                "gocomics", 
                ComicRetrievalStatus.NETWORK_ERROR, 
                "Connection timeout", 
                2000, 
                null
        );
    }
    
    @Test
    void recordRetrievalResultShouldSaveRecord() {
        // Act
        service.recordRetrievalResult(successRecord);
        
        // Assert
        verify(repository).saveRecord(successRecord);
    }
    
    @Test
    void getRetrievalRecordShouldReturnRecordFromRepository() {
        // Arrange
        String recordId = "test-id";
        when(repository.getRecord(recordId)).thenReturn(Optional.of(successRecord));
        
        // Act
        Optional<ComicRetrievalRecord> result = service.getRetrievalRecord(recordId);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(successRecord, result.get());
        verify(repository).getRecord(recordId);
    }
    
    @Test
    void getRetrievalSummaryShouldCalculateCorrectStatistics() {
        // Arrange
        List<ComicRetrievalRecord> mockRecords = Arrays.asList(successRecord, failureRecord);
        when(repository.getRecords(
                isNull(), 
                isNull(), 
                any(LocalDate.class), 
                any(LocalDate.class), 
                eq(Integer.MAX_VALUE)
        )).thenReturn(mockRecords);
        
        // Act
        Map<String, Object> summary = service.getRetrievalSummary(
                LocalDate.now().minusDays(1), 
                LocalDate.now()
        );
        
        // Assert
        assertNotNull(summary);
        assertEquals(2, summary.get("totalCount"));
        assertEquals(0.5, summary.get("successRate"));
        
        @SuppressWarnings("unchecked")
        Map<ComicRetrievalStatus, Long> countsByStatus = 
                (Map<ComicRetrievalStatus, Long>) summary.get("countsByStatus");
                
        assertEquals(1L, countsByStatus.get(ComicRetrievalStatus.SUCCESS));
        assertEquals(1L, countsByStatus.get(ComicRetrievalStatus.NETWORK_ERROR));
    }
}