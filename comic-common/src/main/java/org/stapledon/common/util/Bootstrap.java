package org.stapledon.common.util;

import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.config.IComicsBootstrap;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Bootstrap {
    private List<IComicsBootstrap> dailyComics;
    private List<IComicsBootstrap> kingComics;

    /**
     * Converts the bootstrap comic configurations into a unified ComicConfig object
     * containing all comics from both GoComics and ComicsKingdom sources.
     *
     * @return A ComicConfig object containing all comics
     */
    public ComicConfig getComicConfig() {
        ComicConfig config = new ComicConfig();
        List<ComicItem> comics = new ArrayList<>();

        // Convert dailyComics bootstraps to ComicItems
        if (dailyComics != null) {
            List<ComicItem> items = dailyComics.stream()
                    .map(this::convertBootstrapToComicItem)
                    .toList();
            comics.addAll(items);
        }

        // Convert kingComics bootstraps to ComicItems
        if (kingComics != null) {
            List<ComicItem> items = kingComics.stream()
                    .map(this::convertBootstrapToComicItem)
                    .toList();
            comics.addAll(items);
        }

        config.setComics(comics);
        return config;
    }

    /**
     * Converts an IComicsBootstrap to a ComicItem.
     *
     * @param bootstrap The IComicsBootstrap to convert
     * @return A ComicItem representation of the bootstrap
     */
    private ComicItem convertBootstrapToComicItem(IComicsBootstrap bootstrap) {
        return ComicItem.builder()
                .id(bootstrap.stripName().hashCode())
                .name(bootstrap.stripName())
                .source(bootstrap.getSource())
                .sourceIdentifier(bootstrap.getSourceIdentifier())
                .oldest(bootstrap.startDate())
                .newest(bootstrap.startDate().plusDays(1)) // Default to day after start date
                .enabled(true)
                .build();
    }
}