package org.stapledon.common.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Container for retrieval records, used for JSON serialization.
 */
@Data
public class ComicRetrievalRecordStorage {
    /**
     * Last update timestamp
     */
    private OffsetDateTime lastUpdated;

    /**
     * List of retrieval records
     */
    private List<ComicRetrievalRecord> records;

    public ComicRetrievalRecordStorage() {
        this.lastUpdated = OffsetDateTime.now();
        this.records = new ArrayList<>();
    }
}
