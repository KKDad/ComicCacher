package org.stapledon.engine.source;

import java.time.LocalDate;

/**
 * One comic in a source's catalog, as the source lists it.
 *
 * <p>{@code identifier} is the comic's slug at the source (the {@code sourceIdentifier} a configured comic uses); {@code thumbnailUrl} is the
 * source's image for the comic, or null; {@code startDate} is the comic's first strip at the source when the catalog says (ComicsKingdom does), or null;
 * {@code details} is its description and tags when the catalog carries them (Comics Kingdom does), or null when they are fetched separately.
 */
public record SourceCatalogEntry(String identifier, String name, String author, String thumbnailUrl, LocalDate startDate, CatalogDetails details) {

    public SourceCatalogEntry(String identifier, String name, String author, String thumbnailUrl, LocalDate startDate) {
        this(identifier, name, author, thumbnailUrl, startDate, null);
    }
}
