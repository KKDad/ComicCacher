package org.stapledon.api.dto.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for system health status
 * Contains overall system health information and component-specific metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString(onlyExplicitlyIncluded = true)
public class HealthStatus {

    /**
     * Overall status of the application
     */
    @ToString.Include
    private Status status;

    /**
     * Current server time when check was performed
     */
    private LocalDateTime timestamp;

    /**
     * Application uptime in milliseconds
     */
    private long uptime;

    /**
     * Application version and build information
     */
    private BuildInfo buildInfo;

    /**
     * System resource metrics (CPU, memory, etc.)
     */
    private SystemResources systemResources;

    /**
     * Cache status information
     */
    private CacheStatus cacheStatus;

    /**
     * Status of individual components
     */
    private Map<String, ComponentHealth> components;

    /**
     * Possible health statuses
     */
    public enum Status {
        UP,
        DEGRADED,
        DOWN
    }
}