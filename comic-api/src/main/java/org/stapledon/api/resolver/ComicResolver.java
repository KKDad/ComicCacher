package org.stapledon.api.resolver;

import org.dataloader.DataLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.StripLoaderKey;
import org.stapledon.common.dto.StripLoaderKey.DateStripKey;
import org.stapledon.common.dto.StripLoaderKey.BoundaryStripKey;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.model.ComicNotFoundException;
import org.stapledon.common.model.ComicOperationException;
import org.stapledon.api.dto.payload.MutationPayloads.CreateComicPayload;
import org.stapledon.api.dto.payload.MutationPayloads.DeleteComicPayload;
import org.stapledon.api.dto.payload.MutationPayloads.UpdateComicPayload;
import org.stapledon.engine.management.ManagementFacade;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * GraphQL resolver for Comic operations.
 * Implements queries and mutations defined in comics-schema.graphql.
 */
@Slf4j
@Controller
public class ComicResolver {

    private final ManagementFacade comicManagementFacade;
    private final String externalBaseUrl;

    /**
     * Constructs a ComicResolver with required dependencies.
     */
    public ComicResolver(ManagementFacade comicManagementFacade, @Value("${app.external-base-url:}") String externalBaseUrl) {
        this.comicManagementFacade = comicManagementFacade;
        this.externalBaseUrl = externalBaseUrl;
    }

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Get paginated list of comics with optional filtering.
     */
    @QueryMapping
    public ComicConnection comics(
            @Argument String search,
            @Argument Boolean active,
            @Argument Boolean enabled,
            @Argument Integer first,
            @Argument String after) {

        int limit = first != null ? Math.min(first, 50) : 20;

        List<ComicItem> allComics = comicManagementFacade.getAllComics();

        // Apply filters
        List<ComicItem> filtered = allComics.stream()
                .filter(c -> enabled == null || c.isEnabled() == enabled)
                .filter(c -> search == null || matchesSearch(c, search))
                .toList();

        // Apply cursor-based pagination
        int startIndex = 0;
        if (after != null) {
            int afterId = decodeCursor(after);
            for (int i = 0; i < filtered.size(); i++) {
                if (filtered.get(i).getId() == afterId) {
                    startIndex = i + 1;
                    break;
                }
            }
        }

        List<ComicItem> page = filtered.stream()
                .skip(startIndex)
                .limit(limit + 1L) // Fetch one extra to check hasNextPage
                .toList();

        boolean hasNextPage = page.size() > limit;
        if (hasNextPage) {
            page = page.subList(0, limit);
        }

        List<ComicEdge> edges = page.stream()
                .map(c -> new ComicEdge(c, encodeCursor(c.getId())))
                .toList();

        PageInfo pageInfo = new PageInfo(
                hasNextPage,
                startIndex > 0,
                edges.isEmpty() ? null : edges.getFirst().cursor(),
                edges.isEmpty() ? null : edges.getLast().cursor());

        return new ComicConnection(edges, pageInfo, filtered.size());
    }

    /**
     * Get a specific comic by ID.
     */
    @QueryMapping
    public ComicItem comic(@Argument int id) {
        return comicManagementFacade.getComic(id)
                .orElse(null);
    }

    /**
     * Get a comic strip directly by comic ID and date.
     * More efficient than querying comic.strip when you only need the strip.
     */
    @QueryMapping
    public CompletableFuture<ComicStrip> strip(
            @Argument int comicId,
            @Argument LocalDate date,
            DataLoader<StripLoaderKey, ComicNavigationResult> stripLoader) {

        return comicManagementFacade.getComic(comicId)
                .map(comic -> {
                    StripLoaderKey key = new DateStripKey(comicId, comic.getName(), date);
                    return stripLoader.load(key)
                            .thenApply(result -> toComicStrip(comicId, result));
                })
                .orElse(CompletableFuture.completedFuture(null));
    }

    /**
     * Resolve strip field for Comic type - get strip for specific date.
     * Uses DataLoader to batch multiple strip requests and prevent N+1 queries.
     */
    @SchemaMapping(typeName = "Comic", field = "strip")
    public CompletableFuture<ComicStrip> strip(
            ComicItem comic,
            @Argument LocalDate date,
            DataLoader<StripLoaderKey, ComicNavigationResult> stripLoader) {

        LocalDate targetDate = date != null ? date : comic.getNewest();
        if (targetDate == null) {
            return CompletableFuture.completedFuture(null);
        }

        StripLoaderKey key = new DateStripKey(
                comic.getId(),
                comic.getName(),
                targetDate);

        return stripLoader.load(key)
                .thenApply(result -> toComicStrip(comic.getId(), result));
    }

    /**
     * Search comics by query string.
     */
    @QueryMapping
    public SearchResults search(@Argument String query, @Argument Integer limit) {
        int maxResults = limit != null ? Math.min(limit, 50) : 20;

        List<ComicItem> matched = comicManagementFacade.getAllComics().stream()
                .filter(c -> matchesSearch(c, query))
                .limit(maxResults)
                .toList();

        return new SearchResults(matched, matched.size(), query);
    }

    // =========================================================================
    // SchemaMapping for Comic fields
    // =========================================================================

    /**
     * Resolves oldest field for Comic type from the authoritative index.
     */
    @SchemaMapping(typeName = "Comic", field = "oldest")
    public LocalDate oldest(ComicItem comic) {
        return comicManagementFacade.getOldestDateWithComic(comic.getId())
                .orElse(comic.getOldest());
    }

    /**
     * Resolves newest field for Comic type from the authoritative index.
     */
    @SchemaMapping(typeName = "Comic", field = "newest")
    public LocalDate newest(ComicItem comic) {
        return comicManagementFacade.getNewestDateWithComic(comic.getId())
                .orElse(comic.getNewest());
    }

    /**
     * Resolve avatarUrl field for Comic type.
     */
    @SchemaMapping(typeName = "Comic", field = "avatarUrl")
    public String avatarUrl(ComicItem comic) {
        return comic.isAvatarAvailable()
                ? externalBaseUrl + "/api/v1/comics/" + comic.getId() + "/avatar"
                : null;
    }

    /**
     * Resolve firstStrip field for Comic type.
     * Uses DataLoader for efficient batching when multiple comics are queried.
     */
    @SchemaMapping(typeName = "Comic", field = "firstStrip")
    public CompletableFuture<ComicStrip> firstStrip(
            ComicItem comic,
            DataLoader<StripLoaderKey, ComicNavigationResult> stripLoader) {
        StripLoaderKey key = BoundaryStripKey.first(comic.getId(), comic.getName());
        return stripLoader.load(key)
                .thenApply(result -> toComicStrip(comic.getId(), result));
    }

    /**
     * Resolve lastStrip field for Comic type.
     * Uses DataLoader for efficient batching when multiple comics are queried.
     */
    @SchemaMapping(typeName = "Comic", field = "lastStrip")
    public CompletableFuture<ComicStrip> lastStrip(
            ComicItem comic,
            DataLoader<StripLoaderKey, ComicNavigationResult> stripLoader) {
        StripLoaderKey key = BoundaryStripKey.last(comic.getId(), comic.getName());
        return stripLoader.load(key)
                .thenApply(result -> toComicStrip(comic.getId(), result));
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Create a new comic.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CreateComicPayload createComic(@Argument CreateComicInput input) {
        ComicItem newComic = ComicItem.builder()
                .name(input.name())
                .author(input.author())
                .description(input.description())
                .enabled(Optional.ofNullable(input.enabled()).orElse(true))
                .source(input.source())
                .sourceIdentifier(input.sourceIdentifier())
                .build();

        ComicItem created = comicManagementFacade.createComic(newComic)
                .orElseThrow(ComicOperationException::createFailed);
        return new CreateComicPayload(created, List.of());
    }

    /**
     * Update an existing comic.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UpdateComicPayload updateComic(@Argument int id, @Argument UpdateComicInput input) {
        ComicItem existing = comicManagementFacade.getComic(id)
                .orElseThrow(() -> new ComicNotFoundException(id));

        ComicItem.ComicItemBuilder builder = existing.toBuilder().id(id);
        Optional.ofNullable(input.name()).ifPresent(builder::name);
        Optional.ofNullable(input.author()).ifPresent(builder::author);
        Optional.ofNullable(input.description()).ifPresent(builder::description);
        Optional.ofNullable(input.enabled()).ifPresent(builder::enabled);
        Optional.ofNullable(input.source()).ifPresent(builder::source);
        Optional.ofNullable(input.sourceIdentifier()).ifPresent(builder::sourceIdentifier);
        ComicItem updated = builder.build();

        ComicItem result = comicManagementFacade.updateComic(id, updated)
                .orElseThrow(() -> ComicOperationException.updateFailed(id));
        return new UpdateComicPayload(result, List.of());
    }

    /**
     * Delete a comic.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DeleteComicPayload deleteComic(@Argument int id) {
        boolean deleted = comicManagementFacade.deleteComic(id);
        return new DeleteComicPayload(deleted, List.of());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private boolean matchesSearch(ComicItem comic, String query) {
        String lowerQuery = query.toLowerCase();
        return Optional.ofNullable(comic.getName())
                       .map(String::toLowerCase)
                       .map(s -> s.contains(lowerQuery))
                       .orElse(false)
            || Optional.ofNullable(comic.getAuthor())
                       .map(String::toLowerCase)
                       .map(s -> s.contains(lowerQuery))
                       .orElse(false)
            || Optional.ofNullable(comic.getDescription())
                       .map(String::toLowerCase)
                       .map(s -> s.contains(lowerQuery))
                       .orElse(false);
    }

    private String encodeCursor(int id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(("comic:" + id).getBytes());
    }

    private int decodeCursor(String cursor) {
        String decoded = new String(Base64.getUrlDecoder().decode(cursor));
        return Integer.parseInt(decoded.replace("comic:", ""));
    }

    private ComicStrip toComicStrip(int comicId, ComicNavigationResult result) {
        if (!result.isFound()) {
            // Return navigation hints even when strip not found
            return new ComicStrip(
                    result.getCurrentDate(),
                    false,
                    null,
                    ComicStrip.navStub(result.getNearestPreviousDate()),
                    ComicStrip.navStub(result.getNearestNextDate()));
        }
        String imageUrl = result.getCurrentDate() != null
                ? externalBaseUrl + "/api/v1/comics/" + comicId + "/strip/" + result.getCurrentDate()
                : null;

        return new ComicStrip(
                result.getCurrentDate(),
                result.isFound(),
                imageUrl,
                ComicStrip.navStub(result.getNearestPreviousDate()),
                ComicStrip.navStub(result.getNearestNextDate()));
    }

    // =========================================================================
    // Record Types for GraphQL
    // =========================================================================

    public record ComicConnection(List<ComicEdge> edges, PageInfo pageInfo, int totalCount) {
    }

    public record ComicEdge(ComicItem node, String cursor) {
    }

    public record PageInfo(boolean hasNextPage, boolean hasPreviousPage, String startCursor, String endCursor) {
    }

    public record ComicStrip(LocalDate date, boolean available, String imageUrl, ComicStrip previous,
            ComicStrip next) {

        /** Navigation-only stub with just a date (used for previous/next links). */
        static ComicStrip navStub(LocalDate date) {
            return date != null ? new ComicStrip(date, false, null, null, null) : null;
        }
    }

    public record SearchResults(List<ComicItem> comics, int totalCount, String query) {
    }

    public record CreateComicInput(String name, String author, String description, Boolean enabled, String source,
            String sourceIdentifier) {
    }

    public record UpdateComicInput(String name, String author, String description, Boolean enabled, String source,
            String sourceIdentifier) {
    }
}
