package org.stapledon.api.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stapledon.dto.ComicItem;
import org.stapledon.dto.ComicList;
import org.stapledon.dto.ImageDto;
import org.stapledon.utils.CacheUtils;
import org.stapledon.utils.Direction;
import org.stapledon.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComicsServiceImpl implements ComicsService {
    private final String cacheLocation;
    private final CacheUtils cacheUtils;

    @Getter
    private static final List<ComicItem> comics = Collections.synchronizedList(new ArrayList<>());

    /**
     * Return details of all configured comics
     *
     * @return list of all configured comics
     */
    @Override
    public List<ComicItem> retrieveAll() {
        List<ComicItem> result;

        // Create a thread-safe copy of the comics list
        synchronized (comics) {
            result = new ArrayList<>(comics);
        }

        // Sort the copy (no need for synchronization during sort)
        Collections.sort(result);
        return result;
    }

    /**
     * Return details of a specific comic
     *
     * @param comicId - Comic to lookup
     * @return Optional containing details of the comic, empty if not found
     */
    @Override
    public Optional<ComicItem> retrieveComic(int comicId) {
        Optional<ComicItem> comicOpt;

        // Use proper synchronization when accessing the shared list
        synchronized (comics) {
            comicOpt = comics.stream()
                    .filter(p -> p.getId() == comicId)
                    .findFirst();

            if (comicOpt.isEmpty()) {
                log.error("Unknown comic id={}, total known: {}", comicId, comics.size());
            }
        }

        return comicOpt;
    }

    @Override
    public synchronized Optional<ComicItem> createComic(int comicId, ComicItem comicItem) {
        if (comics.contains(comicItem)) {
            return Optional.empty();
        }
        comics.add(comicItem);
        return Optional.of(comicItem);
    }

    @Override
    public synchronized Optional<ComicItem> updateComic(int comicId, ComicItem comicItem) {
        comics.add(comicItem);
        return Optional.of(comicItem);
    }

    @Override
    public synchronized boolean deleteComic(int comicId) {
        return retrieveComic(comicId)
                .map(comics::remove)
                .orElse(false);
    }


    /**
     * Returns the strip image for a specified api
     *
     * @param comicId - Comic to retrieve
     * @param which   - Direction to retrive from, either oldest or newest.
     * @return 200 with the image or 404 with no response body if not found
     */
    @Override
    public Optional<ImageDto> retrieveComicStrip(int comicId, Direction which) throws IOException {
        log.info("Entering retrieveComicStrip for comicId={}, Direction={}", comicId, which);

        return this.retrieveComic(comicId)
                .flatMap(comic -> {
                    try {
                        File image = cacheUtils.findFirst(comic, which);

                        if (image == null) {
                            log.error("Unable to locate first strip for {}", comic.getName());
                            return Optional.empty();
                        }

                        return Optional.ofNullable(ImageUtils.getImageDto(image));
                    } catch (IOException e) {
                        log.error("Error retrieving comic strip: {}", e.getMessage(), e);
                        return Optional.empty();
                    }
                });
    }

    @Override
    public Optional<ImageDto> retrieveComicStrip(int comicId, Direction which, LocalDate from) throws IOException {
        log.info("Entering retrieveComicStrip for comicId={}, Direction={}, from={}", comicId, which, from);

        return this.retrieveComic(comicId)
                .flatMap(comic -> {
                    try {
                        File image = which == Direction.FORWARD
                                ? cacheUtils.findNext(comic, from)
                                : cacheUtils.findPrevious(comic, from);

                        if (image == null) {
                            log.error("Unable to locate strip for {} from {}", comic.getName(), from);
                            return Optional.empty();
                        }

                        return Optional.ofNullable(ImageUtils.getImageDto(image));
                    } catch (IOException e) {
                        log.error("Error retrieving comic strip: {}", e.getMessage(), e);
                        return Optional.empty();
                    }
                });
    }

    /**
     * Returns the avatar for a specified comic
     *
     * @param comicId - Comic to retrieve
     * @return Optional containing the avatar image, empty if not found
     */
    @Override
    public Optional<ImageDto> retrieveAvatar(int comicId) throws IOException {
        return this.retrieveComic(comicId)
                .flatMap(comic -> {
                    try {
                        String comicNameParsed = comic.getName().replace(" ", "");
                        var avatar = new File(String.format("%s/%s/avatar.png", cacheLocation, comicNameParsed));

                        if (!avatar.exists()) {
                            log.error("Unable to locate avatar for {}", comic.getName());
                            log.error("   checked {}", avatar.getAbsolutePath());
                            return Optional.empty();
                        }

                        return Optional.ofNullable(ImageUtils.getImageDto(avatar));
                    } catch (IOException e) {
                        log.error("Error retrieving avatar: {}", e.getMessage(), e);
                        return Optional.empty();
                    }
                });
    }
}
