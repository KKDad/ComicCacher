package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.common.util.GsonUtils;
import org.stapledon.engine.storage.ImageMetadataRepository;

class StripTextMigrationServiceTest {

    @TempDir
    Path cacheRoot;

    private ImageMetadataRepository repository;
    private StripTextMigrationService service;

    @BeforeEach
    void setUp() {
        repository = new ImageMetadataRepository(GsonUtils.createGson());
        ComicConfig config = new ComicConfig();
        config.getItems().put(1, ComicItem.builder().id(1).name("xkcd").source("xkcd").sourceIdentifier("xkcd").build());
        config.getItems().put(2, ComicItem.builder().id(2).name("Freefall").source("freefall").sourceIdentifier("freefall").build());
        ComicConfigurationService configService = mock(ComicConfigurationService.class);
        when(configService.loadComicConfig()).thenReturn(config);
        service = new StripTextMigrationService(CacheProperties.builder().location(cacheRoot.toString()).build(), repository, configService);
    }

    @Test
    void movesXkcdAltTextOutOfTheTranscript() throws IOException {
        Path strip = strip("xkcd", "2026-10-07", "I need to push some updates.", null);

        assertThat(service.moveXkcdAltText()).isEqualTo(1);

        ImageMetadata moved = repository.loadMetadata(strip.toString()).orElseThrow();
        assertThat(moved.getAltText()).isEqualTo("I need to push some updates.");
        assertThat(moved.getTranscript()).isNull();
        assertThat(moved.getWidth()).isEqualTo(740);
    }

    @Test
    void leavesSidecarsThatAlreadyHaveAltTextAndOtherSourcesAlone() throws IOException {
        Path xkcd = strip("xkcd", "2006-01-01", "[[A boy sits in a barrel.]]", "Don't we all.");
        Path freefall = strip("Freefall", "2026-10-07", "Florence: Hello", null);

        assertThat(service.moveXkcdAltText()).isZero();

        assertThat(repository.loadMetadata(xkcd.toString()).orElseThrow().getTranscript()).isEqualTo("[[A boy sits in a barrel.]]");
        assertThat(repository.loadMetadata(freefall.toString()).orElseThrow().getTranscript()).isEqualTo("Florence: Hello");
        assertThat(repository.loadMetadata(freefall.toString()).orElseThrow().getAltText()).isNull();
    }

    @Test
    void runningItAgainChangesNothing() throws IOException {
        strip("xkcd", "2026-10-07", "I need to push some updates.", null);

        service.moveXkcdAltText();

        assertThat(service.moveXkcdAltText()).isZero();
    }

    private Path strip(String dir, String date, String transcript, String altText) throws IOException {
        Path strip = cacheRoot.resolve(dir).resolve(date.substring(0, 4)).resolve(date + ".png");
        Files.createDirectories(strip.getParent());
        Files.write(strip, new byte[] {1});
        repository.saveMetadata(ImageMetadata.builder()
                .filePath(strip.toString())
                .format(ImageFormat.PNG)
                .width(740)
                .height(300)
                .sizeInBytes(1000)
                .colorMode(ImageMetadata.ColorMode.GRAYSCALE)
                .captureTimestamp(OffsetDateTime.of(2026, 10, 7, 12, 0, 0, 0, ZoneOffset.UTC))
                .transcript(transcript)
                .altText(altText)
                .build());
        return strip;
    }
}
