package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stapledon.AbstractIntegrationTest;

/**
 * Verifies the Spring Boot Actuator health endpoint is accessible without authentication.
 * This is the endpoint used by the Docker HEALTHCHECK — a 401/403 here means unhealthy containers.
 */
class ActuatorHealthIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("Actuator health endpoint should be accessible without authentication")
    void healthEndpointAccessibleWithoutAuth() throws Exception {
        var result = mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.status").exists())
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("Actuator health must not return 401/403 — security misconfiguration would break Docker HEALTHCHECK")
                .isNotIn(401, 403);
    }
}
