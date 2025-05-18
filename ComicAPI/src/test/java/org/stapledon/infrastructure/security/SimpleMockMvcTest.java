package org.stapledon.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.stapledon.api.controller.ComicController;
import org.stapledon.api.exception.GlobalExceptionHandler;
import org.stapledon.core.comic.service.ComicsService;

import java.util.ArrayList;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simple standalone MockMvc test that doesn't rely on Spring Security configuration
 */
public class SimpleMockMvcTest {

    private MockMvc mockMvc;
    
    @Mock
    private ComicsService comicsService;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(comicsService.retrieveAll()).thenReturn(new ArrayList<>());
        
        ComicController comicController = new ComicController(comicsService);
        mockMvc = MockMvcBuilders.standaloneSetup(comicController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
    
    @Test
    public void testGetComicsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/comics"))
                .andExpect(status().isOk());
    }
}