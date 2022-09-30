package org.stapledon.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stapledon.downloader.GoComics;
import org.stapledon.downloader.IDailyComic;
import org.stapledon.web.WebInspectorImpl;

import java.time.LocalDate;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GoComicsBootstrap implements IComicsBootstrap {
    String name;
    LocalDate startDate;

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
        return new GoComics(new WebInspectorImpl());
    }
}
