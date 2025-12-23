package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.stapledon.api.exception.GlobalExceptionHandler;
import org.stapledon.api.model.ApiResponse;
import org.stapledon.api.service.TestUtil;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.infrastructure.config.GsonProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Standalone tests for ComicController that don't rely on Spring context
 */
class ComicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManagementFacade comicManagementFacade;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // Automatically discover and register modules, including JavaTimeModule

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        ComicController comicController = new ComicController(comicManagementFacade, Optional.empty(), Optional.empty());
        mockMvc = MockMvcBuilders.standaloneSetup(comicController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void retrieveAllComics() throws Exception {
        when(comicManagementFacade.getAllComics()).thenReturn(List.of(
                getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007),
                getComicItem(187, "The Dysfunctional Family Circus", "Bil Keane", "The Dysfunctional Family Circus is the name of several long-running parodies of the syndicated api strip The Family Circus, featuring either Bil Keane's artwork with altered captions, or (less often) original artwork made to appear like the targeted strips.", 1989, 8, 3, 2013)
        ));

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        ApiResponse<List<ComicItem>> response = objectMapper.readValue(responseBody,
                new TypeReference<ApiResponse<List<ComicItem>>>() {});
        assertThat(response.getData()).hasSize(2);

        verify(comicManagementFacade, times(1)).getAllComics();
    }

    @Test
    void retrieveComicDetails() throws Exception {
        when(comicManagementFacade.getComic(42)).thenReturn(
                Optional.of(getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007))
        );

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics/42"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        ApiResponse<ComicItem> response = objectMapper.readValue(responseBody,
                new TypeReference<ApiResponse<ComicItem>>() {});
        assertThat(response.getData()).isNotNull();

        verify(comicManagementFacade, times(1)).getComic(42);
    }

    @Test
    void retrieveComicDetailsNotFound() throws Exception {
        when(comicManagementFacade.getComic(42)).thenReturn(Optional.empty());
        this.mockMvc.perform(get("/api/v1/comics/42"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createComicDetailsTest() throws Exception {
        var comic = getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007);
        when(comicManagementFacade.updateComic(42, comic)).thenReturn(Optional.of(comic));

        var mvcResult = this.mockMvc.perform(patch("/api/v1/comics/42")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(comic)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        ApiResponse<ComicItem> response = objectMapper.readValue(responseBody,
                new TypeReference<ApiResponse<ComicItem>>() {});
        assertThat(response.getData()).isNotNull();

        verify(comicManagementFacade, times(1)).updateComic(42, comic);
    }

    @Test
    void createComicDetailsNowAllowedTest() throws Exception {
        var comic = getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007);
        when(comicManagementFacade.updateComic(42, comic)).thenReturn(Optional.empty());
        this.mockMvc.perform(patch("/api/v1/comics/42")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(comic)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateComicDetailsTest() throws Exception {
        var comic = getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007);
        when(comicManagementFacade.createComic(comic)).thenReturn(Optional.of(comic));

        var mvcResult = this.mockMvc.perform(post("/api/v1/comics/42")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(comic)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        ApiResponse<ComicItem> response = objectMapper.readValue(responseBody,
                new TypeReference<ApiResponse<ComicItem>>() {});
        assertThat(response.getData()).isNotNull();

        verify(comicManagementFacade, times(1)).createComic(comic);
    }

    @Test
    void updateComicDetailsNowAllowedTest() throws Exception {
        var comic = getComicItem(42, "Art Comics Daily", "Bebe Williams", "Art Comics Daily is a pioneering webcomic first published in March 1995 by Bebe Williams, who lives in Arlington, Virginia, United States. The webcomic was published on the Internet rather than in print in order to reserve some artistic freedom. Art Comics Daily has been on permanent hiatus since 2007.", 1995, 5, 31, 2007);
        when(comicManagementFacade.createComic(comic)).thenReturn(Optional.empty());
        this.mockMvc.perform(post("/api/v1/comics/42")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(comic)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deleteComicDetails() throws Exception {
        when(comicManagementFacade.deleteComic(42)).thenReturn(true);

        this.mockMvc.perform(delete("/api/v1/comics/42"))
                .andExpect(status().isNoContent())
                .andReturn();

        verify(comicManagementFacade, times(1)).deleteComic(42);
    }

    @Test
    void deleteComicNotFound() throws Exception {
        when(comicManagementFacade.deleteComic(42)).thenReturn(false);

        var mvcResult = this.mockMvc.perform(delete("/api/v1/comics/42"))
                .andExpect(status().isNotFound())
                .andReturn();

        verify(comicManagementFacade, times(1)).deleteComic(42);
    }

    @Test
    void retrieveAvatar() throws Exception {
        var image = ImageDto.builder().build();
        when(comicManagementFacade.getAvatar(42)).thenReturn(Optional.ofNullable(image));

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics/42/avatar"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        var item = new GsonProvider().gson().fromJson(responseBody, ImageDto.class);
        assertThat(item).isNotNull();

        verify(comicManagementFacade, times(1)).getAvatar(42);
    }

    @Test
    void retrieveFirstComicImage() throws Exception {
        var image = ImageDto.builder().build();
        var result = ComicNavigationResult.found(image, null, null);
        when(comicManagementFacade.getComicStrip(eq(42), any(Direction.class))).thenReturn(result);

        this.mockMvc.perform(get("/api/v1/comics/42/strips/first"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));

        verify(comicManagementFacade, times(1)).getComicStrip(eq(42), any(Direction.class));
    }

    @Test
    void retrieveNextComicImage() throws Exception {
        var image = ImageDto.builder().build();
        var result = ComicNavigationResult.found(image, null, null);
        when(comicManagementFacade.getComicStrip(eq(42), eq(Direction.FORWARD), any(LocalDate.class))).thenReturn(result);

        this.mockMvc.perform(get("/api/v1/comics/42/next/2022-01-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));

        verify(comicManagementFacade, times(1)).getComicStrip(eq(42), eq(Direction.FORWARD), any(LocalDate.class));
    }

    @Test
    void retrievePreviousComicImage() throws Exception {
        var image = ImageDto.builder().build();
        var result = ComicNavigationResult.found(image, null, null);
        when(comicManagementFacade.getComicStrip(eq(42), eq(Direction.BACKWARD), any(LocalDate.class))).thenReturn(result);

        this.mockMvc.perform(get("/api/v1/comics/42/previous/2022-01-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));

        verify(comicManagementFacade, times(1)).getComicStrip(eq(42), eq(Direction.BACKWARD), any(LocalDate.class));
    }

    @Test
    void retrieveLastComicImage() throws Exception {
        var image = ImageDto.builder().build();
        var result = ComicNavigationResult.found(image, null, null);
        when(comicManagementFacade.getComicStrip(eq(42), any(Direction.class))).thenReturn(result);

        this.mockMvc.perform(get("/api/v1/comics/42/strips/last"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));

        verify(comicManagementFacade, times(1)).getComicStrip(eq(42), any(Direction.class));
    }

    @Test
    void retrieveFirstComicImageWhenAtEnd() throws Exception {
        var result = ComicNavigationResult.notFound("AT_END", LocalDate.of(2022, 1, 1),
                LocalDate.of(2021, 12, 31), null);
        when(comicManagementFacade.getComicStrip(eq(42), any(Direction.class))).thenReturn(result);

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics/42/strips/first"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        assertThat(responseBody).contains("\"found\":false");
        assertThat(responseBody).contains("\"reason\":\"AT_END\"");
        assertThat(responseBody).contains("\"nearestPreviousDate\":\"2021-12-31\"");

        verify(comicManagementFacade, times(1)).getComicStrip(eq(42), any(Direction.class));
    }

    @Test
    void retrieveNextComicImageWhenAtEnd() throws Exception {
        var result = ComicNavigationResult.notFound("AT_END", LocalDate.of(2022, 1, 1),
                LocalDate.of(2021, 12, 31), null);
        when(comicManagementFacade.getComicStrip(eq(42), eq(Direction.FORWARD), any(LocalDate.class)))
                .thenReturn(result);

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics/42/next/2022-01-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        assertThat(responseBody).contains("\"found\":false");
        assertThat(responseBody).contains("\"reason\":\"AT_END\"");

        verify(comicManagementFacade, times(1))
                .getComicStrip(eq(42), eq(Direction.FORWARD), any(LocalDate.class));
    }

    @Test
    void retrievePreviousComicImageWhenAtBeginning() throws Exception {
        var result = ComicNavigationResult.notFound("AT_BEGINNING", LocalDate.of(2022, 1, 1),
                null, LocalDate.of(2022, 1, 2));
        when(comicManagementFacade.getComicStrip(eq(42), eq(Direction.BACKWARD), any(LocalDate.class)))
                .thenReturn(result);

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics/42/previous/2022-01-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        assertThat(responseBody).contains("\"found\":false");
        assertThat(responseBody).contains("\"reason\":\"AT_BEGINNING\"");
        assertThat(responseBody).contains("\"nearestNextDate\":\"2022-01-02\"");

        verify(comicManagementFacade, times(1))
                .getComicStrip(eq(42), eq(Direction.BACKWARD), any(LocalDate.class));
    }

    @Test
    void retrieveLastComicImageWhenNoComicsAvailable() throws Exception {
        var result = ComicNavigationResult.notFound("NO_COMICS_AVAILABLE", null, null, null);
        when(comicManagementFacade.getComicStrip(eq(42), any(Direction.class))).thenReturn(result);

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics/42/strips/last"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        assertThat(responseBody).contains("\"found\":false");
        assertThat(responseBody).contains("\"reason\":\"NO_COMICS_AVAILABLE\"");

        verify(comicManagementFacade, times(1)).getComicStrip(eq(42), any(Direction.class));
    }

    @Test
    void retrieveComicImageVerifyFoundTrueStructure() throws Exception {
        var image = ImageDto.builder()
                .imageDate(LocalDate.of(2022, 6, 15))
                .imageData("base64data")
                .mimeType("image/png")
                .build();
        var result = ComicNavigationResult.found(image,
                LocalDate.of(2022, 6, 14),
                LocalDate.of(2022, 6, 16));
        when(comicManagementFacade.getComicStrip(eq(42), any(Direction.class))).thenReturn(result);

        var mvcResult = this.mockMvc.perform(get("/api/v1/comics/42/strips/first"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        assertThat(responseBody).contains("\"found\":true");
        assertThat(responseBody).contains("\"imageDate\":\"2022-06-15\"");
        assertThat(responseBody).contains("\"nearestPreviousDate\":\"2022-06-14\"");
        assertThat(responseBody).contains("\"nearestNextDate\":\"2022-06-16\"");
        assertThat(responseBody).contains("\"currentDate\":\"2022-06-15\"");

        verify(comicManagementFacade, times(1)).getComicStrip(eq(42), any(Direction.class));
    }

    private static ComicItem getComicItem(int id, String name, String author, String description, int year, int month, int dayOfMonth, int endYear) {
        return ComicItem.builder()
                .id(id)
                .name(name)
                .author(author)
                .description(description)
                .oldest(LocalDate.of(year, month, dayOfMonth))
                .newest(LocalDate.of(endYear, 12, 8))
                .source("gocomics")
                .sourceIdentifier(name.replace(" ", "").toLowerCase())
                .enabled(true)
                .avatarAvailable(false)
                .build();
    }
}