package org.stapledon.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

/**
 * Reports how long storage reads take (strip images, avatars, JSON files on NFS), so a slow page load can be traced to the disk.
 * <p>
 * Each read is logged at DEBUG and passed to a listener, which the API installs to add the read to the current request's timings and to
 * warn about slow reads. Without a listener (tests, batch-only use) reads are only logged.
 */
@Slf4j
public final class StorageTimings {

    private static final BiConsumer<String, Long> NO_LISTENER = (what, nanos) -> {
    };

    private static volatile BiConsumer<String, Long> listener = NO_LISTENER;

    private StorageTimings() {
        // Utility class - prevent instantiation
    }

    /**
     * Sets the listener that receives every read: what was read and how long it took in nanoseconds. {@code null} removes it.
     */
    public static void setListener(BiConsumer<String, Long> newListener) {
        listener = newListener != null ? newListener : NO_LISTENER;
    }

    /**
     * Records a read that started at {@code startNanos} ({@link System#nanoTime()}) and has just finished.
     */
    public static void record(String what, long startNanos) {
        long nanos = System.nanoTime() - startNanos;
        log.debug("Read {} in {}ms", what, nanos / 1_000_000);
        listener.accept(what, nanos);
    }
}
