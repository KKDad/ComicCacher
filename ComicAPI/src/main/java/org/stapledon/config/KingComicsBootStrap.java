package org.stapledon.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.downloader.ComicsKingdom;
import org.stapledon.downloader.IDailyComic;
import org.stapledon.web.WebInspector;
import org.stapledon.web.WebInspectorImpl;

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