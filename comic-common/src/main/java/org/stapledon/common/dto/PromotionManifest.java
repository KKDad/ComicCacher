package org.stapledon.common.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * The strips one instance has on disk for a date range, which another instance reads to promote them (dev to prod). Comics are identified by
 * source and source identifier, since each instance numbers its comics separately.
 */
public record PromotionManifest(
        LocalDate from,
        LocalDate to,
        List<Comic> comics
) {

    /**
     * One comic and the dates in the range it has a strip for.
     */
    public record Comic(
            String source,
            String sourceIdentifier,
            String name,
            List<LocalDate> dates
    ) {
    }
}
