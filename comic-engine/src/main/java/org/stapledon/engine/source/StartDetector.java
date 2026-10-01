package org.stapledon.engine.source;

import java.io.IOException;
import java.util.Optional;

import org.stapledon.common.dto.ComicItem;

/**
 * Reads where a comic starts at its source. Implementations pace their requests through {@code SourceThrottleService}.
 */
public interface StartDetector {

    /**
     * The comic's first strip at the source, or empty when the source doesn't say.
     */
    Optional<StartInfo> detect(ComicItem comic) throws IOException;
}
