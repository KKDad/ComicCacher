package org.stapledon.engine.source;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.stapledon.common.config.CacheLayout;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.config.properties.DownloaderProperties;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.common.service.ValidationService;
import org.stapledon.common.util.LogContext;
import org.stapledon.common.util.NfsFileOperations;
import org.stapledon.engine.downloader.RateLimitedException;
import org.stapledon.engine.downloader.SourceThrottleService;
import org.stapledon.engine.source.SourceCatalogRepository.ThumbnailEvent;
import org.stapledon.engine.source.SourceCatalogState.Entry;
import org.stapledon.engine.source.SourceCatalogState.SourceEntries;

/**
 * Catalog thumbnails for comics that aren't configured yet. {@code SourceCatalogJob} downloads due ones in the background ({@link #prefetchDue}), and
 * {@link #request} queues one on the {@code catalogTaskExecutor} when the Sources page shows a row that has none yet. Files live in a temporary folder
 * ({@code {cache}/tmp/catalog-thumbnails/{source}/{identifier}.{ext}}), which storage metrics skip and which is always safe to delete. They are kept
 * for about a year ({@link #purge}); the catalog records which are saved, so the job never scans the disk to find work.
 * <p>
 * Images come only over https from the source's {@link ComicSource#imageHosts() image hosts}, redirects included, are capped at 8 MB, must pass
 * image validation, and are shrunk to {@value #MAX_WIDTH} px wide. Requests are paced by {@link SourceThrottleService} under {@code <source>-assets}
 * when that is configured (the image hosts are CDNs that need less care than the pages), otherwise under the source itself. A 429 backs that key off
 * once and drops the download, and a background run stops asking that source.
 */
@Slf4j
@Service
public class CatalogThumbnailService {

    static final String THUMBNAIL_DIRECTORY = "catalog-thumbnails";
    /** Some Comics Kingdom feature images are 3–5 MB; they are shrunk to {@link #MAX_WIDTH} before saving. */
    static final int MAX_BYTES = 8 * 1024 * 1024;
    static final int MAX_WIDTH = 400;
    static final int PURGE_JITTER_DAYS = 90;
    /** How long the job waits before trying a failed thumbnail again. */
    static final Duration FAILURE_RETRY = Duration.ofDays(7);
    private static final int MAX_REDIRECTS = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int SUMMARY_EVERY = 50;
    private static final List<String> EXTENSIONS = List.of("png", "jpg", "gif", "webp", "bmp", "tiff");

    private final SourceRegistry sources;
    private final SourceCatalogRepository catalog;
    private final SourceThrottleService throttle;
    private final DownloaderProperties downloaderProperties;
    private final UserAgentService userAgentService;
    private final ValidationService validationService;
    private final Executor executor;
    private final Path root;
    private final Duration failureMemo;
    private final HttpClient http;
    private final boolean requireHttps;
    private final Clock clock;

    private final Set<String> queued = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> failedAt = new ConcurrentHashMap<>();
    private final AtomicInteger downloaded = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();

    /**
     * What happened to a thumbnail request.
     */
    public enum RequestOutcome {
        /** The queue is full; ask again later. */
        BUSY,
        /** Already on disk. */
        CACHED,
        /** Failed recently; not retried until the failure memo expires. */
        FAILED_RECENTLY,
        /** Already queued. */
        PENDING,
        /** Queued for download now. */
        QUEUED,
        /** No such source or catalog entry, or a malformed identifier. */
        UNKNOWN
    }

    /**
     * A cached thumbnail's bytes and media type.
     */
    public record Thumbnail(byte[] data, String mediaType) {
    }

    @Autowired
    public CatalogThumbnailService(SourceRegistry sources, SourceCatalogRepository catalog, SourceThrottleService throttle,
            DownloaderProperties downloaderProperties, UserAgentService userAgentService, ValidationService validationService,
            @Qualifier("catalogTaskExecutor") Executor executor, CacheProperties cacheProperties,
            @Value("${comics.catalog.thumbnail-dir:}") String thumbnailDir,
            @Value("${comics.catalog.thumbnail-failure-memo-hours:0}") long failureMemoHours) {
        this(sources, catalog, throttle, downloaderProperties, userAgentService, validationService, executor,
                thumbnailDir.isBlank() ? Path.of(cacheProperties.getLocation(), CacheLayout.TMP_DIRECTORY, THUMBNAIL_DIRECTORY) : Path.of(thumbnailDir),
                Duration.ofHours(Math.max(0, failureMemoHours)),
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(TIMEOUT).build(), true, Clock.systemUTC());
    }

    CatalogThumbnailService(SourceRegistry sources, SourceCatalogRepository catalog, SourceThrottleService throttle,
            DownloaderProperties downloaderProperties, UserAgentService userAgentService, ValidationService validationService, Executor executor,
            Path root, Duration failureMemo, HttpClient http, boolean requireHttps, Clock clock) {
        this.sources = sources;
        this.catalog = catalog;
        this.throttle = throttle;
        this.downloaderProperties = downloaderProperties;
        this.userAgentService = userAgentService;
        this.validationService = validationService;
        this.executor = executor;
        this.root = root;
        this.failureMemo = failureMemo;
        this.http = http;
        this.requireHttps = requireHttps;
        this.clock = clock;
    }

    /**
     * The cached thumbnail file, if it has been downloaded. Reads only the disk.
     */
    public Optional<Path> cached(String source, String identifier) {
        if (!sources.isKnown(source) || !ComicSource.isValidIdentifier(identifier)) {
            return Optional.empty();
        }
        Path dir = root.resolve(source);
        return EXTENSIONS.stream()
                .map(ext -> dir.resolve(identifier + "." + ext))
                .filter(Files::isRegularFile)
                .findFirst();
    }

    /**
     * The cached thumbnail's bytes and media type, if it has been downloaded.
     */
    public Optional<Thumbnail> load(String source, String identifier) {
        return cached(source, identifier).flatMap(path -> {
            try {
                return Optional.of(new Thumbnail(Files.readAllBytes(path), mediaType(path)));
            } catch (IOException e) {
                log.warn("Could not read catalog thumbnail {}: {}", path, e.toString());
                return Optional.empty();
            }
        });
    }

    /**
     * True while a download for this thumbnail is queued or running.
     */
    public boolean isPending(String source, String identifier) {
        return queued.contains(key(source, identifier));
    }

    /**
     * Queues a download unless the thumbnail is cached, already queued, or failed recently.
     */
    public RequestOutcome request(String source, String identifier) {
        if (!sources.isKnown(source) || !ComicSource.isValidIdentifier(identifier)) {
            return RequestOutcome.UNKNOWN;
        }
        if (cached(source, identifier).isPresent()) {
            return RequestOutcome.CACHED;
        }
        String key = key(source, identifier);
        Instant failure = failedAt.get(key);
        if (failure != null && failure.plus(failureMemo).isAfter(clock.instant())) {
            return RequestOutcome.FAILED_RECENTLY;
        }
        if (!queued.add(key)) {
            return RequestOutcome.PENDING;
        }
        try {
            executor.execute(() -> {
                try {
                    download(source, identifier);
                } finally {
                    queued.remove(key);
                }
            });
            return RequestOutcome.QUEUED;
        } catch (TaskRejectedException e) {
            queued.remove(key);
            log.debug("Catalog work queue is full; dropped thumbnail {}", key);
            return RequestOutcome.BUSY;
        }
    }

    /**
     * How a single thumbnail download went.
     */
    public enum DownloadOutcome {
        SAVED,
        FAILED,
        /** HTTP 429: the source (or its image host) was backed off; stop asking it for now. */
        RATE_LIMITED
    }

    /**
     * Downloads one thumbnail now, on the calling thread.
     */
    DownloadOutcome download(String source, String identifier) {
        String key = key(source, identifier);
        try (var _ = MDC.putCloseable(LogContext.COMIC, key)) {
            Optional<ComicSource> comicSource = sources.find(source);
            Optional<Entry> entry = catalog.find(source).map(e -> e.getEntries().get(identifier));
            if (comicSource.isEmpty() || entry.isEmpty()) {
                return DownloadOutcome.FAILED;
            }
            Optional<byte[]> image;
            try {
                image = entry.get().getThumbnailUrl() != null
                        ? fetchThumbnail(comicSource.get(), entry.get().getThumbnailUrl())
                        : comicSource.get().downloader().downloadAvatar(0, entry.get().getName(), identifier);
            } catch (RateLimitedException e) {
                recordFailure(key);
                return DownloadOutcome.RATE_LIMITED;
            }
            if (image.isEmpty()) {
                return failed(source, identifier);
            }
            Optional<String> extension = extension(image.get());
            if (extension.isEmpty()) {
                log.warn("Catalog thumbnail for {} failed image validation", key);
                return failed(source, identifier);
            }
            byte[] data = image.get();
            String ext = extension.get();
            Optional<byte[]> smaller = ImageScaler.shrinkToWidth(data, MAX_WIDTH);
            if (smaller.isPresent()) {
                data = smaller.get();
                ext = "png";
            }
            deleteCached(source, identifier);
            Path file = root.resolve(source).resolve(identifier + "." + ext);
            Files.createDirectories(file.getParent());
            NfsFileOperations.atomicWrite(file, data);
            failedAt.remove(key);
            catalog.recordThumbnail(source, identifier, ThumbnailEvent.SAVED);
            log.debug("Saved catalog thumbnail {} ({} bytes)", file, data.length);
            countAndSummarise(true);
            return DownloadOutcome.SAVED;
        } catch (IOException e) {
            log.warn("Could not save catalog thumbnail {}: {}", key, e.toString());
            return failed(source, identifier);
        }
    }

    private DownloadOutcome failed(String source, String identifier) {
        recordFailure(key(source, identifier));
        catalog.recordThumbnail(source, identifier, ThumbnailEvent.FAILED);
        return DownloadOutcome.FAILED;
    }

    private void deleteCached(String source, String identifier) throws IOException {
        Optional<Path> existing = cached(source, identifier);
        if (existing.isPresent()) {
            Files.deleteIfExists(existing.get());
        }
    }

    /**
     * Downloads due thumbnails (see {@link SourceCatalogRepository#isThumbnailDue}) on the calling thread, at most {@code limit} per source. A thumbnail
     * already on disk is only recorded. An HTTP 429 stops that source's downloads for this run; the source has already been backed off.
     */
    public int prefetchDue(int limit) {
        int saved = 0;
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (ComicSource source : sources.all()) {
            Map<String, Entry> entries = catalog.find(source.id()).map(SourceEntries::getEntries).orElse(Map.of());
            int tried = 0;
            for (Map.Entry<String, Entry> e : entries.entrySet()) {
                if (tried >= limit) {
                    break;
                }
                if (!SourceCatalogRepository.isThumbnailDue(e.getValue(), now, FAILURE_RETRY) || queued.contains(key(source.id(), e.getKey()))) {
                    continue;
                }
                if (cached(source.id(), e.getKey()).isPresent()) {
                    catalog.recordThumbnail(source.id(), e.getKey(), ThumbnailEvent.SAVED);
                    continue;
                }
                tried++;
                DownloadOutcome outcome = download(source.id(), e.getKey());
                if (outcome == DownloadOutcome.SAVED) {
                    saved++;
                } else if (outcome == DownloadOutcome.RATE_LIMITED) {
                    log.warn("Thumbnail prefetch for {} stopped after HTTP 429; the rest wait for the next run", source.id());
                    break;
                }
            }
        }
        return saved;
    }

    /**
     * Deletes thumbnails older than {@code maxAge} plus up to {@value #PURGE_JITTER_DAYS} days (fixed per comic, so expiries spread out), and those of
     * comics their source no longer lists. A deleted thumbnail becomes due again. Returns how many were deleted.
     */
    public int purge(Duration maxAge) {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        Instant now = clock.instant();
        int deleted = 0;
        try (Stream<Path> files = Files.walk(root, 2)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String source = file.getParent().getFileName().toString();
                String name = file.getFileName().toString();
                String identifier = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
                if (isStale(file, source, identifier, now.minus(maxAge).minus(jitter(source, identifier)))) {
                    Files.deleteIfExists(file);
                    catalog.recordThumbnail(source, identifier, ThumbnailEvent.DELETED);
                    deleted++;
                }
            }
        } catch (IOException e) {
            log.warn("Could not purge catalog thumbnails under {}: {}", root, e.toString());
        }
        if (deleted > 0) {
            log.info("Deleted {} stale catalog thumbnails from {}", deleted, root);
        }
        return deleted;
    }

    /** Between 0 and {@value #PURGE_JITTER_DAYS} days, always the same for one comic. */
    static Duration jitter(String source, String identifier) {
        return Duration.ofDays(Math.floorMod(key(source, identifier).hashCode(), PURGE_JITTER_DAYS + 1));
    }

    private boolean isStale(Path file, String source, String identifier, Instant cutoff) throws IOException {
        FileTime modified = Files.getLastModifiedTime(file);
        if (modified.toInstant().isBefore(cutoff)) {
            return true;
        }
        return catalog.find(source)
                .map(entries -> entries.getEntries().get(identifier))
                .map(entry -> entry.getRemovedAt() != null)
                .orElse(true);
    }

    /**
     * Fetches a thumbnail. An HTTP 429 backs the throttle key off once and is rethrown, so no caller keeps asking.
     */
    private Optional<byte[]> fetchThumbnail(ComicSource source, String url) throws RateLimitedException {
        String throttleKey = downloaderProperties.isConfigured(source.id() + "-assets") ? source.id() + "-assets" : source.id();
        throttle.await(throttleKey);
        try {
            return Optional.of(fetchImage(url, source.imageHosts(), throttleKey));
        } catch (RateLimitedException e) {
            Duration backoff = throttle.backOff(throttleKey, 1, e.getRetryAfter());
            log.warn("Rate limited (HTTP 429) fetching catalog thumbnail {}; backing off {}s", url, backoff.toSeconds());
            throw e;
        } catch (IOException e) {
            log.warn("Could not download catalog thumbnail {}: {}", url, e.toString());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /**
     * GETs an image, following at most {@value #MAX_REDIRECTS} redirects, each of which must stay on an allowed host.
     */
    byte[] fetchImage(String url, Set<String> allowedHosts, String userAgentSource) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            checkAllowed(uri, allowedHosts);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(TIMEOUT)
                    .header("User-Agent", userAgentService.getUserAgent(userAgentSource))
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            try (InputStream body = response.body()) {
                if (status >= 300 && status < 400) {
                    Optional<String> location = response.headers().firstValue("Location");
                    if (location.isEmpty()) {
                        throw new IOException("HTTP " + status + " without a Location from " + uri);
                    }
                    uri = uri.resolve(location.get());
                    continue;
                }
                if (status == RateLimitedException.HTTP_TOO_MANY_REQUESTS) {
                    throw RateLimitedException.of(uri.toString(), response.headers().firstValue("Retry-After").orElse(null));
                }
                if (status < 200 || status >= 300) {
                    throw new IOException("HTTP " + status + " from " + uri);
                }
                byte[] data = body.readNBytes(MAX_BYTES + 1);
                if (data.length > MAX_BYTES) {
                    throw new IOException("Image larger than " + MAX_BYTES + " bytes at " + uri);
                }
                return data;
            }
        }
        throw new IOException("Too many redirects from " + url);
    }

    private void checkAllowed(URI uri, Set<String> allowedHosts) throws IOException {
        String scheme = Optional.ofNullable(uri.getScheme()).orElse("").toLowerCase(Locale.ROOT);
        boolean schemeOk = "https".equals(scheme) || !requireHttps && "http".equals(scheme);
        String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);
        if (!schemeOk || !allowedHosts.contains(host)) {
            throw new IOException("Refusing to fetch " + uri + ": not an allowed image host");
        }
    }

    private Optional<String> extension(byte[] data) {
        ImageValidationResult validation = validationService.validate(data);
        if (!validation.isValid() || validation.getFormat() == null) {
            return Optional.empty();
        }
        return switch (validation.getFormat()) {
            case PNG -> Optional.of("png");
            case JPEG -> Optional.of("jpg");
            case GIF -> Optional.of("gif");
            case WEBP -> Optional.of("webp");
            case BMP -> Optional.of("bmp");
            case TIFF -> Optional.of("tiff");
            default -> Optional.empty();
        };
    }

    private static String mediaType(Path file) {
        String name = file.getFileName().toString();
        String ext = name.substring(name.lastIndexOf('.') + 1);
        return switch (ext) {
            case "jpg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "tiff" -> "image/tiff";
            default -> "image/png";
        };
    }

    private void recordFailure(String key) {
        if (!failureMemo.isZero()) {
            failedAt.put(key, clock.instant());
        }
        countAndSummarise(false);
    }

    private void countAndSummarise(boolean success) {
        int ok = success ? downloaded.incrementAndGet() : downloaded.get();
        int bad = success ? failed.get() : failed.incrementAndGet();
        if ((ok + bad) % SUMMARY_EVERY == 0) {
            log.info("Catalog thumbnails: {} downloaded, {} failed since startup ({} queued)", ok, bad, Math.max(0, queued.size() - 1));
        }
    }

    private static String key(String source, String identifier) {
        return source + "/" + identifier;
    }
}
