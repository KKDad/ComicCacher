package org.stapledon.common.dto;

/**
 * Value object representing a comic's identity.
 * Encapsulates the (comicId, comicName) pair that is passed throughout the
 * system.
 *
 * <p>
 * Immutable and suitable for use as a cache key.
 * </p>
 */
public record ComicIdentifier(int id, String name) {

    /**
     * Creates a ComicIdentifier from a ComicItem.
     */
    public static ComicIdentifier from(ComicItem comic) {
        return new ComicIdentifier(comic.getId(), comic.getName());
    }

    /**
     * Returns the sanitized directory name for this comic.
     * Falls back to "comic_{id}" if name is null or empty.
     */
    public String getDirectoryName() {
        if (name == null || name.trim().isEmpty()) {
            return "comic_" + id;
        }
        return name.replace(" ", "");
    }

    /**
     * Returns the comic ID.
     * Provided for backward compatibility with code expecting getId().
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the comic name.
     * Provided for backward compatibility with code expecting getName().
     */
    public String getName() {
        return name;
    }
}
