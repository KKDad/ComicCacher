package org.stapledon.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.stapledon.downloader.GoComics;
import org.stapledon.downloader.IDailyComic;
import org.stapledon.web.WebInspector;

import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoComicsBootstrap implements IComicsBootstrap {
    public String name;
    public LocalDate startDate;

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
        return new GoComics(new WebInspector());
    }
}
