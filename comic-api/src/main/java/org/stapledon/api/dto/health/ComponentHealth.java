package org.stapledon.api.dto.health;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for component health status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComponentHealth {
    
    /**
     * Status of the component
     */
    private HealthStatus.Status status;
    
    /**
     * Optional details specific to this component
     */
    private Map<String, Object> details;
    
    /**
     * Optional reason for current status
     */
    private String message;
}