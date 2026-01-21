package org.stapledon.api.resolver;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.stapledon.api.resolver.dataloader.StripLoaderKey;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.model.ComicNotFoundException;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.management.ManagementFacade;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for Comic operations.
 * Implements queries and mutations defined in comics-schema.graphql.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ComicResolver {

    private final ManagementFacade comicManagementFacade;

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
                .collect(Collectors.toList());

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
                .limit(limit + 1) // Fetch one extra to check hasNextPage
                .collect(Collectors.toList());

        boolean hasNextPage = page.size() > limit;
        if (hasNextPage) {
            page = page.subList(0, limit);
        }

        List<ComicEdge> edges = page.stream()
                .map(c -> new ComicEdge(c, encodeCursor(c.getId())))
                .collect(Collectors.toList());

        PageInfo pageInfo = new PageInfo(
                hasNextPage,
                startIndex > 0,
                edges.isEmpty() ? null : edges.get(0).cursor(),
                edges.isEmpty() ? null : edges.get(edges.size() - 1).cursor());

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
     * Search comics by query string.
     */
    @QueryMapping
    public SearchResults search(@Argument String query, @Argument Integer limit) {
        int maxResults = limit != null ? Math.min(limit, 50) : 20;

        List<ComicItem> matched = comicManagementFacade.getAllComics().stream()
                .filter(c -> matchesSearch(c, query))
                .limit(maxResults)
                .collect(Collectors.toList());

        return new SearchResults(matched, matched.size(), query);
    }

    // =========================================================================
    // SchemaMapping for Comic fields
    // =========================================================================

    /**
     * Resolve avatarUrl field for Comic type.
     */
    @SchemaMapping(typeName = "Comic", field = "avatarUrl")
    public String avatarUrl(ComicItem comic) {
        return comic.isAvatarAvailable()
                ? "/api/v1/comics/" + comic.getId() + "/avatar"
                : null;
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

        StripLoaderKey key = new StripLoaderKey(
                comic.getId(),
                comic.getName(),
                targetDate);

        return stripLoader.load(key)
                .thenApply(result -> toComicStrip(comic.getId(), result));
    }

    /**
     * Resolve firstStrip field for Comic type.
     */
    @SchemaMapping(typeName = "Comic", field = "firstStrip")
    public ComicStrip firstStrip(ComicItem comic) {
        ComicNavigationResult result = comicManagementFacade.getComicStrip(
                comic.getId(), Direction.FORWARD);
        return toComicStrip(comic.getId(), result);
    }

    /**
     * Resolve lastStrip field for Comic type.
     */
    @SchemaMapping(typeName = "Comic", field = "lastStrip")
    public ComicStrip lastStrip(ComicItem comic) {
        ComicNavigationResult result = comicManagementFacade.getComicStrip(
                comic.getId(), Direction.BACKWARD);
        return toComicStrip(comic.getId(), result);
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Create a new comic.
     */
    @MutationMapping
    public ComicItem createComic(@Argument CreateComicInput input) {
        ComicItem newComic = ComicItem.builder()
                .name(input.name())
                .author(input.author())
                .description(input.description())
                .enabled(input.enabled() != null ? input.enabled() : true)
                .source(input.source())
                .sourceIdentifier(input.sourceIdentifier())
                .build();

        return comicManagementFacade.createComic(newComic)
                .orElseThrow(() -> new RuntimeException("Failed to create comic"));
    }

    /**
     * Update an existing comic.
     */
    @MutationMapping
    public ComicItem updateComic(@Argument int id, @Argument UpdateComicInput input) {
        ComicItem existing = comicManagementFacade.getComic(id)
                .orElseThrow(() -> new ComicNotFoundException(id));

        ComicItem updated = ComicItem.builder()
                .id(id)
                .name(input.name() != null ? input.name() : existing.getName())
                .author(input.author() != null ? input.author() : existing.getAuthor())
                .description(input.description() != null ? input.description() : existing.getDescription())
                .enabled(input.enabled() != null ? input.enabled() : existing.isEnabled())
                .source(input.source() != null ? input.source() : existing.getSource())
                .sourceIdentifier(
                        input.sourceIdentifier() != null ? input.sourceIdentifier() : existing.getSourceIdentifier())
                .oldest(existing.getOldest())
                .newest(existing.getNewest())
                .avatarAvailable(existing.isAvatarAvailable())
                .build();

        return comicManagementFacade.updateComic(id, updated)
                .orElseThrow(() -> new RuntimeException("Failed to update comic"));
    }

    /**
     * Delete a comic.
     */
    @MutationMapping
    public boolean deleteComic(@Argument int id) {
        return comicManagementFacade.deleteComic(id);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private boolean matchesSearch(ComicItem comic, String query) {
        String lowerQuery = query.toLowerCase();
        return (comic.getName() != null && comic.getName().toLowerCase().contains(lowerQuery))
                || (comic.getAuthor() != null && comic.getAuthor().toLowerCase().contains(lowerQuery))
                || (comic.getDescription() != null && comic.getDescription().toLowerCase().contains(lowerQuery));
    }

    private String encodeCursor(int id) {
        return Base64.getEncoder().encodeToString(("comic:" + id).getBytes());
    }

    private int decodeCursor(String cursor) {
        String decoded = new String(Base64.getDecoder().decode(cursor));
        return Integer.parseInt(decoded.replace("comic:", ""));
    }

    private ComicStrip toComicStrip(int comicId, ComicNavigationResult result) {
        if (!result.isFound()) {
            return null;
        }
        String imageUrl = result.getCurrentDate() != null
                ? "/api/v1/comics/" + comicId + "/strip/" + result.getCurrentDate()
                : null;

        return new ComicStrip(
                result.getCurrentDate(),
                result.isFound(),
                imageUrl,
                result.getNearestPreviousDate(),
                result.getNearestNextDate());
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

    public record ComicStrip(LocalDate date, boolean available, String imageUrl, LocalDate previousDate,
            LocalDate nextDate) {
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
