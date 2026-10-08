package org.stapledon.engine.source;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.downloader.ComicDownloaderStrategy;
import org.stapledon.engine.downloader.XkcdDownloaderStrategy;

/**
 * xkcd, a single numbered comic. Its catalog is that one comic, and it starts at strip 1; neither needs a request.
 */
@Component
public class XkcdSource implements ComicSource {

    static final String ID = "xkcd";
    private static final String IDENTIFIER = "xkcd";
    private static final String SITE_URL = "https://xkcd.com/";

    private final XkcdDownloaderStrategy downloader;

    public XkcdSource(XkcdDownloaderStrategy downloader) {
        this.downloader = downloader;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "xkcd";
    }

    @Override
    public ComicDownloaderStrategy downloader() {
        return downloader;
    }

    @Override
    public String identifierFor(ComicItem comic) {
        return IDENTIFIER;
    }

    @Override
    public String comicPageUrl(String identifier) {
        return SITE_URL;
    }

    @Override
    public Set<String> imageHosts() {
        return Set.of();
    }

    @Override
    public Optional<SourceCatalog> catalog() {
        return Optional.of(new SourceCatalog() {
            @Override
            public String catalogUrl() {
                return SITE_URL;
            }

            @Override
            public List<SourceCatalogEntry> fetch() {
                return List.of(new SourceCatalogEntry(IDENTIFIER, "xkcd", "Randall Munroe", null, null));
            }
        });
    }

    @Override
    public Optional<StartDetector> startDetector() {
        return Optional.of(_ -> Optional.of(StartInfo.ofStripNumber(1)));
    }
}
