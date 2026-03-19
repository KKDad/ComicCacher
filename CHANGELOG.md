# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Fixed
- All checkstyle warnings in integration tests

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
