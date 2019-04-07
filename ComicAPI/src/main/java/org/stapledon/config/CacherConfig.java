package org.stapledon.config;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"squid:ClassVariableVisibilityCheck"})
public class CacherConfig
{
        CacherConfig()
        {
                this.dailyComics = new ArrayList<>();

        }
        public class GoComics
        {
                // Name of the Comic Strip
                public String name;

                // Earliest Date to go back and start caching from
                public LocalDate startDate;

                public GoComics() {
                        // No args constructor for required for Gson deserialize
                }

        }
        public String cacheDirectory;
        public List<GoComics> dailyComics;
}
