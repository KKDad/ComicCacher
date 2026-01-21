package org.stapledon.api.resolver.dataloader;

import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Component;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.management.ManagementFacade;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Registers DataLoaders for batching GraphQL operations.
 * Prevents N+1 query problems when fetching nested comic strip data.
 */
@Slf4j
@Component
public class ComicDataLoaderRegistrar {

    public static final String STRIP_LOADER = "stripLoader";

    private final ManagementFacade managementFacade;

    public ComicDataLoaderRegistrar(ManagementFacade managementFacade, BatchLoaderRegistry registry) {
        this.managementFacade = managementFacade;

        // Register the strip batch loader
        registry.forTypePair(StripLoaderKey.class, ComicNavigationResult.class)
                .withName(STRIP_LOADER)
                .registerMappedBatchLoader((keys, env) -> loadStrips(keys));
    }

    /**
     * Batch loads comic strips for multiple keys.
     * This batches NFS I/O operations instead of making individual calls per comic.
     */
    private Mono<Map<StripLoaderKey, ComicNavigationResult>> loadStrips(Set<StripLoaderKey> keys) {
        log.debug("Batch loading {} comic strips", keys.size());

        return Mono.fromSupplier(() -> keys.stream()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> managementFacade.getComicStrip(
                                key.comicId(),
                                Direction.FORWARD,
                                key.date()))));
    }
}
