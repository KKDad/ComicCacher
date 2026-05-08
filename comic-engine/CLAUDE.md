# Comic Engine Coding Standards

The download and processing engine. Owns scrapers, Spring Batch jobs, the image pipeline, and the comic storage layer. Defers to [@~/comic-api/CLAUDE.md](../comic-api/CLAUDE.md) for cross-cutting Java standards (imports, formatting, Gson, time handling, Lombok).

## Module Layout

| Package | Purpose |
|---------|---------|
| `engine.downloader` | `*DownloaderStrategy` per source (GoComics, ComicsKingdom, Freefall) plus `ComicDownloaderFacade` and `SourceThrottleService` |
| `engine.batch` | Spring Batch jobs — `*JobConfig` per job under `engine.batch.config/` |
| `engine.validation` | Image validation, dedup, metadata backfill |
| `engine.analysis` | Color/grayscale detection |
| `engine.storage` | `FileSystemComicStorageFacade` and supporting classes |
| `engine.caching` | Caffeine cache wiring (navigation, boundary, metadata, lookahead) |
| `engine.management` | Comic management facade |
| `engine.health` | Custom health indicators |

## Downloader Strategy Pattern

- Daily-strip sources extend `AbstractDailyDownloaderStrategy`. Indexed/archive-walk sources extend `AbstractIndexedDownloaderStrategy`. New sources should extend one of these — never `AbstractComicDownloaderStrategy` directly unless implementing a fundamentally different access pattern.
- GoComics requires Selenium (Cloudflare bot detection). ComicsKingdom and Freefall use Jsoup HTML parsing.
- Throttling is mandatory. Pace each request via `SourceThrottleService` using the `downloader.sources.<source>.throttle.min-delay-ms`/`max-delay-ms` properties. GoComics in particular needs aggressive jitter (8–20 s) to stay under Cloudflare's bot threshold.
- The global `downloader.user-agent.default-value` applies unless `downloader.sources.<source>.user-agent` overrides it.

## Spring Batch 5 Conventions

- One job per `@Configuration` class under `engine.batch.config/`. Naming: `<Purpose>JobConfig.java`.
- Use chunk-based steps with explicit `chunk-size`, `max-consecutive-failures`, and per-source overrides where applicable (see `ComicBackfillJobConfig`).
- Schedule via `@Scheduled` annotations driven by `batch.<job>.cron` properties; jobs auto-run is disabled (`spring.batch.job.enabled=false`).
- Use modern `JobOperator.start(Job, JobParameters)` — never the deprecated `JobLauncher`/`SimpleJobLauncher`.
- Execution tracking lands in `${comics.cache.location}/batch-executions.json` via `JsonBatchExecutionTracker`. Don't bypass it.
- New job? Follow [@~/docs/design/adding-batch-jobs.md](../docs/design/adding-batch-jobs.md).

## Image Pipeline

Three layers, in order. Don't skip steps:
1. `ImageValidationService` — size, decode, dimensions
2. `DuplicateImageValidationService` — perceptual + cryptographic hashing
3. `ImageAnalysisService` — color vs grayscale classification

See [@~/docs/design/image-validation.md](../docs/design/image-validation.md).

## Storage

- All persistence goes through `FileSystemComicStorageFacade`. Atomic writes (write-temp-then-move). Never write directly with `Files.write` for JSON metadata.
- Layout: `${comics.cache.location}/<comicId>/{strips,thumbnails,...}`. See [@~/docs/storage/comic-data.md](../docs/storage/comic-data.md).

## Caching

- Caffeine caches are the only in-memory cache. Configured via `comics.cache.caffeine.*` properties; sizes/TTL bound through `CaffeineCacheProperties`.
- If you add a new cache, also add it to `CaffeineCacheConfiguration` so the property binding is real (the `navigation`, `boundary`, and `navigation-dates` caches were historically unbound — verify your additions are wired).
