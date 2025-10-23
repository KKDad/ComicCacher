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

## Build Commands

### Backend Modules (Gradle Multi-Module)

```bash
# Build all modules
./gradlew clean build

# Build specific modules
./gradlew :comic-common:build
./gradlew :comic-metrics:build
./gradlew :comic-engine:build
./gradlew :comic-api:build

# Run tests
./gradlew :comic-engine:test
./gradlew :comic-api:test

# Run integration tests
./gradlew :comic-api:integrationTest

# Run the application locally
./gradlew :comic-api:bootRun

# Build Docker image
./comic-api/build-docker.sh <TAG>
```

### comic-web (Angular)

```bash
# Navigate to comic-web directory
cd comic-web

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

### comic-api
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

### comic-web
- **Technology Stack:** Angular 19.2, TypeScript 5.8, Node.js 22 LTS
- Follows Angular component architecture with Material Design
- Key components:
  - `ComicPage` - Main display for comics
  - `Container` - Handles comic layout and organization
  - `Section` - Displays individual comics
  - `Refresh` - Triggers comic cache updates

#### comic-web Best Practices

**TypeScript & Code Quality:**
- Use primitive types (`string`, `number`, `boolean`) instead of object wrappers (`String`, `Number`, `Boolean`)
- Avoid `any` types - always use proper TypeScript types for type safety
- Enable TypeScript strict mode flags (strictNullChecks, strictFunctionTypes, etc.)
- Add JSDoc comments to all public methods and classes
- Use `readonly` for properties that shouldn't change after initialization
- Extract magic numbers to named constants with descriptive names
- Follow camelCase naming convention (not snake_case)
- Use semantic class names in templates (e.g., `comic-card` not `example-card`)

**Angular Patterns:**
- Use standalone components (no NgModules required in Angular 19+)
- Use signals for reactive state management
- Implement `OnDestroy` and unsubscribe from all subscriptions to prevent memory leaks
- Use `inject()` function for dependency injection in class fields
- Add explicit return types to all public methods
- Remove empty lifecycle methods (`ngOnInit()` with no implementation)
- Use Angular CDK virtual scrolling for large lists
- Follow Angular style guide for component selectors (use `app-` prefix)

**Testing:**
- All tests must pass before merging (strict enforcement)
- Use `createStandaloneComponentFixture()` helper for test setup
- Mock services with `jasmine.createSpyObj()` and provide return values
- Use testing utilities (`getText`, `expectExists`, `click`) for cleaner tests
- Install jasmine.clock() BEFORE calling methods that use setTimeout
- Test both success and error paths for async operations
- Update test expectations when fixing component behavior (don't just make tests pass)
- Run `npm run lint` and `npm run test:headless` before committing

**Subscription Management:**
- Always return `Subscription` from methods that create subscriptions
- Store subscriptions in a `Subscription` object and unsubscribe in `ngOnDestroy()`
- Use `takeUntil()` or `takeUntilDestroyed()` for automatic cleanup
- Avoid creating orphaned subscriptions that never get cleaned up

**Performance:**
- Use `trackBy` functions in `*ngFor` loops for better performance
- Implement virtual scrolling for large lists (CDK `cdk-virtual-scroll-viewport`)
- Use `OnPush` change detection strategy where appropriate
- Lazy load routes and modules when possible

**Node.js & NPM:**
- Use `.nvmrc` file to specify Node.js version for team consistency
- Create `.npmrc` in project root if using public registry (avoids corporate proxy issues)
- Use `npm ci` in CI/CD pipelines (faster, more reliable than `npm install`)
- Keep `package-lock.json` in version control
- Update dependencies regularly but test thoroughly after upgrades

**Git Commits:**
- NEVER add `Co-Authored-By` lines to commit messages
- Keep commit messages concise and descriptive
- Run all tests before committing
- Use conventional commit format when appropriate

## Testing

- comic-api uses JUnit 5 for unit and integration tests
- comic-web uses Karma/Jasmine for Angular component testing

## Docker and Deployment

Both comic-api and comic-web can be built as Docker containers and deployed to Kubernetes:

```bash
# Build and push API Docker image
./comic-api/build-docker.sh <VERSION_TAG>

# Deploy to Kubernetes with Helm
helm upgrade comics comics
```

## Development Workflow

1. Make changes to code
2. Run appropriate tests
3. Build and test locally
4. Build Docker images if needed
5. Deploy to test environment

## Version Management

When incrementing the build version, you must update **all** of the following files to maintain consistency:

### Files to Update:

1. **comic-api/build.gradle** - Update the `version` property
   ```gradle
   version = '2.1.35'
   ```

2. **comic-api/Dockerfile** - Update both the COPY and ENTRYPOINT lines
   ```dockerfile
   COPY build/libs/comic-api-2.1.35.jar /
   ENTRYPOINT [ "java", "-jar", "/comic-api-2.1.35.jar" ]
   ```

3. **comic-web/package.json** - Update the `version` property
   ```json
   "version": "2.1.35"
   ```

### Verification:

After updating all version references, verify the changes:
```bash
# Check all version references (excluding node_modules and .git)
grep -r "VERSION_NUMBER" --include="*.gradle" --include="Dockerfile" --include="package.json" --exclude-dir=node_modules --exclude-dir=.git .
```

### Docker Build Scripts:

The docker build scripts (`comic-api/build-docker.sh` and `comic-web/build-docker.sh`) accept the version tag as a command-line argument, so they do not need to be updated when the version changes:
```bash
./comic-api/build-docker.sh 2.1.35
./comic-web/build-docker.sh 2.1.35
```

## Image Validation Architecture

The application includes a comprehensive image validation service to ensure only well-formed images are saved to the cache.

### Components

**comic-common** (shared interfaces and DTOs):
- `ImageFormat` enum - Supported formats (PNG, JPEG, GIF, WEBP, UNKNOWN)
- `ImageValidationResult` DTO - Validation results with format, dimensions, size, and error details
- `ImageValidationService` interface - Service contract for validation

**comic-engine** (implementation):
- `ImageValidationServiceImpl` - Validates images using standard Java ImageIO
  - Checks for null/empty data
  - Validates file size (max 10MB)
  - Verifies image can be decoded
  - Validates dimensions > 0
  - Determines format (PNG, JPEG, GIF, WEBP)

### Integration Points

1. **AbstractComicDownloaderStrategy** - Validates all downloaded comic strips and avatars before returning
2. **ComicStorageFacadeImpl** - Validates images before saving to disk with minimum dimension requirements (100x50 for comic strips)

### Key Features

- **Format Detection**: Automatically identifies PNG, JPEG, GIF, and WEBP (if TwelveMonkeys plugin present)
- **Corruption Detection**: Rejects truncated, corrupted, or invalid image data
- **Error Page Detection**: Prevents HTML error pages from being saved as images
- **Dimension Validation**: Ensures comic strips meet minimum size requirements
- **Size Limits**: Rejects images over 10MB
- **Fail Fast**: Issues detected immediately at download, before disk I/O

### WebP Support (Optional)

To add WebP support, include TwelveMonkeys ImageIO WebP plugin in `comic-engine/build.gradle`:
```gradle
implementation 'com.twelvemonkeys.imageio:imageio-webp:3.12.0'
```

The plugin is auto-discovered by Java's ImageIO service provider mechanism. No code changes required.

## Common Tasks

- **Adding a new comic source:** Implement a new class extending `DailyComic` in `comic-engine/downloader/` with site-specific parsing logic
- **Modifying comic display:** Update the Angular components in `comic-web/src/app/`
- **Updating caching behavior:** Check `ComicManagementFacade` and `ComicStorageFacade` in comic-engine
- **Adding new metrics:** Implement collectors in `comic-metrics/collector/` and configure output in `MetricsConfiguration`
- **Modifying API endpoints:** Update controllers in `comic-api/src/main/java/org/stapledon/api/controller/`
- **Adjusting image validation:** Modify `ImageValidationServiceImpl` in comic-engine/validation/ or minimum dimension constants in `ComicStorageFacadeImpl`