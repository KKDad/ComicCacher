package org.stapledon.engine.source;

import java.io.IOException;
import java.util.Optional;

/**
 * Reads a comic's description and tags from its page at the source, for sources whose catalog doesn't carry them. Implementations pace their requests
 * through {@code SourceThrottleService}.
 */
public interface DetailsFetcher {

    /**
     * The comic's details at the source, or empty when its page has none.
     */
    Optional<CatalogDetails> fetch(String identifier) throws IOException;
}
