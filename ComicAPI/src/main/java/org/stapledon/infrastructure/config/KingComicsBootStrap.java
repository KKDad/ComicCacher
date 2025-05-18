package org.stapledon.infrastructure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.core.comic.downloader.ComicsKingdom;
import org.stapledon.core.comic.downloader.IDailyComic;
import org.stapledon.infrastructure.web.WebInspector;
import org.stapledon.infrastructure.web.WebInspectorImpl;

import java.time.LocalDate;

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
}