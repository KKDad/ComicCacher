package org.stapledon.engine.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageMetadata;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.engine.downloader.XkcdDownloaderStrategy;
import org.stapledon.engine.storage.ImageMetadataRepository;

/**
 * Moves xkcd's alt text to where it belongs in the metadata sidecars. Before strips had an alt text field, xkcd's alt text was saved as the
 * transcript, so an xkcd sidecar with a transcript and no alt text gets that text moved into {@code altText}. Local files only, no web requests,
 * and safe to repeat: a moved sidecar has alt text and is skipped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripTextMigrationService {

    private final CacheProperties cacheProperties;
    private final ImageMetadataRepository imageMetadataRepository;
    private final ComicConfigurationService comicConfigurationService;

    /**
     * Moves the alt text out of the transcript in every xkcd sidecar that still has it there, returning how many sidecars changed.
     */
    public int moveXkcdAltText() {
        List<ComicItem> comics = comicConfigurationService.loadComicConfig().getItems().values().stream()
                .filter(comic -> XkcdDownloaderStrategy.SOURCE_IDENTIFIER.equals(comic.getSource()))
                .toList();
        int moved = 0;
        for (ComicItem comic : comics) {
            Path dir = Path.of(cacheProperties.getLocation(), ComicIdentifier.from(comic).getDirectoryName());
            if (!Files.isDirectory(dir)) {
                continue;
            }
            int movedForComic = 0;
            try (Stream<Path> paths = Files.walk(dir)) {
                for (Path strip : paths.filter(path -> path.getFileName().toString().endsWith(".png")).toList()) {
                    if (moveAltText(strip)) {
                        movedForComic++;
                    }
                }
            } catch (IOException e) {
                log.error("Couldn't move the alt text for {} out of its transcripts", comic.getName(), e);
            }
            if (movedForComic > 0) {
                log.info("Moved the alt text out of the transcript for {} {} strips", movedForComic, comic.getName());
            }
            moved += movedForComic;
        }
        return moved;
    }

    private boolean moveAltText(Path strip) {
        Optional<ImageMetadata> metadata = imageMetadataRepository.loadMetadata(strip.toString());
        if (metadata.isEmpty() || metadata.get().getTranscript() == null || metadata.get().getAltText() != null) {
            return false;
        }
        ImageMetadata moved = metadata.get().toBuilder()
                .filePath(strip.toString())
                .altText(metadata.get().getTranscript())
                .transcript(null)
                .build();
        boolean saved = imageMetadataRepository.saveMetadata(moved);
        if (saved) {
            log.debug("Moved the alt text out of the transcript for {}", strip);
        }
        return saved;
    }
}
