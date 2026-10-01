package org.stapledon.engine.source;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Every source's catalog as last read, persisted to {@code source-catalog.json} in the cache root. Entries are never deleted: one the source stops
 * listing gets {@code removedAt}, so a comic that disappears from its source still shows up. Map keys are the source id and the comic's identifier.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceCatalogState {

    @Builder.Default
    private Map<String, SourceEntries> sources = new TreeMap<>();

    /**
     * One source's catalog and how the last refresh went.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class SourceEntries {
        /** When the catalog was last read successfully. */
        private OffsetDateTime lastRefreshed;
        /** When a refresh was last tried, successful or not. */
        private OffsetDateTime lastAttempt;
        /** Why the last refresh failed, or null when it worked. */
        private String lastError;

        @Builder.Default
        private Map<String, Entry> entries = new TreeMap<>();
    }

    /**
     * One comic as the source lists it, plus where the source says it starts.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Entry {
        private String name;
        private String author;
        /** The source's image for the comic; downloaded into the temporary thumbnail folder, never served from here. */
        private String thumbnailUrl;
        /** When the thumbnail was saved; null when there is none on disk (never downloaded, failed, or purged). */
        private OffsetDateTime thumbnailSavedAt;
        /** When the last thumbnail download failed; it is retried a week later. */
        private OffsetDateTime thumbnailFailedAt;
        /** The source's short description of the comic, or null. */
        private String description;
        /** The comic's genres or categories at the source; null or empty when it has none. Replaced whole, never changed in place. */
        private List<String> tags;
        /** When the description and tags were last read from the comic's own page. */
        private OffsetDateTime detailsCheckedAt;
        /** When they should be read again; null when due (or never needed, for a catalog that carries them). */
        private OffsetDateTime detailsExpireAt;
        /** The first strip date the source reports (its catalog or a strip page), or null. */
        private LocalDate startDate;
        /** The first strip number the source reports, for indexed comics, or null. */
        private Integer startStripNumber;
        /** When the start was last read from the source. */
        private OffsetDateTime startCheckedAt;
        private OffsetDateTime firstSeen;
        private OffsetDateTime lastSeen;
        /** When the source stopped listing the comic, or null while it is listed. */
        private OffsetDateTime removedAt;
    }
}
