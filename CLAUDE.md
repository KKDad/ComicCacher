# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**IMPORTANT: When working with ComicAPI code, always follow the coding standards in [ComicAPI/CLAUDE_MEMORY.md](./ComicAPI/CLAUDE_MEMORY.md). These standards override any conflicting global standards.**

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

4. **ComicAPI** - REST API orchestration layer:
   - REST controllers (ComicController, UpdateController, BatchJobController)
   - Services (ComicsService, UpdateService)
   - Repositories (ComicRepository, UserRepository, PreferenceRepository)
   - Infrastructure (Scheduling, Security, Configuration)
   - **Depends on:** comic-common, comic-metrics, comic-engine

### Frontend

5. **ComicViewer** - Angular frontend that:
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
     └─── ComicAPI (orchestrates)
              ↓
         ComicViewer (Angular)
```

## Build Commands

### Backend Modules (Gradle Multi-Module)

```bash
# Build all modules
./gradlew clean build

# Build specific modules
./gradlew :comic-common:build
./gradlew :comic-metrics:build
./gradlew :comic-engine:build
./gradlew :ComicAPI:build

# Run tests
./gradlew :comic-engine:test
./gradlew :ComicAPI:test

# Run integration tests
./gradlew :ComicAPI:integrationTest

# Run the application locally
./gradlew :ComicAPI:bootRun

# Build Docker image
./ComicAPI/build-docker.sh <TAG>
```

### ComicViewer (Angular)

```bash
# Navigate to ComicViewer directory
cd ComicViewer

# Install dependencies
npm install

# Run development server (available at http://localhost:4200)
ng serve

# Run tests
ng test

# Lint the code
ng lint

# Build for production
npm run buildProd

# Build Docker image
./build-docker.sh
```

## Module Architecture Details

### comic-common
- **Purpose:** Shared foundation for all modules
- **Key Interfaces:**
  - `ComicConfigurationService` - Configuration loading/saving
  - `RetrievalStatusService` - Comic retrieval tracking
  - `IComicsBootstrap` - Bootstrap configuration
- **DTOs:** ComicItem, ComicConfig, ComicRetrievalRecord, ImageDto, etc.
- **No external module dependencies**

### comic-metrics
- **Purpose:** Independent metrics collection and reporting
- **Key Classes:**
  - `CacheMetricsCollector` - Collects cache statistics
  - `StorageMetricsCollector` - Collects storage statistics
  - `StatsWriter` interface with JSON/Console implementations
- **Depends on:** comic-common only

### comic-engine
- **Purpose:** Independent comic download and storage engine
- **Key Components:**
  - **Downloaders:** `GoComics` and `ComicsKingdom` implement `IDailyComic`
  - **Facades:** `ComicManagementFacade`, `ComicDownloaderFacade`, `ComicStorageFacade`
  - **Batch Jobs:** Spring Batch integration for scheduled downloads
  - `ComicCacher` - Main entry point for comic caching operations
- **Depends on:** comic-common only
- **Can be used standalone** in other applications

### ComicAPI
- **Purpose:** REST API orchestration and user management
- **Key Components:**
  - **Controllers:** REST endpoints (`ComicController`, `UpdateController`, `BatchJobController`)
    - Controllers call engine/common services directly (no redundant service layer)
  - **Services:** `UpdateService`, `RetrievalStatusServiceImpl`, `AuthService`, `UserService`, `PreferenceService`
  - **Repositories:** `ComicRepository`, `UserRepository`, `PreferenceRepository`
  - **Scheduling:** `DailyRunner` (daily caching at 7:00 AM), `StartupReconciler` (reconciliation at 6:00 AM)
  - **Security:** JWT-based authentication and authorization
  - **Configuration:** `ApplicationConfigurationFacade` (extends `ComicConfigurationService`)
- **Depends on:** comic-common, comic-metrics, comic-engine

### ComicViewer
- Follows Angular component architecture with Material Design
- Key components:
  - `ComicPage` - Main display for comics
  - `Container` - Handles comic layout and organization
  - `Section` - Displays individual comics
  - `Refresh` - Triggers comic cache updates

## Testing

- ComicAPI uses JUnit 5 for unit and integration tests
- ComicViewer uses Karma/Jasmine for Angular component testing

## Docker and Deployment

Both ComicAPI and ComicViewer can be built as Docker containers and deployed to Kubernetes:

```bash
# Build and push API Docker image
./ComicAPI/build-docker.sh <VERSION_TAG>

# Deploy to Kubernetes with Helm
helm upgrade comics comics
```

## Development Workflow

1. Make changes to code
2. Run appropriate tests
3. Build and test locally
4. Build Docker images if needed
5. Deploy to test environment

## Common Tasks

- **Adding a new comic source:** Implement a new class extending `DailyComic` in `comic-engine/downloader/` with site-specific parsing logic
- **Modifying comic display:** Update the Angular components in `ComicViewer/src/app/`
- **Updating caching behavior:** Check `ComicManagementFacade` and `ComicStorageFacade` in comic-engine
- **Adding new metrics:** Implement collectors in `comic-metrics/collector/` and configure output in `MetricsConfiguration`
- **Modifying API endpoints:** Update controllers in `ComicAPI/src/main/java/org/stapledon/api/controller/`