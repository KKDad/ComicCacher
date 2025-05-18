# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**IMPORTANT: When working with ComicAPI code, always follow the coding standards in [ComicAPI/CLAUDE_MEMORY.md](./ComicAPI/CLAUDE_MEMORY.md). These standards override any conflicting global standards.**

## Project Overview

ComicCacher is a web comic downloader and viewer application with two main components:

1. **ComicAPI** - Java Spring Boot backend service that:
   - Downloads and caches web comics from sites like GoComics and ComicsKingdom
   - Exposes a REST API for accessing cached comics
   - Includes a daily runner to refresh comics
   - Maintains a cache of comic images with cleanup after 7 days

2. **ComicViewer** - Angular frontend that:
   - Displays cached comics in a user-friendly interface
   - Uses Angular Material for UI components
   - Supports infinite virtual scrolling for comic viewing

## Build Commands

### ComicAPI (Spring Boot)

```bash
# Build the project
./gradlew :ComicAPI:build

# Run tests
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

## Architecture

### ComicAPI

- **Downloaders**: `GoComics` and `ComicsKingdom` classes implement the `IDailyComic` interface, handling comic strip fetching from different sources.
- **Caching**: Downloaded images are stored using the `ComicCacher` with stats tracking via `ImageCacheStatsUpdater`.
- **Services**: 
  - `ComicsService` provides access to cached comics
  - `UpdateService` handles comic updates
  - `DailyRunner` performs scheduled updates
  - `StartupReconciler` performs daily reconciliation on a configurable schedule (default 6:00 AM)
- **Controllers**: REST endpoints exposed via `ComicController` and `UpdateController`

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

- Adding a new comic source: Implement a new class extending `DailyComic` with site-specific parsing logic
- Modifying comic display: Update the Angular components in ComicViewer/src/app
- Updating caching behavior: Check `CacheConfiguration` and `CacheUtils` classes