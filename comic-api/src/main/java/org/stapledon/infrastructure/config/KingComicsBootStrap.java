package org.stapledon.infrastructure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.common.config.IComicsBootstrap;
import org.stapledon.common.infrastructure.web.WebInspector;
import org.stapledon.common.infrastructure.web.WebInspectorImpl;
import org.stapledon.engine.downloader.ComicsKingdom;
import org.stapledon.engine.downloader.IDailyComic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;

@Getter
public class KingComicsBootStrap implements IComicsBootstrap {
    String name;
    String website;
    LocalDate startDate;
    String sourceIdentifier; // Optional explicit override for URL slug
    List<DayOfWeek> publicationDays; // Optional: days comic publishes (null/empty = daily)
    Boolean active; // Optional: whether comic is actively publishing (null/true = active)

    @Autowired
    private WebInspector webInspector;

    public KingComicsBootStrap() {
        // No args constructor for required for Gson deserialize
    }

    @Override
    public String stripName() {
        return this.name;
    }

    @Override
    public LocalDate startDate() {
        return this.getStartDate();
    }

    @Override
    public IDailyComic getDownloader() {
        return new ComicsKingdom(webInspector != null ? webInspector : new WebInspectorImpl(), this.getWebsite());
    }

    @Override
    public String getSource() {
        return "comicskingdom";
    }

    @Override
    public String getSourceIdentifier() {
        // Use explicit sourceIdentifier if provided, otherwise auto-generate
        if (sourceIdentifier != null && !sourceIdentifier.isEmpty()) {
            return sourceIdentifier;
        }
        return stripName().replace(" ", "-").toLowerCase();
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