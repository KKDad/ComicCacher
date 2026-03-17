package org.stapledon.common.dto;

import org.stapledon.common.util.Direction;

import java.time.LocalDate;

/**
 * Key for batching strip lookups via DataLoader.
 * Enables the DataLoader to batch multiple strip requests and execute them efficiently.
 * <p>
 * Two sealed subtypes:
 * <ul>
 *   <li>{@link DateStripKey} - Lookup strip by exact date</li>
 *   <li>{@link BoundaryStripKey} - Lookup first/last strip by direction</li>
 * </ul>
 */
public sealed interface StripLoaderKey permits StripLoaderKey.DateStripKey, StripLoaderKey.BoundaryStripKey {
    int comicId();

    String comicName();

    /**
     * Strip lookup by exact date.
     */
    record DateStripKey(
            int comicId,
            String comicName,
            LocalDate date
    ) implements StripLoaderKey { }

    /**
     * Strip lookup by boundary direction (first/last).
     */
    record BoundaryStripKey(
            int comicId,
            String comicName,
            Direction direction
    ) implements StripLoaderKey {

        /**
         * Factory for first strip (oldest).
         */
        public static BoundaryStripKey first(int comicId, String comicName) {
            return new BoundaryStripKey(comicId, comicName, Direction.FORWARD);
        }

        /**
         * Factory for last strip (newest).
         */
        public static BoundaryStripKey last(int comicId, String comicName) {
            return new BoundaryStripKey(comicId, comicName, Direction.BACKWARD);
        }
    }
}
