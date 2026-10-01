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
import org.stapledon.engine.source.SourceCatalogState.Entry;

/**
 * Catalog thumbnails for comics that aren't configured yet. Nothing is fetched until someone looks: {@link #request} queues a download on the
 * {@code catalogTaskExecutor}, and the Sources page shows initials until the file exists. Files live in a temporary folder
 * ({@code {cache}/tmp/catalog-thumbnails/{source}/{identifier}.{ext}}), which storage metrics skip and which is always safe to delete.
 * <p>
 * Images come only over https from the source's {@link ComicSource#imageHosts() image hosts}, redirects included, are capped at 2 MB, and must pass
 * image validation. Requests are paced by {@link SourceThrottleService} under {@code <source>-assets} when that is configured (the image hosts are
 * CDNs that need less care than the pages), otherwise under the source itself. A 429 backs the source off and drops the download; the page asks again.
 */
@Slf4j
@Service
public class CatalogThumbnailService {

    static final String THUMBNAIL_DIRECTORY = "catalog-thumbnails";
    static final int MAX_BYTES = 2 * 1024 * 1024;
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
     * Downloads one thumbnail now, on the calling thread. Returns true when the file was written.
     */
    boolean download(String source, String identifier) {
        String key = key(source, identifier);
        try (var _ = MDC.putCloseable(LogContext.COMIC, key)) {
            Optional<ComicSource> comicSource = sources.find(source);
            Optional<Entry> entry = catalog.find(source).map(e -> e.getEntries().get(identifier));
            if (comicSource.isEmpty() || entry.isEmpty()) {
                return false;
            }
            Optional<byte[]> image = entry.get().getThumbnailUrl() != null
                    ? fetchThumbnail(comicSource.get(), entry.get().getThumbnailUrl())
                    : comicSource.get().downloader().downloadAvatar(0, entry.get().getName(), identifier);
            if (image.isEmpty()) {
                recordFailure(key);
                return false;
            }
            Optional<String> extension = extension(image.get());
            if (extension.isEmpty()) {
                log.warn("Catalog thumbnail for {} failed image validation", key);
                recordFailure(key);
                return false;
            }
            Path file = root.resolve(source).resolve(identifier + "." + extension.get());
            Files.createDirectories(file.getParent());
            NfsFileOperations.atomicWrite(file, image.get());
            failedAt.remove(key);
            log.debug("Saved catalog thumbnail {} ({} bytes)", file, image.get().length);
            countAndSummarise(true);
            return true;
        } catch (IOException e) {
            log.warn("Could not save catalog thumbnail {}: {}", key, e.toString());
            recordFailure(key);
            return false;
        }
    }

    /**
     * Deletes thumbnails older than {@code maxAge}, and those of comics their source no longer lists. Returns how many were deleted.
     */
    public int purge(Duration maxAge) {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        Instant cutoff = clock.instant().minus(maxAge);
        int deleted = 0;
        try (Stream<Path> files = Files.walk(root, 2)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                if (isStale(file, cutoff)) {
                    Files.deleteIfExists(file);
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

    private boolean isStale(Path file, Instant cutoff) throws IOException {
        FileTime modified = Files.getLastModifiedTime(file);
        if (modified.toInstant().isBefore(cutoff)) {
            return true;
        }
        String source = file.getParent().getFileName().toString();
        String name = file.getFileName().toString();
        String identifier = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        return catalog.find(source)
                .map(entries -> entries.getEntries().get(identifier))
                .map(entry -> entry.getRemovedAt() != null)
                .orElse(true);
    }

    private Optional<byte[]> fetchThumbnail(ComicSource source, String url) {
        String throttleKey = downloaderProperties.isConfigured(source.id() + "-assets") ? source.id() + "-assets" : source.id();
        throttle.await(throttleKey);
        try {
            return Optional.of(fetchImage(url, source.imageHosts(), throttleKey));
        } catch (RateLimitedException e) {
            Duration backoff = throttle.backOff(throttleKey, 1, e.getRetryAfter());
            log.warn("Rate limited (HTTP 429) fetching catalog thumbnail {}; backing off {}s", url, backoff.toSeconds());
            return Optional.empty();
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
