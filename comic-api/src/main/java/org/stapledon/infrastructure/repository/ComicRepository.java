package org.stapledon.infrastructure.repository;

import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicItem;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Comic persistence operations.
 * Abstracts the underlying storage mechanism (JSON files, database, etc.)
 * from the business logic layer.
 */
public interface ComicRepository {

    /**
     * Loads the complete comic configuration containing all comic items.
     */
    ComicConfig loadComicConfig();

    /**
     * Saves the complete comic configuration.
     */
    void saveComicConfig(ComicConfig config);

    /**
     * Finds a comic by its unique ID.
     */
    Optional<ComicItem> findComicById(int id);

    /**
     * Finds a comic by its name (case-insensitive).
     */
    Optional<ComicItem> findComicByName(String name);

    /**
     * Retrieves all comics in the repository.
     */
    List<ComicItem> findAllComics();

    /**
     * Saves or updates a single comic item.
     * If the comic already exists (by ID), it will be updated.
     */
    void saveComic(ComicItem comic);

    /**
     * Deletes a comic by its ID.
     */
    void deleteComic(int id);

    /**
     * Checks if a comic with the given ID exists.
     */
    boolean existsById(int id);

    /**
     * Checks if a comic with the given name exists (case-insensitive).
     */
    boolean existsByName(String name);
}
