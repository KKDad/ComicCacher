package org.stapledon.engine.source;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.downloader.ComicDownloaderStrategy;
import org.stapledon.engine.downloader.IndexedComicDownloaderStrategy;

/**
 * Everything ComicCacher knows about one place comics come from. Each source is one bean implementing this interface; {@link SourceRegistry}
 * collects them, registers their downloaders, and is the one list of sources the rest of the app uses (job parameters, validation, the Sources page).
 * <p>
 * Adding a source means writing its downloader strategy and one {@code ComicSource}. The catalog and start-date detection are optional.
 */
public interface ComicSource {

    /** What a source identifier (a comic's slug at its source) may look like. */
    Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,99}$");

    /** The id comics use in their {@code source} field, e.g. "gocomics". */
    String id();

    /** The name shown to people, e.g. "GoComics". */
    String displayName();

    /** The downloader for this source's strips and avatars. */
    ComicDownloaderStrategy downloader();

    /** True when comics from this source are numbered strips rather than dated ones. */
    default boolean indexed() {
        return downloader() instanceof IndexedComicDownloaderStrategy;
    }

    /**
     * The identifier the downloader uses for {@code comic}: its {@code sourceIdentifier}, or the fallback the downloader derives from its name. Used to
     * match configured comics to catalog entries.
     */
    String identifierFor(ComicItem comic);

    /** A link to the comic's page at the source, for people. */
    String comicPageUrl(String identifier);

    /** Hosts that thumbnails and avatars may be downloaded from. Nothing else is fetched from a scraped URL. */
    Set<String> imageHosts();

    /** Lists every comic the source offers, when it publishes such a list. */
    default Optional<SourceCatalog> catalog() {
        return Optional.empty();
    }

    /** Reads a comic's first strip (date or number) from the source, when it can. */
    default Optional<StartDetector> startDetector() {
        return Optional.empty();
    }

    /** True when {@code identifier} is a well-formed source identifier. */
    static boolean isValidIdentifier(String identifier) {
        return identifier != null && IDENTIFIER_PATTERN.matcher(identifier).matches();
    }
}
