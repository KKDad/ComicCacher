package org.stapledon.engine.source;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.stapledon.engine.batch.scheduler.JobParameterDefinition.Option;
import org.stapledon.engine.downloader.DownloaderFacade;

/**
 * The one list of comic sources. Collects every {@link ComicSource} bean and registers its downloader with the {@link DownloaderFacade}, so a new
 * source needs no other wiring. Everything that lists or checks sources (job parameters, comic validation, the Sources page) asks here.
 */
@Slf4j
@Component
public class SourceRegistry {

    private final Map<String, ComicSource> sources = new LinkedHashMap<>();

    /**
     * Registers every source, sorted by display name, and its downloader.
     */
    public SourceRegistry(List<ComicSource> sources, DownloaderFacade downloaderFacade) {
        sources.stream()
                .sorted(Comparator.comparing(ComicSource::displayName, String.CASE_INSENSITIVE_ORDER))
                .forEach(source -> {
                    if (this.sources.putIfAbsent(source.id(), source) != null) {
                        throw new IllegalStateException("Two comic sources share the id " + source.id());
                    }
                    downloaderFacade.registerDownloaderStrategy(source.id(), source.downloader());
                });
        log.info("Registered {} comic sources: {}", this.sources.size(), this.sources.keySet());
    }

    /** Every source, by display name. */
    public List<ComicSource> all() {
        return List.copyOf(sources.values());
    }

    /** The source with this id. */
    public Optional<ComicSource> find(String id) {
        return Optional.ofNullable(id).map(sources::get);
    }

    /** True when a source has this id. */
    public boolean isKnown(String id) {
        return id != null && sources.containsKey(id);
    }

    /**
     * Options for a batch job's "source" parameter: "ALL" and then every source.
     */
    public List<Option> jobSourceOptions() {
        List<Option> options = new ArrayList<>();
        options.add(new Option("ALL", "All Sources"));
        sources.values().forEach(source -> options.add(new Option(source.id(), source.displayName())));
        return options;
    }
}
