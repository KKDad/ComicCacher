package org.stapledon.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.stapledon.api.service.ComicsService;
import org.stapledon.api.service.DailyRunner;
import org.stapledon.api.service.JsonBootstrapper;
import org.stapledon.config.GsonProvider;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageDto;
import org.stapledon.utils.TestUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    private JsonBootstrapper JsonBootstrapper;

    @Test
    void retrieveAllComics() throws Exception {
        when(comicsService.retrieveAll()).thenReturn(List.of(
                getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007),
                getComicItem(187, "The Dysfunctional Family Circus", "Bil Keane", "The Dysfunctional Family Circus is the name of several long-running parodies of the syndicated api strip The Family Circus, featuring either Bil Keane's artwork with altered captions, or (less often) original artwork made to appear like the targeted strips.", 1989, 8, 3, 2013)
        ));

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        var responseDto = new GsonProvider().gson().fromJson(responseBody, ComicItem[].class);
        assertThat(responseDto).hasSize(2);

        verify(comicsService, times(1)).retrieveAll();
    }


    @Test
    void retrieveComicDetails() throws Exception {
        when(comicsService.retrieveComic(42)).thenReturn(
                getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007)
        );

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics/42"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        var item = new GsonProvider().gson().fromJson(responseBody, ComicItem.class);
        assertThat(item).isNotNull();

        verify(comicsService, times(1)).retrieveComic(42);
    }

    @Test
    void retrieveComicDetailsNotFound() throws Exception {
        when(comicsService.retrieveComic(42)).thenReturn(null);
        this.mockMvc.perform(get("/api/v1/comics/42"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createComicDetailsTest() throws Exception {
        var comic = getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007);
        when(comicsService.updateComic(42, comic)).thenReturn(comic);

        var mvcResult = this.mockMvc.perform(patch("/api/v1/comics/42")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(comic)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        var item = new GsonProvider().gson().fromJson(responseBody, ComicItem.class);
        assertThat(item).isNotNull();

        verify(comicsService, times(1)).updateComic(42, comic);
    }

    @Test
    void createComicDetailsNowAllowedTest() throws Exception {
        var comic = getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007);
        when(comicsService.updateComic(42, comic)).thenReturn(null);
        this.mockMvc.perform(patch("/api/v1/comics/42")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(comic)))
                .andExpect(status().isConflict());
    }


    @Test
    void updateComicDetailsTest() throws Exception {
        var comic = getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007);
        when(comicsService.createComic(42, comic)).thenReturn(comic);

        var mvcResult = this.mockMvc.perform(post("/api/v1/comics/42")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(comic)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        var item = new GsonProvider().gson().fromJson(responseBody, ComicItem.class);
        assertThat(item).isNotNull();

        verify(comicsService, times(1)).createComic(42, comic);
    }

    @Test
    void updateComicDetailsNowAllowedTest() throws Exception {
        var comic = getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007);
        when(comicsService.updateComic(42, comic)).thenReturn(null);
        this.mockMvc.perform(post("/api/v1/comics/42")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(comic)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteComicDetails() throws Exception {
        when(comicsService.deleteComic(42)).thenReturn(true);

        this.mockMvc.perform(delete("/api/v1/comics/42"))
                .andExpect(status().isNoContent())
                .andReturn();

        verify(comicsService, times(1)).deleteComic(42);
    }

    @Test
    void deleteComicNotFound() throws Exception {
        when(comicsService.deleteComic(42)).thenReturn(false);

        var mvcResult = this.mockMvc.perform(delete("/api/v1/comics/42"))
                .andExpect(status().isNotFound())
                .andReturn();

        verify(comicsService, times(1)).deleteComic(42);
    }

    @Test
    void retrieveAvatar() throws Exception {
        var image = ImageDto.builder().build();
        when(comicsService.retrieveAvatar(42)).thenReturn(Optional.ofNullable(image));

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics/42/avatar"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        var item = new GsonProvider().gson().fromJson(responseBody, ImageDto.class);
        assertThat(item).isNotNull();

        verify(comicsService, times(1)).retrieveAvatar(42);
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

    private static ComicItem getComicItem(int id, String Art_Comics_Daily, String Bebe_Williams, String description, int year, int month, int dayOfMonth, int year1) {
        return ComicItem.builder()
                .id(id)
                .name(Art_Comics_Daily)
                .author(Bebe_Williams)
                .description(description)
                .oldest(LocalDate.of(year, month, dayOfMonth))
                .newest(LocalDate.of(year1, 12, 8))
                .build();
    }
}