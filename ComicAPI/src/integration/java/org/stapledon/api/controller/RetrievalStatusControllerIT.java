package org.stapledon.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.core.comic.dto.ComicRetrievalRecord;
import org.stapledon.core.comic.dto.ComicRetrievalStatus;
import org.stapledon.core.comic.service.RetrievalStatusRepository;
import org.stapledon.core.comic.service.RetrievalStatusService;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class RetrievalStatusControllerIT extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private RetrievalStatusService retrievalStatusService;
    
    @Autowired
    private RetrievalStatusRepository retrievalStatusRepository;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void getRetrievalRecordsShouldReturnAvailableRecords() throws Exception {
        // Reset repository state for clean test
        retrievalStatusRepository.resetRecords();
        
        // Arrange - Create test records
        ComicRetrievalRecord successRecord = ComicRetrievalRecord.success(
                "TestComic", 
                LocalDate.now(), 
                "gocomics", 
                500, 
                20000L
        );
        
        ComicRetrievalRecord failureRecord = ComicRetrievalRecord.failure(
                "TestComic", 
                LocalDate.now(), 
                "gocomics", 
                ComicRetrievalStatus.NETWORK_ERROR, 
                "Connection timeout", 
                2000, 
                null
        );
        
        retrievalStatusService.recordRetrievalResult(successRecord);
        retrievalStatusService.recordRetrievalResult(failureRecord);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/retrieval-status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[*].comicName", hasItem("TestComic")))
                .andExpect(jsonPath("$.message", containsString("Retrieved status records")));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void getSummaryShouldReturnAggregatedStatistics() throws Exception {
        // Reset repository state for clean test
        retrievalStatusRepository.resetRecords();
        
        // Ensure we have at least one record
        ComicRetrievalRecord record = ComicRetrievalRecord.success(
                "TestComic", 
                LocalDate.now(), 
                "gocomics", 
                500, 
                20000L
        );
        retrievalStatusService.recordRetrievalResult(record);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/retrieval-status/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.countsByStatus", notNullValue()))
                .andExpect(jsonPath("$.data.successRate", notNullValue()));
    }
}