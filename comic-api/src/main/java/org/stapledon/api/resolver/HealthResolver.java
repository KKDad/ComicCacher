package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.health.HealthStatus;
import org.stapledon.api.service.HealthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for Health operations.
 * Provides health status information about the application.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HealthResolver {

    private final HealthService healthService;

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get the health status of the application.
     * 
     * @param detailed Whether to include detailed metrics (default: false)
     */
    @QueryMapping
    public HealthStatus health(@Argument Boolean detailed) {
        boolean isDetailed = detailed != null && detailed;
        log.debug("Getting health status, detailed={}", isDetailed);

        return isDetailed
                ? healthService.getDetailedHealthStatus()
                : healthService.getHealthStatus();
    }
}
