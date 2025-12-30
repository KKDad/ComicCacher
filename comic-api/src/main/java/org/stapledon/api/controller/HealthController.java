package org.stapledon.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stapledon.api.dto.health.HealthStatus;
import org.stapledon.api.model.ResponseBuilder;
import org.stapledon.api.service.HealthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Controller for health check endpoint
 * Provides information about application status and health
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Endpoints for monitoring application health and status")
public class HealthController {

    private final HealthService healthService;

    /**
     * Get the health status of the application
     *
     * @param detailed Whether to include detailed metrics (default: false)
     * @return Health status information
     */
    @Operation(summary = "Get application health status", description = "Returns the current health status of the application, optionally with detailed metrics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Health status retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.stapledon.api.model.ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<org.stapledon.api.model.ApiResponse<HealthStatus>> getHealthStatus(
            @RequestParam(required = false, defaultValue = "false") boolean detailed) {

        HealthStatus status = detailed
                ? healthService.getDetailedHealthStatus()
                : healthService.getHealthStatus();

        return ResponseBuilder.ok(status);
    }
}