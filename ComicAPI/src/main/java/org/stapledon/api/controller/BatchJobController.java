package org.stapledon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.infrastructure.batch.ComicBatchService;
import org.stapledon.infrastructure.batch.ComicJobSummary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * Controller for monitoring and managing comic retrieval batch jobs.
 * Provides comprehensive visibility into job execution history and performance.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/batch")
@Tag(name = "Batch Jobs", description = "Endpoints for monitoring comic retrieval batch jobs")
public class BatchJobController {

    private final ComicBatchService comicBatchService;

    @GetMapping("/jobs/recent")
    @Operation(summary = "Get recent job executions", 
               description = "Returns the most recent batch job executions with detailed status information")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentJobs(
            @Parameter(description = "Number of recent jobs to return")
            @RequestParam(defaultValue = "10") int count) {
        
        List<JobExecution> executions = comicBatchService.getRecentJobExecutions(count);
        List<Map<String, Object>> jobDetails = executions.stream()
                .map(this::convertJobExecutionToMap)
                .toList();
        
        return ResponseBuilder.ok(jobDetails, "Retrieved recent job executions");
    }

    @GetMapping("/jobs/date-range")
    @Operation(summary = "Get job executions for date range",
               description = "Returns batch job executions within the specified date range")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getJobsByDateRange(
            @Parameter(description = "Start date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date (inclusive)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<JobExecution> executions = comicBatchService.getJobExecutionsForDateRange(startDate, endDate);
        List<Map<String, Object>> jobDetails = executions.stream()
                .map(this::convertJobExecutionToMap)
                .toList();
        
        return ResponseBuilder.ok(jobDetails, "Retrieved job executions for date range");
    }

    @GetMapping("/jobs/{executionId}")
    @Operation(summary = "Get specific job execution details",
               description = "Returns detailed information about a specific job execution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobExecution(
            @Parameter(description = "Job execution ID")
            @PathVariable Long executionId) {
        
        JobExecution execution = comicBatchService.getJobExecution(executionId);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> jobDetails = convertJobExecutionToDetailedMap(execution);
        return ResponseBuilder.ok(jobDetails, "Retrieved job execution details");
    }

    @GetMapping("/summary")
    @Operation(summary = "Get job execution summary",
               description = "Returns aggregated statistics for job executions over a specified period")
    public ResponseEntity<ApiResponse<ComicJobSummary>> getJobSummary(
            @Parameter(description = "Number of days to include in summary")
            @RequestParam(defaultValue = "7") int days) {
        
        ComicJobSummary summary = comicBatchService.getJobSummary(days);
        return ResponseBuilder.ok(summary, "Retrieved job execution summary");
    }

    @PostMapping("/jobs/trigger")
    @Operation(summary = "Manually trigger comic retrieval job",
               description = "Manually starts a comic retrieval job for the specified date")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerJob(
            @Parameter(description = "Target date for comic retrieval")
            @RequestParam(defaultValue = "today") 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        
        if (targetDate == null) {
            targetDate = LocalDate.now();
        }
        
        try {
            JobExecution execution = comicBatchService.runComicRetrievalJob(targetDate, "MANUAL");
            Map<String, Object> result = Map.of(
                "executionId", execution.getId(),
                "status", execution.getStatus().toString(),
                "targetDate", targetDate.toString()
            );
            
            return ResponseBuilder.ok(result, "Job triggered successfully");
        } catch (Exception e) {
            return ResponseBuilder.error(500, "Failed to trigger job: " + e.getMessage());
        }
    }

    /**
     * Convert JobExecution to a simplified map for API response
     */
    private Map<String, Object> convertJobExecutionToMap(JobExecution execution) {
        Map<String, Object> map = new HashMap<>();
        map.put("executionId", execution.getId());
        map.put("jobName", execution.getJobInstance().getJobName());
        map.put("status", execution.getStatus().toString());
        map.put("exitCode", execution.getExitStatus().getExitCode());
        
        if (execution.getStartTime() != null) {
            map.put("startTime", LocalDateTime.ofInstant(
                execution.getStartTime().toInstant(), ZoneId.systemDefault()));
        }
        
        if (execution.getEndTime() != null) {
            map.put("endTime", LocalDateTime.ofInstant(
                execution.getEndTime().toInstant(), ZoneId.systemDefault()));
            
            long durationSeconds = java.time.Duration.between(
                execution.getStartTime().toInstant(),
                execution.getEndTime().toInstant()
            ).getSeconds();
            map.put("durationSeconds", durationSeconds);
        }
        
        map.put("jobParameters", execution.getJobParameters().getParameters());
        
        return map;
    }

    /**
     * Convert JobExecution to a detailed map including step information
     */
    private Map<String, Object> convertJobExecutionToDetailedMap(JobExecution execution) {
        Map<String, Object> map = convertJobExecutionToMap(execution);
        
        // Add step execution details
        List<Map<String, Object>> stepDetails = execution.getStepExecutions().stream()
                .map(this::convertStepExecutionToMap)
                .toList();
        map.put("stepExecutions", stepDetails);
        
        // Add failure exceptions if any
        if (!execution.getAllFailureExceptions().isEmpty()) {
            List<String> failures = execution.getAllFailureExceptions().stream()
                    .map(Throwable::getMessage)
                    .toList();
            map.put("failureMessages", failures);
        }
        
        return map;
    }

    /**
     * Convert StepExecution to map
     */
    private Map<String, Object> convertStepExecutionToMap(StepExecution stepExecution) {
        Map<String, Object> map = new HashMap<>();
        map.put("stepName", stepExecution.getStepName());
        map.put("status", stepExecution.getStatus().toString());
        map.put("readCount", stepExecution.getReadCount());
        map.put("writeCount", stepExecution.getWriteCount());
        map.put("commitCount", stepExecution.getCommitCount());
        map.put("skipCount", stepExecution.getSkipCount());
        map.put("rollbackCount", stepExecution.getRollbackCount());
        
        if (stepExecution.getStartTime() != null) {
            map.put("startTime", LocalDateTime.ofInstant(
                stepExecution.getStartTime().toInstant(), ZoneId.systemDefault()));
        }
        
        if (stepExecution.getEndTime() != null) {
            map.put("endTime", LocalDateTime.ofInstant(
                stepExecution.getEndTime().toInstant(), ZoneId.systemDefault()));
        }
        
        return map;
    }
}