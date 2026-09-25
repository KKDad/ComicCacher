package org.stapledon.common.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * Data Transfer Object representing a request to download a comic.
 * Contains all information needed to identify and download a specific comic on
 * a specific date.
 */
@Data
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
public class ComicDownloadRequest {
    /**
     * The unique identifier of the comic.
     */
    @ToString.Include
    private final int comicId;

    /**
     * The name of the comic.
     */
    @ToString.Include
    private final String comicName;

    /**
     * The source where the comic can be downloaded from.
     */
    private final String source;

    /**
     * The URL or identifier used to locate the comic at the source.
     */
    private final String sourceIdentifier;

    /**
     * The date of the comic to download.
     */
    @ToString.Include
    private final LocalDate date;

    /**
     * When true, an HTTP 429 fails the download at once instead of retrying. The source-wide backoff still applies. Used by backfill, which can simply try again later.
     */
    private final boolean failFastOnRateLimit;
}
