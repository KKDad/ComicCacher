# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.4.9] - 2026-09-25
### Changed
- Backend runs on Java 25 (toolchain, CI and the `eclipse-temurin:25.0.4_7-jre-alpine-3.24` base image), using Java 25 idioms (pattern switches, `_` for unused variables, `getFirst()`/`getLast()`)
- The Spring Boot BOM comes from the Spring Boot plugin, so the plugin version is the only Spring Boot pin. Libraries move from 4.0.6 (stale) to 4.1.1
- A download run that hits an error now ends FAILED; a corrupt `comics.json` now errors instead of showing an empty list (startup still succeeds); the dashboard "skipped" count only includes strips that are really unavailable
- `MetricsArchiveJob` builds combined metrics on demand instead of reading the stale `combined-metrics.json` (unused since 2026-01-09). Archives older than `comics.metrics.history-retention-days` are now deleted after each archive
- `dev-build-and-run.sh` deploys over ssh with a new `utils/dev-run.sh` on the Docker host, so it works from a Podman workstation
- Dependabot watches the `comic-api` and `comic-hub` Dockerfiles and stays on LTS Java and Node base images
- Comic backfill fills the last 7 days across every comic first, then older gaps round-robin, so recent misses are no longer starved by old gaps in comics early in the alphabet
- Comic backfill runs every 2 hours during the day (`0 30 7-19/2 * * ?`). A run with nothing to do is skipped without any web request, and there is no startup catch-up run
- Backfill limits are per run: `batch.comic-backfill.default-max-per-run` and `sources.<source>.max-per-run` replace the old per-day meaning of `max-per-day`. `max-per-day` is now an optional ceiling across all of a day's runs (0 = none). New `recent-days` setting
- Backfill waits 10 s between downloads (was 5 s)

### Added
- Backfill stops a source for the rest of the run on its first HTTP 429, with no retries (the source-wide backoff still applies). This includes indexed sources (Freefall)
- `backfill-state.json`: backfill gives up on dates that keep coming back unavailable or as duplicates, and learns how far back each comic and source serves strips. Settings: `give-up-after`, `horizon-consecutive-failures`, `horizon-min-comics`, `horizon-tolerance-days`, `retry-given-up-after-days`. The `resetState` job parameter clears it
- `DailyJobScheduler` options for several runs a day and a precondition that skips runs with nothing to do
- `batch.comic-backfill.remember-cached-strips`: backfill scans remember for the day which strips are on disk and recheck only the gaps and the recent window. It only steers which dates are scanned. A mismatch is logged at WARN and each scan logs a summary line. Off when unset; on in `application.properties`
- Schedulers no longer launch a job while a run of it is still in progress (a manual trigger then reports that the job failed to start)
- Support logging: `X-Request-Id` on every request plus one completion line per request, log context (`[req= user= op= job= comic= date=]`) on the console and in per-execution batch logs, compact stack traces, `AUDIT` lines for admin changes, masked emails and a startup summary

### Fixed
- All checkstyle warnings in integration tests
- Backfill counted duplicate images as successful downloads and moved the comic's oldest date back to them
- An HTTP 404 or 410 from a source is reported as unavailable rather than a transient error, so backfill can give up on the date instead of retrying it every run
- A 429 reported as a plain HTTP error (Jsoup `HttpStatusException`, e.g. from Comics Kingdom or Freefall pages) now backs the source off and counts as rate limited. Freefall no longer tries the fallback page after a 429
- Indexed latest-strip downloads back off on 429
- A failed read of the users, preferences or comics config was cached as empty, so the next save could wipe the file. It now throws, and config files are written atomically
- Strip and avatar files are written atomically
- A failed daily download run finished COMPLETED, backfill errors disappeared as filtered items, and `updateComic`/`updateAllComics` returned true on failure
- The HTTP status never reached retrieval records, and every failure was recorded as `COMIC_UNAVAILABLE`
- NPE on undecodable images; a failed hash save logged as success; misleading avatar job summary
- `MetricsArchiveJob` failed every run on dev and archived the same January snapshot every day on prod
- The storage scan counted `metrics-history/` as a comic

### Security
- A failed password reset email no longer reveals which addresses have accounts
- Removed the unused trust-all `DefaultTrustManager`
- Updated actions/setup-java from 6.0.0 to 6.0.1

## [2.4.8] - 2026-09-24
### Changed
- comic-api base image pinned to `eclipse-temurin:21.0.12_8-jre-alpine-3.24` instead of a floating tag; base images use fully qualified `docker.io/library/...` names so Podman can resolve them

### Fixed
- Mother Goose & Grimm and Sherman's Lagoon had no strips on disk since 2026-01-09: comics with `&` or `'` in their names had their date index written to a `comic_{id}/` directory the container couldn't write, and the failed index write deleted the saved strip. The index now uses the comic's strip directory
- A failed save replaces the downloader's `SUCCESS` retrieval record with `STORAGE_ERROR`
- Podman builds dropped the Docker `HEALTHCHECK` (OCI format), which would make every prod deploy roll back. `build-docker.sh` now sets `BUILDAH_FORMAT=docker`

## [2.4.7] - 2026-09-24
### Added
- GoComics HTTP 429 handling: `Retry-After` is honoured (seconds or HTTP date), otherwise exponential backoff with jitter, with retries set by `downloader.sources.<source>.retry.{max-attempts,initial-backoff-ms,max-backoff-ms}` (GoComics: 4 attempts, 60 s initial backoff, 10 min cap). Every 429 logs a WARN
- Deploy split into `utils/prod-build.sh` (workstation) and `utils/prod-run.sh` (Docker host); `prod-run.sh` accepts digest-pinned refs and rolls back to the running image's digest

### Changed
- Prod batch jobs run 90 minutes after dev's (07:30–08:45 Toronto), so dev and prod no longer hit GoComics at the same time
- Browser user agent updated to Chrome 154

### Security
- Updated the org.springframework.boot plugin from 4.0.6 to 4.1.1 (libraries stayed on 4.0.6 until 2.4.9)
- Updated Gradle wrapper from 9.5.0 to 9.7.1
- Updated org.openrewrite.rewrite from 7.32.1 to 7.39.0
- Updated org.openrewrite.recipe:rewrite-static-analysis
- Updated org.openrewrite.recipe:rewrite-testing-frameworks
- Updated actions/checkout from 6 to 7, actions/setup-node from 6 to 7, actions/setup-java from 5 to 6.0.0
- Updated next and other npm dependencies in /comic-hub

## [2.4.6] - 2026-05-09
### Added
- Freefall downloader and a pipeline for strip-number (indexed) comics, with configurable batch job parameters
- Date-column grid reader at `/read` showing every comic for one date; the single-strip reader is now a secondary view
- Reader mode: full-screen reader with infinite scroll on desktop, snap-swipe on mobile, pinch-to-zoom, date picker, reading list drawer and last-read tracking
- GraphQL `randomStrip` query, `Comic.stripWindow` field and `ComicStrip` `width`/`height`
- `UserAgentService` shared by the downloaders
- `utils/prod-build-and-run.sh` with pre-flight gates, health polling and auto-rollback, and a versioned `utils/prod/docker-compose.yml`
- comic-hub `/api/health` endpoint

### Changed
- GoComics downloads are throttled (8–20 s between requests) and each source downloads on its own thread
- Batch log files are named `{jobName}-{date}-{hash}.log`
- Semantic z-index tokens in comic-hub, with a test that rejects numeric z-index classes
- Shared comic-hub UI components (`ImageWithFallback`, `EmptyState`, date utils)
- Persisted timestamps use `OffsetDateTime`/`Instant` (legacy offset-less values still load as UTC); domain DTOs use Gson instead of Jackson
- Removed Jackson BOM overrides now that Spring Boot bundles them

### Fixed
- `ComicDownloadJob` did nothing on every run after the first one since container start (missing `@StepScope` on the date reader)
- Job parameters from GraphQL triggers were discarded by `RunIdIncrementer`
- Indexed comic backfill couldn't start before the first strip was downloaded
- comic-api and comic-hub Docker health checks (missing `wget`, Next.js binding to the container IP, busybox `wget` preferring IPv6)
- Crash when `NEXT_PUBLIC_GRAPHQL_ENDPOINT` is unset
- Mermaid diagrams not rendering on GitHub

### Security
- Containers run as non-root users; comic-api uses a JRE instead of a JDK image
- Security headers (HSTS, X-Frame-Options, etc.) in the Next.js config
- `/actuator/health` open without auth for Docker health checks; mail health indicator disabled
- Updated org.springframework.boot from 4.0.3 to 4.0.6
- Updated Gradle wrapper from 9.4.0 to 9.5.0
- Updated Lombok 1.18.44, Jsoup 1.22.1, Selenium 4.41.0, TwelveMonkeys 3.13.1, Checkstyle 13.3.0, JaCoCo 0.8.14
- Updated org.openrewrite.rewrite from 7.28.1 to 7.32.1
- Updated org.openrewrite.recipe:rewrite-static-analysis
- Updated org.openrewrite.recipe:rewrite-testing-frameworks

## [2.4.5] - 2026-03-19
### Added
- OPERATOR role with read-only operational access
- Branch coverage tests to meet 90% threshold

### Changed
- Restructured docs/ into api/design/storage layout with accuracy fixes
- Refreshed README with marketing-focused copy and feature highlights

### Fixed
- Expired JWT returning FORBIDDEN instead of UNAUTHORIZED
- Replaced brittle message-string auth checks with structured extension checks

### Security
- Locked down unauthenticated GraphQL endpoints

## [2.4.4] - 2026-03-17
### Added
- Batch jobs admin screen with runtime scheduler control
- Retrieval status GraphQL layer with security enforcement
- JaCoCo coverage enforcement with dead code removal

### Changed
- Upgraded Jackson/Spring Boot and cleaned up Gradle configuration
- Strengthened weak frontend tests

### Fixed
- Batch job UI tooltip, timezone conversion, and MDC log file placement
- AvatarBackfillJob wiring and admin-only permission enforcement

### Security
- Updated org.openrewrite.rewrite from 7.28.0 to 7.28.1
- Updated org.openrewrite.recipe:rewrite-static-analysis
- Updated org.openrewrite.recipe:rewrite-testing-frameworks
- Updated next in /comic-hub

## [2.4.3] - 2026-03-15
### Added
- Metrics page with GraphQL metrics layer
- CONTRIBUTING.md, CODE_OF_CONDUCT.md, SECURITY.md

### Security
- Updated undici in /comic-hub

## [2.4.2] - 2026-03-11
### Security
- Updated org.springframework.boot from 4.0.2 to 4.0.3
- Updated Gradle wrapper from 9.3.1 to 9.4.0
- Updated com.graphql-java:graphql-java-extended-scalars from 22.0 to 24.0
- Updated org.openrewrite.rewrite from 7.27.0 to 7.28.0
- Updated org.openrewrite.recipe:rewrite-static-analysis
- Updated org.openrewrite.recipe:rewrite-testing-frameworks
- Updated hono in /comic-hub
- Updated immutable in /comic-hub

## [2.4.1] - 2026-03-05
### Security
- Updated OpenRewrite static-analysis from 2.1.1 to 2.28.0
- Updated Gradle wrapper from 9.3.0 to 9.3.1
- Updated OpenRewrite rewrite from 7.25.0 to 7.27.0
- Updated com.graphql-java:graphql-java-extended-scalars from 22.0 to 24.0
- Various npm dependency bumps

## [2.4.0] - 2026-02-28
### Added
- Next.js 16 frontend (comic-hub) replacing Angular — React 19, TypeScript 5, TanStack Query v5, Zustand, Tailwind CSS 4, Radix UI/shadcn
- GraphQL API layer with custom scalar types (Date, DateTime, JSON)
- Server-side GraphQL proxy with httpOnly cookie authentication

### Changed
- Updated Spring Boot from 4.0.1 to 4.0.2
- Updated Gradle wrapper from 8.14 to 9.3.0

### Removed
- Angular comic-web frontend (deprecated in favor of comic-hub)
- Deprecated REST controllers (AuthController, BatchJobController, HealthController, MetricsController, PreferenceController, RetrievalStatusController, UpdateController, UserController)
- comics-server module

### Security
- Updated actions/upload-artifact from 6 to 7
- Updated org.openrewrite.rewrite from 7.23.0 to 7.25.0
- Updated org.assertj:assertj-core from 3.27.6 to 3.27.7
- Updated org.openrewrite.recipe:rewrite-testing-frameworks
- Various npm bumps (minimatch, rollup, hono, qs)

## [2.3.1] - 2026-01-11
### Added
- Source-specific rate limiting for download sources
- Timing instrumentation for cold-start performance visibility

### Changed
- Migrated Angular 19 → 21 with Vitest and zoneless change detection
- Eliminated MetricsUpdateJob in favor of event-driven persistence
- Migrated Spring Batch APIs to non-deprecated versions

### Fixed
- Navigation cache bug causing stale page state
- @StepScope added to defer findMissingStrips until job execution
- Task scheduling issues

### Security
- Updated actions/upload-artifact from 5 to 6
- Updated org.openrewrite.recipe:rewrite-testing-frameworks
- Updated org.openrewrite.rewrite from 6.26.0 to 7.23.0

## [2.3.0] - 2025-12-29
### Changed
- Updated Spring Boot from 3.5.7 to 4.0.x
- Migrated JUnit assertions to AssertJ via OpenRewrite recipes
- Removed deprecated code and enforced checkstyle

### Fixed
- Navigation cache bug (BUG-NAV-1)
- PageUp/PageDown scroll alignment (BUG-UI-1)
- Cache staleness and error accumulation issues

### Security
- Updated actions/checkout from 4 to 6
- Updated actions/upload-artifact from 4 to 5
- Updated actions/setup-node from 4 to 6
- Updated com.github.ben-manes.caffeine:caffeine from 3.2.2 to 3.2.3

## [2.2.0] - 2025-10-27
### Added
- Comprehensive image validation service (3-layer pipeline: format validation, hash-based dedup, color analysis)
- GitHub Actions workflow for Angular CI
- Null-safety checks for comics without source information
- Configurable Chrome headless mode via application properties

### Changed
- Upgraded comic-web to Angular 19.2 with modern tooling
- UI refreshed with glassmorphism design
- Batch job reorganization and tracking validation

### Fixed
- Spring Batch bean conflict in production environment
- All test failures after Angular 19 upgrade

### Security
- Updated org.springframework.boot from 3.4.5 to 3.5.7
- Updated org.seleniumhq.selenium:selenium-java from 4.11.0 to 4.38.0
- Updated org.springdoc:springdoc-openapi-starter-webmvc-ui
- Updated com.github.ben-manes.caffeine:caffeine from 3.1.8 to 3.2.2

## [2.1.0] - 2025-10-23
### Added
- Spring Batch for comic retrieval jobs (ComicDownloadJob, ComicBackfillJob, AvatarBackfillJob, ImageMetadataBackfillJob, MetricsArchiveJob, RetrievalRecordPurgeJob)

### Changed
- 10-phase modular refactoring: monolith decomposed into comic-common, comic-metrics, comic-engine, comic-api
- ComicCacher delegated to ComicManagementFacade
- Renamed CacheUtils → AccessMetricsCollector, ImageCacheStatsUpdater → StorageMetricsCollector
- Renamed ComicAPI → comic-api for consistent module naming
- Removed on-demand download infrastructure (CacheMissEvent)

### Fixed
- GoComics CSS selectors updated for site changes
- Spring Batch integration and compilation fixes

### Security
- Updated com.google.guava:guava from 33.4.6-jre to 33.5.0-jre
- Updated org.projectlombok:lombok from 1.18.34 to 1.18.42
- Updated com.fasterxml.jackson:jackson-bom from 2.18.3 to 2.20.0
- Updated org.jsoup:jsoup from 1.18.1 to 1.21.2

## [2.0.3] - 2025-05-22
### Added
- Enhanced API documentation and updated user model
- Username uniqueness check during registration
- Postman collection and environment for API testing
- Null name validation and improved error handling in GlobalExceptionHandler
- Health check endpoint and related services
- Daily reconciliation scheduling with unit tests
- Task execution tracking to ensure operations run once per day
- OpenAPI documentation with Swagger UI
- OS-specific cache path handling
- Accessibility improvements with ARIA attributes and keyboard navigation
- Loading indicators and signal-based state management (ComicStateService)

### Changed
- Restructured codebase into core, api, infrastructure, common domains
- Refactored ComicService to use signals and modern RxJS patterns
- Converted app to standalone components with modern bootstrapping
- Upgraded Angular through v17 → v18, including Material, CDK, and ESLint
- Updated to Java 21 and modernized Java codebase
- Improved JaCoCo configuration with exclusion patterns
- Optimized build configuration with modern bundling

### Fixed
- GoComics caching
- SpringDoc OpenAPI compatibility with Spring Boot 3.4.5
- Spring Boot dependency conflicts with JWT and OpenAPI libraries
- StartupReconciler to respect enabled property
- Profile handling for API documentation generation

### Security
- Updated org.springdoc:springdoc-openapi-starter-webmvc-ui to 2.8.8
- Updated org.jsoup:jsoup from 1.18.1 to 1.20.1
- Updated com.google.code.gson:gson from 2.11.0 to 2.13.1

## [2.0.2] - 2025-03-31
### Security
- Updated com.google.guava:guava from 33.3.1-jre to 33.4.6-jre

## [2.0.1] - 2025-03-24
### Security
- Updated org.springframework.boot from 3.3.5 to 3.4.4
- Updated com.coditory.integration-test from 2.0.3 to 2.2.5

## [2.0.0] - 2022-09
### Added
- Introduced Lombok
- Switched junit4 to junit5
- Switched ascii-docs to swagger-ui

### Changed
- Updated Angular from 7.2.3 to 14.2.3
- Split the docker container into separate Backend and frontend containers
- Updated Java 8 to Java 11
- Updated Spring 2.5 to 2.7
- Split apart unit tests and integration testing

## [1.2.0] - 2019-11-10
### Added
- Background to main page
- Several new comics:
  - Luann
  - CalvinAndHobbes
  - Pickles
  - Frank-And-Ernest
  - ScaryGary
  - Beetle Bailey
  - Dustin
  - Hagar
  - Mother Goose & Grimm
  - Sherman's Lagoon
  - Zits

## [1.1.0] - 2019-11-09
### Added
- Support for KingFeatures
- BabyBlues comic
- Support to Reconcile CacherBootstrapConfig and ComicConfig

### Changed
- Updated API documentation for previously undocumented methods

### Deprecated
- Minimum Security comic

## [1.0.0] - 2019
### Added
- Statistics about the Images cached in each top-level comics cache directory to speed up retrieval

## [0.2.0]
### Added
- Initial REST api exposing method /comics/v1/list
- File maintenance for comics.json in ComicCacher

## [0.1.0] - Initial Version
### Added
- Caching support for GoComics
