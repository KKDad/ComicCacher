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
     * Returns the sanitized directory name for this comic: the name with spaces removed.
     * Falls back to "comic_{id}" if the name is empty or could escape the cache root
     * (path separators, "." or "..").
     */
    public String getDirectoryName() {
        if (name == null) {
            return "comic_" + id;
        }
        String dirName = name.replace(" ", "");
        if (dirName.isEmpty() || dirName.equals(".") || dirName.equals("..")
                || dirName.contains("/") || dirName.contains("\\")) {
            return "comic_" + id;
        }
        return dirName;
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
