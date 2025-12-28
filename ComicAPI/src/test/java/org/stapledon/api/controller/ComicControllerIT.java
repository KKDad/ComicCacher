package org.stapledon.api.controller;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ImageDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;


class ComicControllerIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testRetrieveAllComics() {
        ResponseEntity<ComicItem[]> response = restTemplate.getForEntity("/api/v1/comics", ComicItem[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testRetrieveComicDetails() {
        ResponseEntity<ComicItem> response = restTemplate.getForEntity("/api/v1/comics/42", ComicItem.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testRetrieveComicDetailsNotFound() {
        ResponseEntity<ComicItem> response = restTemplate.getForEntity("/api/v1/comics/999", ComicItem.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testCreateComicDetails() {
        ComicItem comic = new ComicItem();
        // Set comic properties here
        ResponseEntity<ComicItem> response = restTemplate.postForEntity("/api/v1/comics/42", comic, ComicItem.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testUpdateComicDetails() {
        ComicItem comic = new ComicItem();
        // Set comic properties here
        restTemplate.patchForObject("/api/v1/comics/42", comic, ComicItem.class);
        ResponseEntity<ComicItem> response = restTemplate.getForEntity("/api/v1/comics/42", ComicItem.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testDeleteComicDetails() {
        restTemplate.delete("/api/v1/comics/42");
        ResponseEntity<ComicItem> response = restTemplate.getForEntity("/api/v1/comics/42", ComicItem.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testRetrieveAvatar() {
        ResponseEntity<ImageDto> response = restTemplate.getForEntity("/api/v1/comics/42/avatar", ImageDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testRetrieveFirstComicImage() {
        ResponseEntity<ImageDto> response = restTemplate.getForEntity("/api/v1/comics/42/strips/first", ImageDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testRetrieveNextComicImage() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        ResponseEntity<ImageDto> response = restTemplate.getForEntity("/api/v1/comics/42/next/" + date, ImageDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testRetrievePreviousComicImage() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        ResponseEntity<ImageDto> response = restTemplate.getForEntity("/api/v1/comics/42/previous/" + date, ImageDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testRetrieveLastComicImage() {
        ResponseEntity<ImageDto> response = restTemplate.getForEntity("/api/v1/comics/42/strips/last", ImageDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}