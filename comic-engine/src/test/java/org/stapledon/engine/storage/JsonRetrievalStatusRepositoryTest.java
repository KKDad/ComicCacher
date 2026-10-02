package org.stapledon.engine.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.util.GsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

class JsonRetrievalStatusRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T16:00:00Z"), ZoneId.of("America/Toronto"));

    @TempDir
    Path cacheRoot;

    @Test
    void purgeFallsBackToTheStripDateForRecordsWrittenWithoutAnAttemptTime() throws Exception {
        // Written before attemptedAt was kept: one strip from last month, one from yesterday
        Files.writeString(cacheRoot.resolve("retrieval-status.json"), """
                {"lastUpdated": "2026-10-01T12:00:00Z", "records": [
                  {"id": "Old_2026-08-20", "comicName": "Old", "comicDate": "2026-08-20", "status": "SUCCESS"},
                  {"id": "New_2026-10-01", "comicName": "New", "comicDate": "2026-10-01", "status": "SUCCESS"}
                ]}
                """);
        JsonRetrievalStatusRepository repository = new JsonRetrievalStatusRepository(GsonUtils.createGson(),
                CacheProperties.builder().location(cacheRoot.toString()).build(), CLOCK);

        assertThat(repository.purgeOldRecords(30)).isEqualTo(1);

        assertThat(repository.getRecords(null, null, null, null, 10)).extracting(ComicRetrievalRecord::getComicName).containsExactly("New");
    }

    @Test
    void saveStampsTheAttemptTimeFromTheClock() {
        JsonRetrievalStatusRepository repository = new JsonRetrievalStatusRepository(GsonUtils.createGson(),
                CacheProperties.builder().location(cacheRoot.toString()).build(), CLOCK);

        repository.saveRecord(ComicRetrievalRecord.success(7, "Garfield", CLOCK.instant().atZone(CLOCK.getZone()).toLocalDate(), "gocomics", 100, 2000L));

        assertThat(repository.getRecords(null, null, null, null, 1).getFirst().getAttemptedAt())
                .isEqualTo(OffsetDateTime.parse("2026-10-02T16:00:00Z"));
    }
}
