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
                this.kingComics = new ArrayList<>();

        }
        public List<GoComics> dailyComics;
        public List<KingComics> kingComics;

        public class GoComics
        {
                public String name;
                public LocalDate startDate;

                public GoComics() {
                        // No args constructor for required for Gson deserialize
                }
        }

        public class KingComics
        {
                public String name;
                public String website;
                public LocalDate startDate;

                public KingComics() {
                        // No args constructor for required for Gson deserialize
                }
        }
}
