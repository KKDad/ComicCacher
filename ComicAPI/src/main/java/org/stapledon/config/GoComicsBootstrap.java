package org.stapledon.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.stapledon.downloader.GoComics;
import org.stapledon.downloader.IDailyComic;
import org.stapledon.web.WebInspector;
import org.stapledon.web.WebInspectorImpl;

import java.time.LocalDate;

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
}
