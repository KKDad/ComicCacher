package org.stapledon.infrastructure.repository;

import org.springframework.stereotype.Repository;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.infrastructure.config.ConfigurationFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON file-based implementation of ComicRepository.
 * Delegates to ApplicationConfigurationFacade for actual file I/O operations.
 */
@Slf4j
@ToString
@Repository
@RequiredArgsConstructor
public class JsonComicRepository implements ComicRepository {

    private final ConfigurationFacade configurationFacade;

    @Override
    public ComicConfig loadComicConfig() {
        return configurationFacade.loadComicConfig();
    }

    @Override
    public void saveComicConfig(ComicConfig config) {
        boolean success = configurationFacade.saveComicConfig(config);
        if (!success) {
            log.error("Failed to save comic configuration");
        }
    }

    @Override
    public Optional<ComicItem> findComicById(int id) {
        ComicConfig config = loadComicConfig();
        if (config.getItems() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(config.getItems().get(id));
    }

    @Override
    public Optional<ComicItem> findComicByName(String name) {
        if (name == null) {
            return Optional.empty();
        }

        ComicConfig config = loadComicConfig();
        if (config.getItems() == null) {
            return Optional.empty();
        }

        return config.getItems().values().stream()
                .filter(comic -> {
                    String comicName = comic.getName();
                    return comicName != null && comicName.equalsIgnoreCase(name);
                })
                .findFirst();
    }

    @Override
    public List<ComicItem> findAllComics() {
        ComicConfig config = loadComicConfig();
        if (config.getItems() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(config.getItems().values());
    }

    @Override
    public void saveComic(ComicItem comic) {
        ComicConfig config = loadComicConfig();
        if (config.getItems() == null) {
            config.setItems(new java.util.concurrent.ConcurrentHashMap<>());
        }
        config.getItems().put(comic.getId(), comic);
        saveComicConfig(config);
    }

    @Override
    public void deleteComic(int id) {
        ComicConfig config = loadComicConfig();
        if (config.getItems() != null) {
            config.getItems().remove(id);
            saveComicConfig(config);
        }
    }

    @Override
    public boolean existsById(int id) {
        return findComicById(id).isPresent();
    }

    @Override
    public boolean existsByName(String name) {
        return findComicByName(name).isPresent();
    }
}
