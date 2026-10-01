package org.stapledon.engine.source;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
        /** The source's image for the comic; downloaded on demand into the temporary thumbnail folder, never served from here. */
        private String thumbnailUrl;
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
