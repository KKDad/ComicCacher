package org.stapledon.config;

import org.stapledon.downloader.GoComics;
import org.stapledon.downloader.IDailyComic;
import org.stapledon.downloader.KingFeatures;
import org.stapledon.web.WebInspector;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"squid:ClassVariableVisibilityCheck", "squid:S1845"})
public class CacherBootstrapConfig
{
        CacherBootstrapConfig()
        {
                this.dailyComics = new ArrayList<>();
                this.kingComics = new ArrayList<>();

        }
        public List<GoComicsBootstrap> dailyComics;
        public List<KingComicsBootStrap> kingComics;

        public class GoComicsBootstrap implements IComicsBootstrap
        {
                public String name;
                public LocalDate startDate;

                public GoComicsBootstrap() {
                        // No args constructor for required for Gson deserialize
                }

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

        public class KingComicsBootStrap implements IComicsBootstrap
        {
                public String name;
                public String website;
                public LocalDate startDate;

                public KingComicsBootStrap() {
                        // No args constructor for required for Gson deserialize
                }
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
                        return new KingFeatures(new WebInspector(), this.website);
                }
        }
}
