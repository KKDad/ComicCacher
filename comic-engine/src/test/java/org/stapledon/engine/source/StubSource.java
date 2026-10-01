package org.stapledon.engine.source;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.downloader.ComicDownloaderStrategy;
import org.stapledon.engine.downloader.DailyComicDownloaderStrategy;

/**
 * A comic source for tests: its catalog and start answers are set by the test, and nothing touches the network.
 */
class StubSource implements ComicSource {

    private final String id;
    private final ComicDownloaderStrategy downloader;
    final List<SourceCatalogEntry> catalogEntries = new ArrayList<>();
    final Map<String, StartInfo> starts = new HashMap<>();
    IOException catalogFailure;
    int catalogFetches;

    StubSource(String id) {
        this(id, mock(DailyComicDownloaderStrategy.class));
    }

    StubSource(String id, ComicDownloaderStrategy downloader) {
        this.id = id;
        this.downloader = downloader;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return id.toUpperCase(Locale.ROOT);
    }

    @Override
    public ComicDownloaderStrategy downloader() {
        return downloader;
    }

    @Override
    public String identifierFor(ComicItem comic) {
        return comic.getSourceIdentifier() != null ? comic.getSourceIdentifier() : comic.getName().replace(" ", "").toLowerCase(Locale.ROOT);
    }

    @Override
    public String comicPageUrl(String identifier) {
        return "https://" + id + ".example/" + identifier;
    }

    @Override
    public Set<String> imageHosts() {
        return Set.of("127.0.0.1", "img.example");
    }

    @Override
    public Optional<SourceCatalog> catalog() {
        return Optional.of(new SourceCatalog() {
            @Override
            public String catalogUrl() {
                return "https://" + id + ".example/catalog";
            }

            @Override
            public List<SourceCatalogEntry> fetch() throws IOException {
                catalogFetches++;
                if (catalogFailure != null) {
                    throw catalogFailure;
                }
                return List.copyOf(catalogEntries);
            }
        });
    }

    @Override
    public Optional<StartDetector> startDetector() {
        return Optional.of(comic -> Optional.ofNullable(starts.get(identifierFor(comic))));
    }
}
