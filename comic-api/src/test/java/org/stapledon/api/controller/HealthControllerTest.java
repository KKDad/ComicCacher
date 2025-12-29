package org.stapledon.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.stapledon.api.dto.health.BuildInfo;
import org.stapledon.api.dto.health.CacheStatus;
import org.stapledon.api.dto.health.HealthStatus;
import org.stapledon.api.dto.health.SystemResources;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.service.HealthService;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the HealthController
 */
class HealthControllerTest {

    @Mock
    private HealthService mockHealthService;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getHealthStatus_withoutDetailedFlag_shouldReturnBasicHealthStatus() {
        // Arrange
        HealthStatus mockStatus = createBasicHealthStatus();
        when(mockHealthService.getHealthStatus()).thenReturn(mockStatus);

        // Act
        ResponseEntity<ApiResponse<HealthStatus>> response = healthController.getHealthStatus(false);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(mockStatus);

        // Verify HealthService was called
        verify(mockHealthService).getHealthStatus();
    }

    @Test
    void getHealthStatus_withDetailedFlag_shouldReturnDetailedHealthStatus() {
        // Arrange
        HealthStatus mockStatus = createDetailedHealthStatus();
        when(mockHealthService.getDetailedHealthStatus()).thenReturn(mockStatus);

        // Act
        ResponseEntity<ApiResponse<HealthStatus>> response = healthController.getHealthStatus(true);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(mockStatus);

        // Verify HealthService was called
        verify(mockHealthService).getDetailedHealthStatus();
    }

    /**
     * Creates a basic health status for testing
     */
    private HealthStatus createBasicHealthStatus() {
        return HealthStatus.builder()
                .status(HealthStatus.Status.UP)
                .timestamp(LocalDateTime.now())
                .uptime(60000)
                .buildInfo(BuildInfo.builder()
                        .name("ComicAPI")
                        .version("1.0.0")
                        .javaVersion("17")
                        .build())
                .build();
    }

    /**
     * Creates a detailed health status for testing
     */
    private HealthStatus createDetailedHealthStatus() {
        return HealthStatus.builder()
                .status(HealthStatus.Status.UP)
                .timestamp(LocalDateTime.now())
                .uptime(60000)
                .buildInfo(BuildInfo.builder()
                        .name("ComicAPI")
                        .version("1.0.0")
                        .javaVersion("17")
                        .build())
                .systemResources(SystemResources.builder()
                        .availableProcessors(4)
                        .memory(SystemResources.MemoryInfo.builder()
                                .totalMemory(1024)
                                .freeMemory(512)
                                .maxMemory(2048)
                                .usedPercentage(50.0)
                                .build())
                        .diskSpace(SystemResources.DiskSpace.builder()
                                .total(10240)
                                .free(5120)
                                .usable(5120)
                                .usedPercentage(50.0)
                                .build())
                        .build())
                .cacheStatus(CacheStatus.builder()
                        .totalComics(5)
                        .totalImages(100)
                        .totalStorageBytes(1024 * 1024 * 10)
                        .cacheLocation("/tmp/comics")
                        .build())
                .components(new HashMap<>())
                .build();
    }
}