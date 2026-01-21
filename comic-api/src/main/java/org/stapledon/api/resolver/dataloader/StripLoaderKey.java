package org.stapledon.api.resolver.dataloader;

import java.time.LocalDate;

/**
 * Key for batching strip lookups via DataLoader.
 * Enables the DataLoader to batch multiple strip requests and execute them
 * efficiently.
 */
public record StripLoaderKey(int comicId, String comicName, LocalDate date) {
}
