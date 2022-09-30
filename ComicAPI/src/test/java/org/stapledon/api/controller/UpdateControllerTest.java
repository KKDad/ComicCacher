package org.stapledon.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.stapledon.api.service.StartupReconciler;
import org.stapledon.api.service.UpdateService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(UpdateController.class)
class UpdateControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UpdateService updateService;

    @MockBean
    private StartupReconciler startupReconciler;

    @Test
    void updateAll() throws Exception {
        when(updateService.updateAll()).thenReturn(true);

        this.mockMvc.perform(get("/api/v1/update"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(updateService, times(1)).updateAll();
    }

    @Test
    void updateSpecific() throws Exception {
        when(updateService.updateComic(42)).thenReturn(true);

        this.mockMvc.perform(get("/api/v1/update/42"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(updateService, times(1)).updateComic(anyInt());
    }
}
