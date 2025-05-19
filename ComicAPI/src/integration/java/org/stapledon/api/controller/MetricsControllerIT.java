package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.stapledon.AbstractIntegrationTest;

class MetricsControllerIT extends AbstractIntegrationTest {

    @Test
    void getStorageMetricsTest() throws Exception {
        try {
            MvcResult result = mockMvc.perform(get("/api/v1/metrics/storage"))
                .andDo(print())
                .andReturn();
                
            System.out.println("Get storage metrics response status: " + result.getResponse().getStatus());
            System.out.println("Get storage metrics response: " + result.getResponse().getContentAsString());
            
            // For test environments, we can be more relaxed with expectations
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            
            // Basic validation that response contains expected data
            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent).contains("data");
        } catch (Exception e) {
            System.out.println("Test exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void getAccessMetricsTest() throws Exception {
        try {
            MvcResult result = mockMvc.perform(get("/api/v1/metrics/access"))
                .andDo(print())
                .andReturn();
                
            System.out.println("Get access metrics response status: " + result.getResponse().getStatus());
            System.out.println("Get access metrics response: " + result.getResponse().getContentAsString());
            
            // For test environments, we can be more relaxed with expectations
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            
            // Basic validation that response contains expected data
            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent).contains("data");
        } catch (Exception e) {
            System.out.println("Test exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void getCombinedMetricsTest() throws Exception {
        try {
            MvcResult result = mockMvc.perform(get("/api/v1/metrics/combined"))
                .andDo(print())
                .andReturn();
                
            System.out.println("Get combined metrics response status: " + result.getResponse().getStatus());
            System.out.println("Get combined metrics response: " + result.getResponse().getContentAsString());
            
            // For test environments, we can be more relaxed with expectations
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            
            // Basic validation that response contains expected data
            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent).contains("data");
        } catch (Exception e) {
            System.out.println("Test exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void refreshStorageMetricsTest() throws Exception {
        try {
            MvcResult result = mockMvc.perform(get("/api/v1/metrics/storage/refresh"))
                .andDo(print())
                .andReturn();
                
            System.out.println("Refresh storage metrics response status: " + result.getResponse().getStatus());
            System.out.println("Refresh storage metrics response: " + result.getResponse().getContentAsString());
            
            // For test environments, we can be more relaxed with expectations
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            
            // Basic validation that response contains expected data
            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent).contains("data");
        } catch (Exception e) {
            System.out.println("Test exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}