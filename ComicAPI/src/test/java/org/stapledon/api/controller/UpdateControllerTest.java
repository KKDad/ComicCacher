package org.stapledon.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.stapledon.api.service.UpdateService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone tests for UpdateController that don't rely on Spring context
 */
class UpdateControllerTest {
    
    private MockMvc mockMvc;
    
    @Mock
    private UpdateService updateService;
    
    private UpdateController updateController;
    
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        updateController = new UpdateController(updateService);
        mockMvc = MockMvcBuilders.standaloneSetup(updateController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
    
    @Test
    void updateAll() throws Exception {
        // Given
        when(updateService.updateAll()).thenReturn(true);
        
        // When & Then
        this.mockMvc.perform(get("/api/v1/update"))
                .andDo(print())
                .andExpect(status().isOk());
        
        verify(updateService, times(1)).updateAll();
    }
    
    @Test
    void updateSpecific() throws Exception {
        // Given
        when(updateService.updateComic(42)).thenReturn(true);
        
        // When & Then
        this.mockMvc.perform(get("/api/v1/update/42"))
                .andDo(print())
                .andExpect(status().isOk());
        
        verify(updateService, times(1)).updateComic(anyInt());
    }
    
    @Test
    void updateAllFailure() throws Exception {
        // Given
        when(updateService.updateAll()).thenReturn(false);

        // When & Then
        this.mockMvc.perform(get("/api/v1/update"))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(updateService, times(1)).updateAll();
    }

    @Test
    void updateSpecificFailure() throws Exception {
        // Given
        when(updateService.updateComic(42)).thenReturn(false);

        // When & Then
        this.mockMvc.perform(get("/api/v1/update/42"))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(updateService, times(1)).updateComic(anyInt());
    }
}