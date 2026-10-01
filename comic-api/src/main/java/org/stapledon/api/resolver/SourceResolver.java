package org.stapledon.api.resolver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.stapledon.api.dto.ErrorCode;
import org.stapledon.api.dto.payload.MutationPayloads.CreateComicPayload;
import org.stapledon.api.dto.payload.MutationPayloads.QueueComicTaskPayload;
import org.stapledon.api.dto.payload.MutationPayloads.RequestCatalogThumbnailsPayload;
import org.stapledon.api.dto.payload.MutationPayloads.TriggerBatchJobPayload;
import org.stapledon.api.dto.payload.UserError;
import org.stapledon.api.resolver.ComicResolver.PageInfo;
import org.stapledon.common.config.properties.DownloaderProperties;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.engine.batch.BackfillConfigurationService;
import org.stapledon.engine.batch.config.SourceCatalogJobConfig;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.engine.source.CatalogThumbnailService;
import org.stapledon.engine.source.CatalogThumbnailService.RequestOutcome;
import org.stapledon.engine.source.ComicSource;
import org.stapledon.engine.source.SourceCatalogService;
import org.stapledon.engine.source.SourceCatalogService.AddResult;
import org.stapledon.engine.source.SourceCatalogService.CatalogRow;
import org.stapledon.engine.source.SourceCatalogService.SourceSummary;
import org.stapledon.engine.source.SourceCatalogState.Entry;
import org.stapledon.engine.source.SourceRegistry;
import org.stapledon.engine.source.StartInfo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

/**
 * GraphQL resolver for the Sources page: each source's summary, settings and catalog, and the actions on it. Operators can read; only admins change
 * anything.
 */
@Slf4j
@Controller
public class SourceResolver {

    private static final int MAX_CATALOG_PAGE = 500;
    private static final int MAX_THUMBNAIL_REQUESTS = 100;

    private final SourceRegistry sources;
    private final SourceCatalogService catalogService;
    private final CatalogThumbnailService thumbnails;
    private final ManagementFacade comics;
    private final BatchJobResolver batchJobResolver;
    private final DownloaderProperties downloaderProperties;
    private final UserAgentService userAgentService;
    private final BackfillConfigurationService backfillConfig;
    private final String externalBaseUrl;

    /**
     * Constructs the resolver.
     */
    public SourceResolver(SourceRegistry sources, SourceCatalogService catalogService, CatalogThumbnailService thumbnails, ManagementFacade comics,
            BatchJobResolver batchJobResolver, DownloaderProperties downloaderProperties, UserAgentService userAgentService,
            BackfillConfigurationService backfillConfig, @Value("${app.external-base-url:}") String externalBaseUrl) {
        this.sources = sources;
        this.catalogService = catalogService;
        this.thumbnails = thumbnails;
        this.comics = comics;
        this.batchJobResolver = batchJobResolver;
        this.downloaderProperties = downloaderProperties;
        this.userAgentService = userAgentService;
        this.backfillConfig = backfillConfig;
        this.externalBaseUrl = externalBaseUrl;
    }

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Every source.
     */
    @QueryMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public List<SourceView> sources() {
        return catalogService.summaries().stream().map(this::toView).toList();
    }

    /**
     * One source.
     */
    @QueryMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public SourceView source(@Argument String id) {
        return catalogService.summary(id).map(this::toView).orElse(null);
    }

    /**
     * A source's read-only settings.
     */
    @SchemaMapping(typeName = "Source", field = "settings")
    public SourceSettings settings(SourceView source) {
        DownloaderProperties.Throttle throttle = downloaderProperties.throttleFor(source.id());
        DownloaderProperties.Retry retry = downloaderProperties.retryFor(source.id());
        return new SourceSettings(userAgentService.getUserAgent(source.id()),
                (int) throttle.getMinDelayMs(), (int) throttle.getMaxDelayMs(),
                retry.getMaxAttempts(), (int) retry.getInitialBackoffMs(), (int) retry.getMaxBackoffMs(),
                backfillConfig.isSourceEnabled(source.id()), backfillConfig.getMaxDaysBackForSource(source.id()),
                backfillConfig.getMaxPerRunForSource(source.id()), backfillConfig.getMaxPerDayForSource(source.id()),
                backfillConfig.getRecentDaysForSource(source.id()), backfillConfig.getPreferColorForSource(source.id()));
    }

    /**
     * A page of a source's catalog.
     */
    @SchemaMapping(typeName = "Source", field = "catalog")
    public SourceCatalogConnection catalog(SourceView source, @Argument String search, @Argument CatalogEntryFilter filter, @Argument String tag,
            @Argument Integer first, @Argument String after) {
        Optional<ComicSource> comicSource = sources.find(source.id());
        if (comicSource.isEmpty()) {
            return new SourceCatalogConnection(List.of(), new PageInfo(false, false, null, null), 0);
        }
        CatalogEntryFilter effectiveFilter = filter != null ? filter : CatalogEntryFilter.ALL;
        String query = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
        List<CatalogRow> rows = catalogService.catalog(source.id()).stream()
                .filter(row -> effectiveFilter.matches(row))
                .filter(row -> query == null || matches(row, query))
                .filter(row -> tag == null || tag.isBlank() || tagsOf(row.entry()).contains(tag))
                .toList();

        int limit = first != null ? Math.clamp(first, 1, MAX_CATALOG_PAGE) : 200;
        int start = 0;
        if (after != null) {
            String afterId = decodeCursor(after);
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).identifier().equals(afterId)) {
                    start = i + 1;
                    break;
                }
            }
        }
        List<CatalogRow> page = rows.subList(Math.min(start, rows.size()), Math.min(start + limit, rows.size()));
        List<SourceCatalogEdge> edges = page.stream()
                .map(row -> new SourceCatalogEdge(toEntry(comicSource.get(), row), encodeCursor(row.identifier())))
                .toList();
        boolean hasNext = start + limit < rows.size();
        PageInfo pageInfo = new PageInfo(hasNext, start > 0, edges.isEmpty() ? null : edges.getFirst().cursor(), edges.isEmpty() ? null : edges.getLast().cursor());
        return new SourceCatalogConnection(edges, pageInfo, rows.size());
    }

    /**
     * Every tag in a source's catalog, by name.
     */
    @SchemaMapping(typeName = "Source", field = "tags")
    public List<String> tags(SourceView source) {
        return catalogService.catalog(source.id()).stream()
                .flatMap(row -> tagsOf(row.entry()).stream())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /**
     * Configured comics the source's catalog doesn't list.
     */
    @SchemaMapping(typeName = "Source", field = "orphans")
    public List<ComicItem> orphans(SourceView source) {
        return catalogService.orphans(source.id());
    }

    /**
     * Whether an avatar download is queued or running for the comic.
     */
    @SchemaMapping(typeName = "Comic", field = "avatarPending")
    public boolean avatarPending(ComicItem comic) {
        return catalogService.isAvatarPending(comic.getId());
    }

    /**
     * Whether start detection is queued or running for the comic.
     */
    @SchemaMapping(typeName = "Comic", field = "startPending")
    public boolean startPending(ComicItem comic) {
        return catalogService.isStartPending(comic.getId());
    }

    /**
     * The first strip date the comic's source reports.
     */
    @SchemaMapping(typeName = "Comic", field = "reportedStartDate")
    public LocalDate reportedStartDate(ComicItem comic) {
        return catalogService.reportedStart(comic).map(StartInfo::date).orElse(null);
    }

    /**
     * The first strip number the comic's source reports.
     */
    @SchemaMapping(typeName = "Comic", field = "reportedStartStripNumber")
    public Integer reportedStartStripNumber(ComicItem comic) {
        return catalogService.reportedStart(comic).map(StartInfo::stripNumber).orElse(null);
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Reads a source's catalog now, by running the catalog job for it.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TriggerBatchJobPayload refreshSourceCatalog(@Argument String source) {
        Optional<ComicSource> found = sources.find(source);
        if (found.isEmpty() || found.get().catalog().isEmpty()) {
            return new TriggerBatchJobPayload(null, List.of(new UserError("No catalog for source \"" + source + "\"", "source", ErrorCode.VALIDATION_ERROR)));
        }
        if (catalogService.isRefreshing(source)) {
            return new TriggerBatchJobPayload(null, List.of(new UserError(found.get().displayName() + " is already being refreshed", "source", ErrorCode.VALIDATION_ERROR)));
        }
        log.info("AUDIT source catalog refresh requested: source={}", source);
        return batchJobResolver.triggerJob(SourceCatalogJobConfig.JOB_NAME, Map.of("source", source, "force", "true"));
    }

    /**
     * Configures a comic from its source's catalog.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CreateComicPayload addComicFromCatalog(@Argument AddComicFromCatalogInput input) {
        AddResult result = catalogService.addFromCatalog(input.source(), input.identifier(),
                input.active() == null || input.active(), input.enabled() == null || input.enabled());
        List<UserError> errors = result.problems().stream()
                .map(problem -> new UserError(problem.message(), "input." + problem.field(), ErrorCode.VALIDATION_ERROR))
                .toList();
        return new CreateComicPayload(result.comic(), errors);
    }

    /**
     * Queues a download of a comic's avatar.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public QueueComicTaskPayload fetchComicAvatar(@Argument int id) {
        Optional<ComicItem> comic = comics.getComic(id);
        if (comic.isEmpty()) {
            return new QueueComicTaskPayload(false, null, List.of(new UserError("Comic " + id + " not found", "id", ErrorCode.COMIC_NOT_FOUND)));
        }
        boolean queued = catalogService.requestAvatar(id);
        log.info("AUDIT comic avatar fetch requested: id={}, name={}, queued={}", id, comic.get().getName(), queued);
        return new QueueComicTaskPayload(queued, comic.get(), queued ? List.of() : List.of(new UserError("The work queue is full; try again shortly", "id", null)));
    }

    /**
     * Queues reading where a comic starts from its source.
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public QueueComicTaskPayload detectComicStart(@Argument int id) {
        Optional<ComicItem> comic = comics.getComic(id);
        if (comic.isEmpty()) {
            return new QueueComicTaskPayload(false, null, List.of(new UserError("Comic " + id + " not found", "id", ErrorCode.COMIC_NOT_FOUND)));
        }
        boolean queued = catalogService.requestStartDetection(id);
        log.info("AUDIT comic start detection requested: id={}, name={}, queued={}", id, comic.get().getName(), queued);
        return new QueueComicTaskPayload(queued, comic.get(),
                queued ? List.of() : List.of(new UserError("Its source can't report a start, or the work queue is full", "id", null)));
    }

    /**
     * Queues downloads of the catalog thumbnails someone is looking at.
     */
    @MutationMapping
    @PreAuthorize("hasRole('OPERATOR')")
    public RequestCatalogThumbnailsPayload requestCatalogThumbnails(@Argument String source, @Argument List<String> identifiers) {
        if (!sources.isKnown(source)) {
            return new RequestCatalogThumbnailsPayload(0, List.of(new UserError("Unknown source \"" + source + "\"", "source", ErrorCode.VALIDATION_ERROR)));
        }
        int queued = 0;
        for (String identifier : identifiers.stream().distinct().limit(MAX_THUMBNAIL_REQUESTS).toList()) {
            if (thumbnails.request(source, identifier) == RequestOutcome.QUEUED) {
                queued++;
            }
        }
        log.debug("Queued {} catalog thumbnails for {} ({} asked for)", queued, source, identifiers.size());
        return new RequestCatalogThumbnailsPayload(queued, List.of());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private SourceView toView(SourceSummary summary) {
        ComicSource source = summary.source();
        return new SourceView(source.id(), source.displayName(), source.indexed() ? SourceKind.INDEXED : SourceKind.DAILY, source.catalog().isPresent(),
                source.catalog().map(c -> c.catalogUrl()).orElse(null), source.startDetector().isPresent(),
                summary.catalogCount(), summary.configuredCount(), summary.activeCount(), summary.lastRefreshed(), summary.lastAttempt(), summary.lastError(),
                catalogService.isRefreshing(source.id()));
    }

    private SourceCatalogEntryView toEntry(ComicSource source, CatalogRow row) {
        Entry entry = row.entry();
        String thumbnailUrl = thumbnails.cached(source.id(), row.identifier()).isPresent()
                ? externalBaseUrl + "/api/v1/sources/" + source.id() + "/thumbnails/" + row.identifier()
                : null;
        return new SourceCatalogEntryView(row.identifier(), entry.getName(), entry.getAuthor(), entry.getDescription(), tagsOf(entry),
                source.comicPageUrl(row.identifier()), thumbnailUrl,
                thumbnails.isPending(source.id(), row.identifier()), entry.getStartDate(), entry.getStartStripNumber(), entry.getFirstSeen(), entry.getLastSeen(),
                entry.getRemovedAt(), row.comic());
    }

    private static boolean matches(CatalogRow row, String query) {
        return contains(row.entry().getName(), query) || contains(row.entry().getAuthor(), query) || row.identifier().contains(query)
                || contains(row.entry().getDescription(), query)
                || row.comic() != null && contains(row.comic().getName(), query);
    }

    private static List<String> tagsOf(Entry entry) {
        return entry.getTags() != null ? entry.getTags() : List.of();
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String encodeCursor(String identifier) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(("entry:" + identifier).getBytes());
    }

    private static String decodeCursor(String cursor) {
        return new String(Base64.getUrlDecoder().decode(cursor)).replace("entry:", "");
    }

    // =========================================================================
    // Record Types for GraphQL
    // =========================================================================

    /**
     * Which catalog entries to list.
     */
    public enum CatalogEntryFilter {
        ALL,
        CONFIGURED,
        NOT_CONFIGURED,
        REMOVED;

        boolean matches(CatalogRow row) {
            return switch (this) {
                case ALL -> true;
                case CONFIGURED -> row.comic() != null;
                case NOT_CONFIGURED -> row.comic() == null && row.entry().getRemovedAt() == null;
                case REMOVED -> row.entry().getRemovedAt() != null;
            };
        }
    }

    /**
     * How a source identifies strips.
     */
    public enum SourceKind {
        DAILY,
        INDEXED
    }

    public record SourceView(String id, String displayName, SourceKind kind, boolean hasCatalog, String catalogUrl, boolean canDetectStart, int catalogCount,
            int configuredCount, int activeCount, OffsetDateTime lastRefreshed, OffsetDateTime lastRefreshAttempt, String lastRefreshError, boolean refreshing) {
    }

    public record SourceSettings(String userAgent, int throttleMinDelayMs, int throttleMaxDelayMs, int retryMaxAttempts, int retryInitialBackoffMs,
            int retryMaxBackoffMs, boolean backfillEnabled, int backfillMaxDaysBack, int backfillMaxPerRun, int backfillMaxPerDay, int backfillRecentDays,
            boolean backfillPreferColor) {
    }

    public record SourceCatalogEntryView(String identifier, String name, String author, String description, List<String> tags, String pageUrl,
            String thumbnailUrl, boolean thumbnailPending, LocalDate startDate, Integer startStripNumber, OffsetDateTime firstSeen, OffsetDateTime lastSeen,
            OffsetDateTime removedAt, ComicItem comic) {
    }

    public record SourceCatalogConnection(List<SourceCatalogEdge> edges, PageInfo pageInfo, int totalCount) {
    }

    public record SourceCatalogEdge(SourceCatalogEntryView node, String cursor) {
    }

    public record AddComicFromCatalogInput(String source, String identifier, Boolean active, Boolean enabled) {
    }
}
