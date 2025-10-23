package org.stapledon.api.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.service.RetrievalStatusService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/retrieval-status"})
@Tag(name = "Retrieval Status", description = "Endpoints for comic retrieval status records")
public class RetrievalStatusController {
    private final RetrievalStatusService retrievalStatusService;

    @GetMapping
    @Operation(
        summary = "Get retrieval records with optional filtering",
        description = "Returns retrieval records from the last 7 days with optional filters. If date parameters are not provided, all available records are returned."
    )
    public ResponseEntity<ApiResponse<List<ComicRetrievalRecord>>> getRetrievalRecords(
            @Parameter(description = "Filter by comic name")
            @RequestParam(required = false) String comicName,
            
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) ComicRetrievalStatus status,
            
            @Parameter(description = "Filter by from date (inclusive, within the 7-day window)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            
            @Parameter(description = "Filter by to date (inclusive, within the 7-day window)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(required = false, defaultValue = "100") int limit) {
        
        List<ComicRetrievalRecord> records = retrievalStatusService.getRetrievalRecords(
                comicName, status, fromDate, toDate, limit);
        
        return ResponseBuilder.ok(records, "Retrieved status records successfully");
    }

    @GetMapping("/{recordId}")
    @Operation(summary = "Get a specific retrieval record by ID")
    public ResponseEntity<ApiResponse<ComicRetrievalRecord>> getRetrievalRecord(
            @Parameter(description = "Record ID") 
            @PathVariable String recordId) {
        
        Optional<ComicRetrievalRecord> record = retrievalStatusService.getRetrievalRecord(recordId);
        
        if (record.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ComicRetrievalRecord>error(
                            HttpStatus.NOT_FOUND.value(), 
                            "Retrieval record not found with ID: " + recordId));
        }
        
        return ResponseBuilder.ok(record.get(), "Retrieved status record successfully");
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Get summary statistics of retrieval operations",
        description = "Returns aggregated statistics for all retrieval operations. Date parameters are optional and will further filter results within the available 7-day window."
    )
    public ResponseEntity<ApiResponse<Object>> getRetrievalSummary(
            @Parameter(description = "Filter by from date (inclusive, within the 7-day window)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            
            @Parameter(description = "Filter by to date (inclusive, within the 7-day window)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        Object summary = retrievalStatusService.getRetrievalSummary(fromDate, toDate);
        
        return ResponseBuilder.ok(summary, "Retrieved status summary successfully");
    }

    @GetMapping("/comics/{comicName}")
    @Operation(summary = "Get retrieval records for a specific comic")
    public ResponseEntity<ApiResponse<List<ComicRetrievalRecord>>> getRetrievalRecordsForComic(
            @Parameter(description = "Comic name") 
            @PathVariable String comicName,
            
            @Parameter(description = "Maximum number of records to return")
            @RequestParam(required = false, defaultValue = "20") int limit) {
        
        List<ComicRetrievalRecord> records = retrievalStatusService.getRetrievalRecords(
                comicName, null, null, null, limit);
        
        return ResponseBuilder.ok(records, "Retrieved status records for comic successfully");
    }

    @DeleteMapping("/{recordId}")
    @Operation(summary = "Delete a specific retrieval record")
    public ResponseEntity<ApiResponse<String>> deleteRetrievalRecord(
            @Parameter(description = "Record ID") 
            @PathVariable String recordId) {
        
        boolean deleted = retrievalStatusService.deleteRetrievalRecord(recordId);
        
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<String>error(
                            HttpStatus.NOT_FOUND.value(), 
                            "Retrieval record not found with ID: " + recordId));
        }
        
        return ResponseBuilder.ok("Record deleted successfully", "Retrieval record deleted");
    }

    @DeleteMapping
    @Operation(summary = "Purge all retrieval records older than specified days")
    public ResponseEntity<ApiResponse<String>> purgeOldRecords(
            @Parameter(description = "Days to keep (default is 7)")
            @RequestParam(required = false, defaultValue = "7") int daysToKeep) {
        
        int purgedCount = retrievalStatusService.purgeOldRecords(daysToKeep);
        
        return ResponseBuilder.ok(
                "Purged " + purgedCount + " records older than " + daysToKeep + " days",
                "Old retrieval records purged successfully");
    }
}