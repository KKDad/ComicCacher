package org.stapledon.common.dto;

import java.time.LocalDateTime;
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
    private LocalDateTime lastUpdated;

    /**
     * List of retrieval records
     */
    private List<ComicRetrievalRecord> records;

    public ComicRetrievalRecordStorage() {
        this.lastUpdated = LocalDateTime.now();
        this.records = new ArrayList<>();
    }
}
