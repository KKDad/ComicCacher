package org.stapledon.api.service;

import org.stapledon.api.dto.health.HealthStatus;

/**
 * Service interface for health check functionality
 */
public interface HealthService {
    
    /**
     * Get the current health status of the application
     * 
     * @return Health status information
     */
    HealthStatus getHealthStatus();
    
    /**
     * Get detailed health status, including more comprehensive metrics
     * 
     * @return Detailed health status information
     */
    HealthStatus getDetailedHealthStatus();
}