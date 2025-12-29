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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonRetrievalStatusServiceTest {
    @Mock
    private RetrievalStatusRepository repository;
    
    @InjectMocks
    private JsonRetrievalStatusService service;
    
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
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(successRecord);
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
        assertThat(summary).isNotNull();
        assertThat(summary.get("totalCount")).isEqualTo(2);
        assertThat(summary.get("successRate")).isEqualTo(0.5);
        
        @SuppressWarnings("unchecked")
        Map<ComicRetrievalStatus, Long> countsByStatus = 
                (Map<ComicRetrievalStatus, Long>) summary.get("countsByStatus");

        assertThat(countsByStatus.get(ComicRetrievalStatus.SUCCESS)).isEqualTo(1L);
        assertThat(countsByStatus.get(ComicRetrievalStatus.NETWORK_ERROR)).isEqualTo(1L);
    }
}