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
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.repository.RetrievalStatusRepository;
import org.stapledon.common.service.RetrievalStatusService;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class RetrievalStatusMultiDayIT extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private RetrievalStatusService retrievalStatusService;
    
    @Autowired
    private RetrievalStatusRepository retrievalStatusRepository;
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldStoreAndRetrieveMultipleDaysOfRecords() throws Exception {
        // Reset repository state for clean test
        retrievalStatusRepository.resetRecords();
        
        String comicName = "TestComic";
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDate threeDaysAgo = today.minusDays(3);
        
        // Create records for multiple days for the same comic
        ComicRetrievalRecord todayRecord = ComicRetrievalRecord.success(
                comicName, today, "gocomics", 500, 20000L);
        
        ComicRetrievalRecord yesterdayRecord = ComicRetrievalRecord.success(
                comicName, yesterday, "gocomics", 600, 21000L);
        
        ComicRetrievalRecord twoDaysAgoRecord = ComicRetrievalRecord.failure(
                comicName, twoDaysAgo, "gocomics", 
                ComicRetrievalStatus.NETWORK_ERROR, "Connection timeout", 2000, 500);
        
        ComicRetrievalRecord threeDaysAgoRecord = ComicRetrievalRecord.success(
                comicName, threeDaysAgo, "gocomics", 450, 19500L);
        
        // Store all records
        retrievalStatusService.recordRetrievalResult(todayRecord);
        retrievalStatusService.recordRetrievalResult(yesterdayRecord);
        retrievalStatusService.recordRetrievalResult(twoDaysAgoRecord);
        retrievalStatusService.recordRetrievalResult(threeDaysAgoRecord);
        
        // Verify records are stored correctly via repository
        List<ComicRetrievalRecord> allRecords = retrievalStatusRepository.getRecords(
                null, null, null, null, 100);
        
        System.out.println("=== Repository Records ===");
        allRecords.forEach(record -> 
            System.out.println("ID: " + record.getId() + 
                             ", Comic: " + record.getComicName() + 
                             ", Date: " + record.getComicDate() + 
                             ", Status: " + record.getStatus()));
        
        assertEquals(4, allRecords.size(), "Should have 4 records stored");
        
        // Verify records for specific comic
        List<ComicRetrievalRecord> comicRecords = retrievalStatusRepository.getRecords(
                comicName, null, null, null, 100);
        
        System.out.println("=== Comic-Specific Records ===");
        comicRecords.forEach(record -> 
            System.out.println("ID: " + record.getId() + 
                             ", Comic: " + record.getComicName() + 
                             ", Date: " + record.getComicDate() + 
                             ", Status: " + record.getStatus()));
        
        assertEquals(4, comicRecords.size(), "Should have 4 records for the comic");
        
        // Test via API - Get all records
        mockMvc.perform(get("/api/v1/retrieval-status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[*].comicName", everyItem(equalTo(comicName))))
                .andExpect(jsonPath("$.message", containsString("Retrieved status records")));
        
        // Test via API - Get records for specific comic
        mockMvc.perform(get("/api/v1/retrieval-status/comics/" + comicName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[*].comicName", everyItem(equalTo(comicName))))
                .andExpect(jsonPath("$.message", containsString("Retrieved status records for comic")));
        
        // Test date filtering
        mockMvc.perform(get("/api/v1/retrieval-status")
                .param("fromDate", yesterday.toString())
                .param("toDate", today.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].comicDate", 
                    containsInAnyOrder(today.toString(), yesterday.toString())));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldStoreMultipleComicsForSameDay() throws Exception {
        // Reset repository state for clean test
        retrievalStatusRepository.resetRecords();
        
        LocalDate testDate = LocalDate.now();
        
        // Create records for multiple comics on the same day
        ComicRetrievalRecord dilbertRecord = ComicRetrievalRecord.success(
                "Dilbert", testDate, "gocomics", 500, 20000L);
        
        ComicRetrievalRecord garfieldRecord = ComicRetrievalRecord.success(
                "Garfield", testDate, "gocomics", 600, 25000L);
        
        ComicRetrievalRecord calvinRecord = ComicRetrievalRecord.failure(
                "CalvinAndHobbes", testDate, "gocomics", 
                ComicRetrievalStatus.COMIC_UNAVAILABLE, "Comic not available", 1000, 404);
        
        // Store all records
        retrievalStatusService.recordRetrievalResult(dilbertRecord);
        retrievalStatusService.recordRetrievalResult(garfieldRecord);
        retrievalStatusService.recordRetrievalResult(calvinRecord);
        
        // Verify via repository
        List<ComicRetrievalRecord> dayRecords = retrievalStatusRepository.getRecords(
                null, null, testDate, testDate, 100);
        
        System.out.println("=== Same Day Multiple Comics ===");
        dayRecords.forEach(record -> 
            System.out.println("ID: " + record.getId() + 
                             ", Comic: " + record.getComicName() + 
                             ", Date: " + record.getComicDate() + 
                             ", Status: " + record.getStatus()));
        
        assertEquals(3, dayRecords.size(), "Should have 3 records for the same day");
        
        // Test via API
        mockMvc.perform(get("/api/v1/retrieval-status")
                .param("fromDate", testDate.toString())
                .param("toDate", testDate.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].comicName", 
                    containsInAnyOrder("Dilbert", "Garfield", "CalvinAndHobbes")));
    }
}