package org.stapledon.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.stapledon.api.service.ComicsService;
import org.stapledon.api.service.DailyRunner;
import org.stapledon.api.service.StartupReconciler;
import org.stapledon.config.GsonProvider;
import org.stapledon.dto.ComicItem;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComicController.class)
class ComicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComicsService comicsService;

    @MockBean
    private DailyRunner dailyRunner;

    @MockBean
    private StartupReconciler startupReconciler;

    @Test
    void retrieveAllComics() throws Exception {
        when(comicsService.retrieveAll()).thenReturn(
                List.of(
                        ComicItem.builder()
                                .id(42)
                                .name("Art Comics Daily")
                                .author("Bebe Williams")
                                .description("Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.")
                                .oldest(LocalDate.of(1995, 5, 31))
                                .newest(LocalDate.of(2007, 12, 8))
                                .build(),
                        ComicItem.builder()
                                .id(187)
                                .name("The Dysfunctional Family Circus")
                                .author("Bil Keane")
                                .description("The Dysfunctional Family Circus is the name of several long-running parodies of the syndicated api strip The Family Circus, featuring either Bil Keane's artwork with altered captions, or (less often) original artwork made to appear like the targeted strips.")
                                .oldest(LocalDate.of(1989, 8, 3))
                                .newest(LocalDate.of(2013, 12, 8))
                                .build()
                )
        );

        MvcResult mvcResult = this.mockMvc.perform(get("/api/v1/comics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        var responseDto = new GsonProvider().gson().fromJson(responseBody, ComicItem[].class);
        assertThat(responseDto).hasSize(2);

        verify(comicsService, times(1)).retrieveAll();
    }


    @Test
    void retrieveComicDetails() {
    }

    @Test
    void createComicDetails() {
    }

    @Test
    void updateComicDetails() {
    }

    @Test
    void deleteComicDetails() {
    }

    @Test
    void retrieveAvatar() {
    }

    @Test
    void retrieveFirstComicImage() {
    }

    @Test
    void retrieveNextComicImage() {
    }

    @Test
    void retrievePreviousComicImage() {
    }

    @Test
    void retrieveLastComicImage() {
    }
}