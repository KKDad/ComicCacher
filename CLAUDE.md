# CLAUDE.md

## Project Overview

ComicCacher is a web comic downloader and viewer application with a modular multi-module architecture.

### Backend Modules (Java/Spring Boot)

1. **comic-common** - Shared foundation: DTOs, service interfaces, config, utilities. No external module dependencies.
2. **comic-metrics** - Cache & storage metrics collection. Depends on: comic-common only.
3. **comic-engine** - Download & storage engine, Spring Batch jobs. Depends on: comic-common only.
4. **comic-api** - REST + GraphQL API layer. Depends on: comic-common, comic-metrics, comic-engine.

### Frontend

5. **comic-hub** - New web frontend (Next.js 16, React 19, TypeScript). See `comic-hub/README.md`.
6. **comic-web** - Legacy Angular frontend (being replaced by comic-hub).

### Architecture

```
comic-common (shared DTOs, config, services)
     ↑
     ├─── comic-metrics (independent)
     ├─── comic-engine (independent)
     │         ↑
     └─── comic-api (orchestrates)
              ↓
         comic-hub (Next.js)
```

### Key Dependency Versions

**Backend:**
- Java 21, Spring Boot 3.5.7
- Lombok 1.18.42, Gson 2.11.0, Guava 33.5.0-jre
- Jsoup 1.21.2, Caffeine Cache 3.2.2
- Jackson BOM 2.20.0, JJWT 0.11.5
- Selenium 4.38.0, Springdoc OpenAPI 2.8.13
- TwelveMonkeys ImageIO 3.12.0

**Frontend (comic-hub):**
- Next.js 16, React 19, TypeScript 5
- TanStack Query v5, graphql-request v7, Zustand v5
- Tailwind CSS 4, Radix UI / shadcn
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
Downloaders (GoComics, ComicsKingdom implement IDailyComic), facades (ComicManagementFacade, ComicDownloaderFacade, ComicStorageFacade), Spring Batch integration. Depends on: comic-common only.

### comic-api
Controllers (ComicController, UpdateController, BatchJobController), Services (UpdateService, RetrievalStatusServiceImpl, AuthService, UserService, PreferenceService), Repositories (ComicRepository, UserRepository, PreferenceRepository). JWT-based security. **Follow the coding standards in [comic-api/CLAUDE.md](comic-api/CLAUDE.md) — they override any conflicting global standards.**

### comic-hub
Next.js 16 App Router with httpOnly cookie auth, server-side GraphQL proxy, codegen-generated hooks. See `comic-hub/README.md`.

## Image Validation

- **comic-common:** `ImageFormat` enum, `ImageValidationResult` DTO, `ImageValidationService` interface
- **comic-engine:** `ImageValidationServiceImpl` — validates null/empty data, file size (max 10MB), decode ability, dimensions (min 100x50 for strips)
- **Format Support:** PNG, JPEG, GIF, BMP, WebP, TIFF via TwelveMonkeys ImageIO 3.12.0

## Debug Utilities

- **`utils/fetch-prod-logs.sh [lines]`** — Fetches Docker logs from production container via SSH (default 500 lines).
- **`utils/tunnel-to-prod-api.sh`** — SSH tunnel mapping `localhost:8888` to the production API.
