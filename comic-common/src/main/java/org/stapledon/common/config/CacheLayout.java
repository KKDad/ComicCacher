package org.stapledon.common.config;

import java.util.Set;

/**
 * Names of the cache-root directories that hold something other than a comic. Anything that walks the cache root looking for comics skips them.
 */
public final class CacheLayout {

    /** Per-execution batch job logs. */
    public static final String BATCH_LOGS_DIRECTORY = "batch-logs";

    /** Daily metrics snapshots. */
    public static final String METRICS_HISTORY_DIRECTORY = "metrics-history";

    /** Synology's index folder, created by the NAS in every directory. */
    public static final String SYNOLOGY_INDEX_DIRECTORY = "@eaDir";

    /** Disposable files, such as source catalog thumbnails. Excluded from storage metrics and always safe to delete. */
    public static final String TMP_DIRECTORY = "tmp";

    /** Every cache-root directory that isn't a comic. */
    public static final Set<String> NON_COMIC_DIRECTORIES = Set.of(BATCH_LOGS_DIRECTORY, METRICS_HISTORY_DIRECTORY, SYNOLOGY_INDEX_DIRECTORY, TMP_DIRECTORY);

    private CacheLayout() {
    }

    /**
     * True when a cache-root directory with this name could hold a comic.
     */
    public static boolean isComicDirectory(String name) {
        return !NON_COMIC_DIRECTORIES.contains(name);
    }
}
