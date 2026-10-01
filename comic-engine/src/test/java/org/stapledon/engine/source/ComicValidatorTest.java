package org.stapledon.engine.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.stapledon.common.dto.ComicItem;
import org.stapledon.engine.downloader.ComicsKingdomDownloaderStrategy;
import org.stapledon.engine.downloader.DownloaderFacade;
import org.stapledon.engine.downloader.FreefallDownloaderStrategy;
import org.stapledon.engine.downloader.GoComicsDownloaderStrategy;
import org.stapledon.engine.downloader.SourceThrottleService;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.engine.source.ComicValidator.Problem;

class ComicValidatorTest {

    private ManagementFacade comics;
    private ComicValidator validator;

    private final ComicItem peanuts = ComicItem.builder().id(1).name("Peanuts").source("gocomics").sourceIdentifier("peanuts").build();
    private final ComicItem beetle = ComicItem.builder().id(2).name("Beetle Bailey").source("comicskingdom").build();

    @BeforeEach
    void setUp() {
        comics = mock(ManagementFacade.class);
        when(comics.getAllComics()).thenReturn(List.of(peanuts, beetle));
        SourceThrottleService throttle = mock(SourceThrottleService.class);
        SourceRegistry registry = new SourceRegistry(List.of(
                new GoComicsSource(mock(GoComicsDownloaderStrategy.class), null, throttle),
                new ComicsKingdomSource(mock(ComicsKingdomDownloaderStrategy.class), null, throttle),
                new FreefallSource(mock(FreefallDownloaderStrategy.class))), mock(DownloaderFacade.class));
        validator = new ComicValidator(registry, comics, Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC));
    }

    private static List<String> fields(List<Problem> problems) {
        return problems.stream().map(Problem::field).toList();
    }

    @Test
    void acceptsAGoodNewComic() {
        ComicItem comic = ComicItem.builder().name("Calvin and Hobbes").source("gocomics").sourceIdentifier("calvinandhobbes").build();

        assertThat(validator.validateNew(comic)).isEmpty();
    }

    @Test
    void rejectsBlankDuplicateAndReservedNames() {
        assertThat(fields(validator.validateNew(ComicItem.builder().name(" ").build()))).containsExactly("name");
        assertThat(fields(validator.validateNew(ComicItem.builder().name("PEANUTS").build()))).containsExactly("name");
        assertThat(fields(validator.validateNew(ComicItem.builder().name("Bee tleBailey").build()))).containsExactly("name");
        assertThat(fields(validator.validateNew(ComicItem.builder().name("tmp").build()))).containsExactly("name");
    }

    @Test
    void rejectsBadIdentifiersAndUnknownSources() {
        assertThat(fields(validator.validateNew(ComicItem.builder().name("X").source("gocomics").sourceIdentifier("../etc").build())))
                .containsExactly("sourceIdentifier");
        assertThat(fields(validator.validateNew(ComicItem.builder().name("X").source("nowhere").build()))).containsExactly("source");
    }

    @Test
    void rejectsASecondComicForTheSameSourceComic() {
        assertThat(fields(validator.validateNew(ComicItem.builder().name("Peanuts Again").source("gocomics").sourceIdentifier("peanuts").build())))
                .containsExactly("sourceIdentifier");
        // Beetle Bailey has no identifier: its fallback from the name still counts
        assertThat(fields(validator.validateNew(ComicItem.builder().name("Beetle").source("comicskingdom").sourceIdentifier("beetle-bailey").build())))
                .containsExactly("sourceIdentifier");
    }

    @Test
    void numberedComicsNeedAFirstStrip() {
        ComicItem freefall = ComicItem.builder().name("Freefall").source("freefall").build();

        assertThat(fields(validator.validateNew(freefall))).containsExactly("firstStripNumber");
        assertThat(validator.validateNew(freefall.toBuilder().firstStripNumber(1).build())).isEmpty();
    }

    @Test
    void rejectsImpossibleStarts() {
        assertThat(fields(validator.validateNew(ComicItem.builder().name("X").firstStripNumber(0).build()))).containsExactly("firstStripNumber");
        assertThat(fields(validator.validateNew(ComicItem.builder().name("X").firstStripNumber(10).lastStripNumber(5).build())))
                .containsExactly("firstStripNumber");
        assertThat(fields(validator.validateNew(ComicItem.builder().name("X").sourceStartDate(LocalDate.of(2026, 9, 29)).build())))
                .containsExactly("sourceStartDate");
    }

    @Test
    void updateChecksOnlyChangedFields() {
        // An identifier saved before validation existed doesn't block switching the comic off
        ComicItem legacy = ComicItem.builder().id(3).name("Legacy").source("gocomics").sourceIdentifier("Legacy Slug").build();

        assertThat(validator.validateUpdate(legacy, legacy.toBuilder().active(false).build())).isEmpty();
        assertThat(fields(validator.validateUpdate(legacy, legacy.toBuilder().name("Peanuts").build()))).containsExactly("name");
    }

    @Test
    void updateDoesNotClashWithItself() {
        assertThat(validator.validateUpdate(peanuts, peanuts.toBuilder().name("Peanuts").sourceIdentifier("peanuts").author("Schulz").build())).isEmpty();
    }
}
