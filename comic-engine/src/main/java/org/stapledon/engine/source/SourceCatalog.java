package org.stapledon.engine.source;

import java.io.IOException;
import java.util.List;

/**
 * A source's list of every comic it offers.
 */
public interface SourceCatalog {

    /** Where the list is read from, for people and logs. */
    String catalogUrl();

    /**
     * Reads the whole list from the source. Implementations pace their requests through {@code SourceThrottleService}. An empty result is never
     * returned: a page with nothing on it means the layout changed, and throws instead.
     */
    List<SourceCatalogEntry> fetch() throws IOException;
}
