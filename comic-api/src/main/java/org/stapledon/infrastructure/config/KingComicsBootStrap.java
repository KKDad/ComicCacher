package org.stapledon.infrastructure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.common.config.IComicsBootstrap;
import org.stapledon.engine.downloader.ComicsKingdom;
import org.stapledon.engine.downloader.IDailyComic;
import org.stapledon.common.infrastructure.web.WebInspector;
import org.stapledon.common.infrastructure.web.WebInspectorImpl;

import java.time.LocalDate;

import lombok.Getter;

@Getter
public class KingComicsBootStrap implements IComicsBootstrap {
    String name;
    String website;
    LocalDate startDate;

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
        return stripName().replace(" ", "-").toLowerCase();
    }
}