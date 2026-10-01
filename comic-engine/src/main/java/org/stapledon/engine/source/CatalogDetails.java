package org.stapledon.engine.source;

import java.util.List;

/**
 * What a source says about a comic beyond its name: a short description and its genres or categories. The description may be null and the tags empty.
 */
public record CatalogDetails(String description, List<String> tags) {

    public CatalogDetails {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
