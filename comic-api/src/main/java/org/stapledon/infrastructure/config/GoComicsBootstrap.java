package org.stapledon.infrastructure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.common.config.IComicsBootstrap;
import org.stapledon.engine.downloader.GoComics;
import org.stapledon.engine.downloader.IDailyComic;
import org.stapledon.common.infrastructure.web.WebInspector;
import org.stapledon.common.infrastructure.web.WebInspectorImpl;

import java.time.LocalDate;

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

    @Autowired
    private WebInspector webInspector;

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
        return new GoComics(webInspector != null ? webInspector : new WebInspectorImpl());
    }

    @Override
    public String getSource() {
        return "gocomics";
    }

    @Override
    public String getSourceIdentifier() {
        return stripName().replace(" ", "").toLowerCase();
    }
}
