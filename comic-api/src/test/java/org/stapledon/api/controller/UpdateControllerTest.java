package org.stapledon.api.controller;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.stapledon.api.exception.GlobalExceptionHandler;
import org.stapledon.engine.management.ManagementFacade;

/**
 * Standalone tests for UpdateController that don't rely on Spring context
 */
class UpdateControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManagementFacade comicManagementFacade;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        UpdateController updateController = new UpdateController(comicManagementFacade);
        mockMvc = MockMvcBuilders.standaloneSetup(updateController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateAll() throws Exception {
        // Given
        when(comicManagementFacade.updateAllComics()).thenReturn(true);

        // When & Then
        this.mockMvc.perform(get("/api/v1/update"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(comicManagementFacade, times(1)).updateAllComics();
    }

    @Test
    void updateSpecific() throws Exception {
        // Given
        when(comicManagementFacade.updateComic(42)).thenReturn(true);

        // When & Then
        this.mockMvc.perform(get("/api/v1/update/42"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(comicManagementFacade, times(1)).updateComic(anyInt());
    }

    @Test
    void updateAllFailure() throws Exception {
        // Given
        when(comicManagementFacade.updateAllComics()).thenReturn(false);

        // When & Then
        this.mockMvc.perform(get("/api/v1/update"))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(comicManagementFacade, times(1)).updateAllComics();
    }

    @Test
    void updateSpecificFailure() throws Exception {
        // Given
        when(comicManagementFacade.updateComic(42)).thenReturn(false);

        // When & Then
        this.mockMvc.perform(get("/api/v1/update/42"))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(comicManagementFacade, times(1)).updateComic(anyInt());
    }
}