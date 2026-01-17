package org.stapledon.comics.server;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides "given" methods to set up test data and scenarios for integration tests. Modeled after {@code StapledonAccountGivens}.
 */
@Slf4j @Component
class ComicServerGivens {

    /**
     * Creates a context for a given comic.
     *
     * @param comicId the comic ID
     * @return the given context
     */
    public GivenComicContext givenComic(int comicId) {
        return GivenComicContext.builder().comicId(comicId).build();
    }

    /**
     * Context for a comic in a test scenario.
     */
    @Setter @Getter @Builder @NoArgsConstructor @AllArgsConstructor @ToString
    public static class GivenComicContext {
        private int comicId;
        private String comicName;

        public GivenComicContext comicId(int comicId) {
            this.comicId = comicId;
            return this;
        }

        public GivenComicContext comicName(String comicName) {
            this.comicName = comicName;
            return this;
        }
    }
}
