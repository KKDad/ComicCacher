package org.stapledon.common.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utility class for NFS-safe file operations.
 * Provides atomic write operations using the "write-to-temp-then-move" pattern
 * to prevent corruption during network hiccups on NFS filesystems.
 */
public final class NfsFileOperations {

    private NfsFileOperations() {
        // Utility class - prevent instantiation
    }

    /**
     * Atomically write content to a file using the write-to-temp-then-move pattern.
     * This is safe for NFS where network hiccups could corrupt direct writes.
     * Uses UTF-8 charset by default.
     */
    public static void atomicWrite(Path target, String content) throws IOException {
        atomicWrite(target, content, StandardCharsets.UTF_8);
    }

    /**
     * Atomically write content to a file using the write-to-temp-then-move pattern.
     * This is safe for NFS where network hiccups could corrupt direct writes.
     */
    public static void atomicWrite(Path target, String content, Charset charset) throws IOException {
        // Ensure parent directory exists
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // Write to temp file first
        Path tempFile = target
                .resolveSibling(target.getFileName() + ".tmp." + System.nanoTime());
        try {
            Files.writeString(tempFile, content, charset);
            // Atomic move to target location
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Clean up temp file if move failed
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // Best effort cleanup
            }
            throw e;
        }
    }

    /**
     * Read file contents as a string using UTF-8 charset.
     */
    public static String readAsString(Path file) throws IOException {
        return readAsString(file, StandardCharsets.UTF_8);
    }

    /**
     * Read file contents as a string.
     */
    public static String readAsString(Path file, Charset charset) throws IOException {
        return Files.readString(file, charset);
    }

    /**
     * Check if a file exists.
     */
    public static boolean exists(Path file) {
        return Files.exists(file);
    }

    /**
     * Resolve a path from base and additional path parts.
     */
    public static Path resolvePath(String base, String... parts) {
        Path path = Path.of(base);
        for (String part : parts) {
            path = path.resolve(part);
        }
        return path;
    }
}
