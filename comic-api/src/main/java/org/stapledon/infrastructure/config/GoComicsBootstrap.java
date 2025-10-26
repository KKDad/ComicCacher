package org.stapledon.infrastructure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.config.IComicsBootstrap;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.JsoupInspectorService;
import org.stapledon.engine.downloader.GoComics;
import org.stapledon.engine.downloader.IDailyComic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GoComicsBootstrap implements IComicsBootstrap {
    String name;
    LocalDate startDate;
    String sourceIdentifier; // Optional explicit override for URL slug
    List<DayOfWeek> publicationDays; // Optional: days comic publishes (null/empty = daily)
    Boolean active; // Optional: whether comic is actively publishing (null/true = active)

    @Autowired
    private InspectorService webInspector;

    @Autowired
    private CacheProperties cacheProperties;

    @Override
    public String stripName() {
        return this.name;
    }

    @Override
    public LocalDate startDate() {
        return this.startDate;
    }

    @Override
    public IDailyComic getDownloader() {
        return new GoComics(
            webInspector != null ? webInspector : new JsoupInspectorService(),
            cacheProperties
        );
    }

    @Override
    public String getSource() {
        return "gocomics";
    }

    @Override
    public String getSourceIdentifier() {
        // Use explicit sourceIdentifier if provided, otherwise auto-generate
        if (sourceIdentifier != null && !sourceIdentifier.isEmpty()) {
            return sourceIdentifier;
        }
        return stripName().replace(" ", "").toLowerCase();
    }

    @Override
    public List<DayOfWeek> getPublicationDays() {
        return publicationDays;
    }

    @Override
    public Boolean getActive() {
        return active != null ? active : true;
    }
}
