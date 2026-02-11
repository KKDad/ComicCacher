package org.stapledon.api.resolver.dataloader;

import jakarta.annotation.PostConstruct;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Component;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.StripLoaderKey;
import org.stapledon.engine.management.ManagementFacade;

import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Registers DataLoaders for batching GraphQL operations.
 * Prevents N+1 query problems when fetching nested comic strip data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComicDataLoaderRegistrar {

    public static final String STRIP_LOADER = "stripLoader";

    private final ManagementFacade managementFacade;
    private final BatchLoaderRegistry registry;

    @PostConstruct
    public void registerLoaders() {
        // Register the strip batch loader
        registry.forTypePair(StripLoaderKey.class, ComicNavigationResult.class)
                .withName(STRIP_LOADER)
                .registerMappedBatchLoader((keys, env) -> loadStrips(keys));
    }

    /**
     * Batch loads comic strips for multiple keys.
     * Uses the true batch loading method from ManagementFacade for better performance.
     */
    private Mono<Map<StripLoaderKey, ComicNavigationResult>> loadStrips(Set<StripLoaderKey> keys) {
        log.debug("Batch loading {} comic strips", keys.size());
        return Mono.fromSupplier(() -> managementFacade.getComicStripsWithNavigation(keys));
    }
}
