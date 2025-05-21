# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.33] - 2025-05-19
### Added
- Check for existing usernames during user registration
- Enhanced user management capabilities

### Changed
- Refactored tests to be more production-like to better catch issues

### Fixed
- Fixed all tests to ensure system stability

## [2.0.32] - 2025-05-19
### Added
- Test for handling quoted refresh tokens in AuthController
- Handling for null comic names in directory naming
- Enhanced validation for comic names

## [2.0.31] - 2025-05-19
### Added
- Postman collection and environment for ComicCacher API testing
- Enhanced comic retrieval and reconciliation with better handling of null names
- Null name validation for comics
- Improved error handling in GlobalExceptionHandler

## [2.0.30] - 2025-05-18
### Added
- TypeMismatchException handler to GlobalExceptionHandler for improved error handling

### Changed
- Refactored tests to use List.of() for role assignments
- Improved mock implementations
- Enhanced cache miss event handling
- Removed CacheUtils bean definition and added custom deserializer for IComicsBootstrap

## [2.0.29] - 2025-05-18
### Added
- Comprehensive API documentation for ComicCacher

## [2.0.28] - 2025-05-18
### Added
- Health check endpoint and related services

### Changed
- Consolidated and enhanced testing for ComicAPI components

## [2.0.27] - 2025-05-18
### Changed
- Updated documentation to reflect daily reconciliation scheduling for StartupReconciler
- Restructured codebase into better-separated domains (core, api, infrastructure, common)

## [2.0.26] - 2025-05-18
### Added
- Daily reconciliation scheduling
- Unit tests for StartupReconciler
- Documentation for JSON storage details in ComicCacher

## [2.0.25] - 2025-05-18
### Changed
- Removed temporary files for cleaner codebase

## [2.0.24] - 2025-05-17
### Security
- Updated org.springdoc:springdoc-openapi-starter-webmvc-ui to version 2.8.8

## [2.0.23] - 2025-05-15
### Security
- Updated org.jsoup:jsoup from 1.18.1 to 1.20.1
- Updated com.google.code.gson:gson from 2.11.0 to 2.13.1

## [2.0.22] - 2025-05-15
### Fixed
- SpringDoc OpenAPI compatibility with Spring Boot 3.4.5
- Simplified Gradle dependencies to reduce conflicts

## [2.0.21] - 2025-05-15
### Added
- OS-specific cache path handling
- Access to Swagger UI

## [2.0.20] - 2025-05-15
### Fixed
- Tests for TaskExecutionTracker implementation

## [2.0.19] - 2025-05-15
### Added
- Task execution tracking to ensure operations run once per day

## [2.0.18] - 2025-05-15
### Fixed
- Spring Boot dependency conflicts with JWT and OpenAPI libraries

## [2.0.17] - 2025-05-15
### Added
- Initial OpenAPI documentation

## [2.0.16] - 2025-05-15
### Added
- Shell script for OpenAPI documentation generation

## [2.0.15] - 2025-05-15
### Fixed
- StartupReconciler to respect enabled property
- Added necessary dependencies for StartupReconciler

## [2.0.14] - 2025-05-15
### Changed
- Used customBootRun feature to set profile for API documentation generation

## [2.0.13] - 2025-05-15
### Fixed
- Profile handling for API documentation generation

## [2.0.12] - 2025-05-15
### Added
- Disabled-caching profile for API documentation generation

## [2.0.11] - 2025-05-09
### Changed
- Improved JaCoCo configuration with exclusion patterns

## [2.0.10] - 2025-05-09
### Added
- Accessibility improvements with ARIA attributes and keyboard navigation

### Changed
- Optimized build configuration and implemented modern bundling
- Enhanced responsive design for mobile and tablet viewing

## [2.0.9] - 2025-05-09
### Added
- Loading indicators and error handling throughout the application
- Signal-based state management with ComicStateService

## [2.0.8] - 2025-05-09
### Changed
- Refactored ComicService to use signals and modern RxJS patterns
- Updated scroll virtualization with latest Angular CDK patterns and improved styling

## [2.0.7] - 2025-05-09
### Changed
- Converted app to use standalone components and modern bootstrapping
- Updated remaining dependencies for Angular 18 modern stack

## [2.0.6] - 2025-05-09
### Changed
- Updated Angular Material, CDK, and CDK-experimental to v18
- Updated Angular ESLint packages to v18

## [2.0.5] - 2025-05-09
### Changed
- Updated to Angular 18 and migrated HTTP modules

## [2.0.4] - 2025-05-09
### Changed
- Updated Angular Material and CDK to v17
- Updated Angular ESLint packages to v17
- Updated package-lock.json for Angular v17

## [2.0.3] - 2025-05-09
### Changed
- Updated Angular to v17
- Updated to Java 21
- Modernized Java codebase

### Fixed
- GoComics caching

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