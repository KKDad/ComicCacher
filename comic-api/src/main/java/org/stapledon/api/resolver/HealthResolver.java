package org.stapledon.api.resolver;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.ErrorCode;
import org.stapledon.api.dto.health.ComponentHealth;
import org.stapledon.api.dto.health.HealthStatus;
import org.stapledon.api.service.SystemHealthService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for health and error-code queries.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HealthResolver {

    private final SystemHealthService systemHealthService;

    /**
     * Get the health status of the application.
     */
    @QueryMapping
    public HealthStatus health(@Argument boolean detailed) {
        log.debug("GraphQL: Health check request, detailed={}", detailed);
        return detailed
                ? systemHealthService.getDetailedHealthStatus()
                : systemHealthService.getHealthStatus();
    }

    /**
     * Get all known error codes defined in the schema.
     */
    @QueryMapping
    public List<String> errorCodes() {
        return Arrays.stream(ErrorCode.values())
                .map(ErrorCode::name)
                .toList();
    }

    // =========================================================================
    // Schema Mappings — bridge Java DTOs to GraphQL types
    // =========================================================================

    /**
     * Converts Map&lt;String, ComponentHealth&gt; to [ComponentHealthEntry!] for GraphQL.
     */
    @SchemaMapping(typeName = "HealthStatus", field = "components")
    public List<ComponentHealthEntryDto> components(HealthStatus healthStatus) {
        Map<String, ComponentHealth> components = healthStatus.getComponents();
        if (components == null) {
            return List.of();
        }
        return components.entrySet().stream()
                .map(entry -> new ComponentHealthEntryDto(
                        entry.getKey(),
                        entry.getValue().getStatus().name(),
                        entry.getValue().getMessage()))
                .toList();
    }

    /**
     * Flattens nested MemoryInfo/DiskSpace into the flat GraphQL SystemResources type.
     */
    @SchemaMapping(typeName = "HealthStatus", field = "systemResources")
    public SystemResourcesDto systemResources(HealthStatus healthStatus) {
        var sr = healthStatus.getSystemResources();
        if (sr == null) {
            return null;
        }
        return new SystemResourcesDto(
                sr.getAvailableProcessors(),
                sr.getMemory() != null ? (double) sr.getMemory().getTotalMemory() : null,
                sr.getMemory() != null ? (double) sr.getMemory().getFreeMemory() : null,
                sr.getMemory() != null ? (double) sr.getMemory().getMaxMemory() : null,
                sr.getMemory() != null ? sr.getMemory().getUsedPercentage() : null);
    }

    // =========================================================================
    // Intermediate Records
    // =========================================================================

    public record ComponentHealthEntryDto(String name, String status, String details) {
    }

    public record SystemResourcesDto(
            Integer availableProcessors,
            Double totalMemory,
            Double freeMemory,
            Double maxMemory,
            Double memoryUsagePercent) {
    }
}
