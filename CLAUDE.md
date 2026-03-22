# CLAUDE.md

## Project Overview

ComicCacher is a web comic downloader and viewer application with a modular multi-module architecture.

### Backend Modules (Java/Spring Boot)

1. **comic-common** - Shared foundation: DTOs, service interfaces, config, utilities. No external module dependencies.
2. **comic-metrics** - Cache & storage metrics collection. Depends on: comic-common only.
3. **comic-engine** - Download & storage engine, Spring Batch jobs. Depends on: comic-common, comic-metrics.
4. **comic-api** - REST + GraphQL API layer. Depends on: comic-common, comic-metrics, comic-engine.

### Frontend

5. **comic-hub** - New web frontend (Next.js 16, React 19, TypeScript). See `comic-hub/README.md`.
6. **comic-web** - Legacy Angular frontend (being replaced by comic-hub).

### Architecture

```
comic-common (shared DTOs, config, services)
     ↑
     ├─── comic-metrics (depends on comic-common)
     ├─── comic-engine (depends on comic-common, comic-metrics)
     │         ↑
     └─── comic-api (orchestrates all)
              ↓
         comic-hub (Next.js)
```

### Key Dependency Versions

**Backend:**
- Java 21, Spring Boot 4.0.4
- Lombok 1.18.44, Gson 2.11.0, Guava 33.5.0-jre
- Jsoup 1.22.1, Caffeine Cache 3.2.3
- Jackson BOM 2.21.1, JJWT 0.13.0
- Selenium 4.41.0, Springdoc OpenAPI 2.8.13
- TwelveMonkeys ImageIO 3.13.1

**Frontend (comic-hub):**
- Next.js 16, React 19, TypeScript 5
- TanStack Query v5, graphql-request v7, Zustand v5
- Tailwind CSS 4, Radix UI / shadcn
- react-hook-form v7, Zod v4, Sonner v2, next-themes v0.4
- Vitest, React Testing Library, MSW
- Node.js 22 LTS

## Build Commands

**Backend:**
- Build: `./gradlew clean build`
- Run API: `./gradlew :comic-api:bootRun`
- Unit tests: `./gradlew :comic-api:test`
- Integration tests: `./gradlew :comic-api:integrationTest`
- **Final verification:** `./gradlew clean testAll` (run before finishing any task)

**Frontend (comic-hub):**
- Dev: `cd comic-hub && npm run dev`
- Build: `cd comic-hub && npm run build`
- Test: `cd comic-hub && npm test`
- GraphQL codegen: `cd comic-hub && npm run codegen`

## Git Workflow

**Never commit or push directly to master.**

1. Create a feature/fix branch: `git checkout -b feature/description` or `git checkout -b fix/description`
2. Commit changes to the branch
3. Push and create a PR via `gh pr create`
4. Merge via GitHub after review

**Never add `Co-Authored-By` lines to commit messages.**

## Module Details

### comic-common
Shared DTOs (ComicItem, ComicConfig, ComicRetrievalRecord, ImageDto), interfaces (ComicConfigurationService, RetrievalStatusService, IComicsBootstrap), utilities (Bootstrap, Direction, ImageUtils).

### comic-metrics
CacheMetricsCollector, StorageMetricsCollector, StatsWriter (JSON/Console). Depends on: comic-common only.

### comic-engine
Downloaders (GoComics, ComicsKingdom implement IDailyComic), facades (ComicManagementFacade, ComicDownloaderFacade, ComicStorageFacade), Spring Batch integration (6 jobs: ComicDownloadJob, ComicBackfillJob, AvatarBackfillJob, ImageMetadataBackfillJob, MetricsArchiveJob, RetrievalRecordPurgeJob). Depends on: comic-common, comic-metrics.

### comic-api
Controllers (ComicController, UpdateController, BatchJobController), Services (UpdateService, RetrievalStatusServiceImpl, AuthService, UserService, PreferenceService), Repositories (ComicRepository, UserRepository, PreferenceRepository). JWT-based security with three roles: USER (default), OPERATOR (read-only operational access), ADMIN (full control). **Follow the coding standards in [comic-api/CLAUDE.md](comic-api/CLAUDE.md) — they override any conflicting global standards.**

### comic-hub
Next.js 16 App Router with httpOnly cookie auth, server-side GraphQL proxy, codegen-generated hooks. See `comic-hub/README.md`.

**Next.js 16 Proxy (replaces middleware):**
- `middleware.ts` is **deprecated** in Next.js 16 — use `proxy.ts` instead (same level as `app/`).
- Export a named `proxy` function (or default export) and an optional `config` with `matcher`.
- Proxy files must **not** import shared modules or globals — inline any constants they need. See [Next.js proxy docs](https://nextjs.org/docs/app/api-reference/file-conventions/proxy).

## Image Validation

3-layer pipeline in comic-engine:

1. **ImageValidationService** — null/empty check, file size (max 10MB), ImageIO decode test, dimension check (min 100x50 for strips)
2. **DuplicateImageValidationService** — year-scoped hash-based dedup with perceptual (DIFFERENCE_HASH, AVERAGE_HASH) and cryptographic (MD5, SHA256) algorithms
3. **ImageAnalysisService** — color/grayscale detection via pixel sampling

- **comic-common:** `ImageFormat` enum, `ImageValidationResult` DTO, `ImageValidationService` interface
- **Format Support:** PNG, JPEG, GIF, BMP, WebP, TIFF via TwelveMonkeys ImageIO

See [docs/design/image-validation.md](docs/design/image-validation.md) for the full pipeline specification.

## Time Handling Rules

1. **Storage:** All persisted timestamps MUST be UTC. Use `OffsetDateTime` (with explicit UTC offset) or `Instant`, never bare `LocalDateTime` for timestamps.
2. **`LocalDate`** is fine for date-only values (comic dates, filter ranges) — no timezone ambiguity.
3. **`LocalDateTime`** is acceptable only for purely local display formatting — never for storage or wire transfer.
4. **Spring Batch boundary:** Spring Batch returns `LocalDateTime` in the JVM's system timezone. Convert at the resolver boundary using the configured `batch.timezone` property.
5. **GraphQL wire format:** The `DateTime` scalar serializes `OffsetDateTime` as ISO-8601 with offset (e.g., `2026-03-18T10:00:00-04:00`). Clients parse and display in the user's local timezone.
6. **Frontend:** Use `new Date(isoString)` for parsing. Display with `toLocaleString()` or relative formatters. Never assume a timezone — the offset in the ISO string handles it.
7. **Gson adapters:** `OffsetDateTimeAdapter` handles ISO-8601 with offset. `LocalDateTimeAdapter` exists for backward compatibility only — prefer `OffsetDateTime` in new code.

## Debug Utilities

- **`utils/fetch-prod-logs.sh [lines]`** — Fetches Docker logs from production container via SSH (default 500 lines).
- **`utils/tunnel-to-prod-api.sh`** — SSH tunnel mapping `localhost:8888` to the production API.

## Documentation

Detailed API, design, and storage documentation lives in [`docs/`](docs/README.md):

- **api/** — Endpoint reference for Comics, Auth, Users, Batch Jobs, Metrics, Health
- **design/** — Architecture, download pipeline, batch jobs, image validation
- **storage/** — NFS directory layout, configuration files, operational state, comic data
