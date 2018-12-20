package org.stapledon.config;

import java.util.List;

public class ComicCacherConfig {
        public String getCacheDirectory() {
                return cacheDirectory;
        }

        public void setCacheDirectory(String cacheDirectory) {
                this.cacheDirectory = cacheDirectory;
        }

        public List<DailyComicConfig> getDailyComics() {
                return dailyComics;
        }

        public void setDailyComics(List<DailyComicConfig> dailyComics) {
                this.dailyComics = dailyComics;
        }

        private String cacheDirectory;
        private List<DailyComicConfig> dailyComics;
}
