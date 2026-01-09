package org.stapledon.common.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Value object representing a comic's identity.
 * Encapsulates the (comicId, comicName) pair that is passed throughout the
 * system.
 * 
 * Immutable and suitable for use as a cache key.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public final class ComicIdentifier {

    private final int id;
    private final String name;

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
}
