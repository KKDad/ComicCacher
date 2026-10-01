package org.stapledon.common.dto;

/**
 * Where a comic's start value ({@code sourceStartDate} or {@code firstStripNumber}) came from.
 */
public enum StartSource {
    /** Read from the source (its catalog, a strip page) or proven by a stored strip. */
    DETECTED,
    /** Set by an admin. Detection never overwrites it; only a stored strip older than it does. */
    MANUAL
}
