# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**IMPORTANT: When working with comic-api code, always follow the coding standards in [comic-api/CLAUDE_MEMORY.md](./comic-api/CLAUDE_MEMORY.md). These standards override any conflicting global standards.**

## Project Overview

ComicCacher is a web comic downloader and viewer application with a **modular multi-module architecture**:

### Backend Modules (Java/Spring Boot)

1. **comic-common** - Shared foundation module:
    - Common DTOs (ComicItem, ComicConfig, ComicRetrievalRecord, etc.)
    - Service interfaces (ComicConfigurationService, RetrievalStatusService)
    - Configuration classes (IComicsBootstrap, CacheProperties)
    - Utilities (Bootstrap, Direction, ImageUtils)
    - Infrastructure (TaskExecutionTracker, WebInspector)

2. **comic-metrics** - Independent metrics collection module:
    - Metric collectors (CacheMetricsCollector, StorageMetricsCollector)
    - Stats writers (JsonStatsWriter, ConsoleStatsWriter)
    - Pluggable metrics outputs
    - **Depends only on:** comic-common

3. **comic-engine** - Independent download/storage engine:
    - Comic downloaders (GoComics, ComicsKingdom, ComicCacher)
    - Management facade (ComicManagementFacade)
    - Storage facade (ComicStorageFacade)
    - Spring Batch jobs for scheduled downloads
    - **Depends only on:** comic-common

4. **comic-api** - REST API orchestration layer:
    - REST controllers (ComicController, UpdateController, BatchJobController)
    - Services (ComicsService, UpdateService)
    - Repositories (ComicRepository, UserRepository, PreferenceRepository)
    - Infrastructure (Scheduling, Security, Configuration)
    - **Depends on:** comic-common, comic-metrics, comic-engine

### Frontend

5. **comic-web** - Angular frontend that:
    - Displays cached comics in a user-friendly interface
    - Uses Angular Material for UI components
    - Supports infinite virtual scrolling for comic viewing

### Architecture Diagram

```
comic-common (shared DTOs, config, services)
     ↑
     ├─── comic-metrics (independent)
     ├─── comic-engine (independent)
     │         ↑
     └─── comic-api (orchestrates)
              ↓
         comic-web (Angular)
```

### Key Dependency Versions

**Backend (Java/Spring Boot)**:
- Java: 21
- Spring Boot: 3.5.7
- Lombok: 1.18.42
- Gson: 2.11.0
- Guava: 33.5.0-jre
- Jsoup: 1.21.2
- Caffeine Cache: 3.2.2
- Jackson BOM: 2.20.0
- JJWT: 0.11.5
- Selenium: 4.38.0
- Springdoc OpenAPI: 2.8.13
- TwelveMonkeys ImageIO: 3.12.0

**Frontend (Angular)**:
- Angular: 19.2.0
- TypeScript: 5.8.0
- Node.js: 22 LTS
- RxJS: 7.8.2
- Jasmine: 5.7.1
- Karma: 6.4.4

## Build Commands

**Backend:** `./gradlew clean build`, `./gradlew :comic-api:bootRun`, `./gradlew :comic-api:integrationTest`
**Frontend:** `cd comic-web && npm install`, `ng serve` (http://localhost:4200), `ng test`, `ng lint`, `npm run buildProd`

## Module Architecture Details

### comic-common
Shared foundation: DTOs (ComicItem, ComicConfig, ComicRetrievalRecord, ImageDto), interfaces (ComicConfigurationService, RetrievalStatusService, IComicsBootstrap), utilities (Bootstrap, Direction, ImageUtils). No external module dependencies.

### comic-metrics
Independent metrics: CacheMetricsCollector, StorageMetricsCollector, StatsWriter (JSON/Console). Depends on: comic-common only.

### comic-engine
Download/storage engine: Downloaders (GoComics, ComicsKingdom implement IDailyComic), facades (ComicManagementFacade, ComicDownloaderFacade, ComicStorageFacade), Spring Batch integration. Depends on: comic-common only. Can be used standalone.

### comic-api
REST orchestration: Controllers (ComicController, UpdateController, BatchJobController call engine/common services directly), Services (UpdateService, RetrievalStatusServiceImpl, AuthService, UserService, PreferenceService), Repositories (ComicRepository, UserRepository, PreferenceRepository). 6 Spring Batch schedulers: ComicDownloadJobScheduler (6 AM cron, CommandLineRunner for startup), ComicBackfillJobScheduler, RetrievalRecordPurgeJobScheduler, MetricsUpdateJobScheduler, MetricsArchiveJobScheduler, ImageMetadataJobScheduler (all with @Scheduled, @PostConstruct logging, @ConditionalOnProperty). JWT-based security. ApplicationConfigurationFacade extends ComicConfigurationService. Depends on: comic-common, comic-metrics, comic-engine.

### comic-web
Angular 19.2, TypeScript 5.8, Material Design. Components: ComicPage, Container, Section, Refresh.

#### comic-web Standards

- **TypeScript:** Primitive types, strict mode, no `any`, JSDoc on public methods, `readonly` where applicable
- **Angular:** Standalone components, signals for state, `inject()` for DI, explicit return types, virtual scrolling (CDK)
- **Testing:** All tests must pass before merge; use `createStandaloneComponentFixture()`, `jasmine.createSpyObj()`, testing utilities
- **Subscriptions:** Return `Subscription` from methods, unsubscribe in `ngOnDestroy()`, use `takeUntil()` or `takeUntilDestroyed()`
- **Commits:** Never add `Co-Authored-By` lines, run tests before committing

## Testing & Deployment

- comic-api: JUnit 5 (unit/integration tests)
- comic-web: Karma/Jasmine
- Docker images: `./comic-api/build-docker.sh <TAG>`, `./comic-web/build-docker.sh <TAG>`
- Kubernetes deployment: `helm upgrade comics comics`

## Version Management

Update version in: `comic-api/build.gradle`, `comic-api/Dockerfile`, `comic-web/package.json`

## Image Validation

**comic-common:** `ImageFormat` enum, `ImageValidationResult` DTO, `ImageValidationService` interface
**comic-engine:** `ImageValidationServiceImpl` - validates null/empty data, file size (max 10MB), decode ability, dimensions (min 100x50 for strips)
**Integration:** `AbstractComicDownloaderStrategy` validates downloads; `ComicStorageFacadeImpl` validates before disk write
**Format Support:** PNG, JPEG, GIF, BMP, WebP, TIFF via TwelveMonkeys ImageIO 3.12.0 (auto-discovered by Java ImageIO SPI)

## Debug Utilities

**utils/debug/** - Production debugging tools:
- `fetch-prod-logs.sh [lines]` - Fetches Docker logs from production container (portainer.stapledon.ca) via SSH
- `tunnel-to-prod-api.sh` - Creates SSH tunnel mapping localhost:8888 to production API for local debugging