# ComicAPI Modularization - Refactoring Plan

## Status: Phase 5a - Infrastructure Extraction (✅ COMPLETED)

Last Updated: 2025-10-22
Phase 1 Completed: 2025-10-22
Phase 2 Completed: 2025-10-22
Phase 3 Completed: 2025-10-22
Phase 4 Completed: 2025-10-22
Phase 5a Completed: 2025-10-22

---

## Overview

Refactoring ComicAPI from a monolithic application into a clean multi-module architecture with three main modules:
- **comic-api**: REST API layer with auth/user management
- **comic-engine**: Comic download, caching, and storage engine
- **comic-metrics**: Metrics collection and reporting (pluggable outputs)

## Target Architecture

```
comicapi-root/
├── comic-common/        # Shared DTOs, utilities, properties
├── comic-engine/        # Download, caching, storage
├── comic-metrics/       # Metrics collection & reporting
└── comic-api/          # REST API + Auth + Scheduling
```

### Module Dependencies

```
comic-common (shared)
     ↑
     ├─── comic-engine (core functionality)
     │         ↑
     │         └─── comic-metrics (observability)
     │                   ↑
     └─── comic-api (presentation + orchestration)
```

---

## Phase 1: Foundation & Cleanup

**Status:** ✅ COMPLETED
**Started:** 2025-10-22
**Completed:** 2025-10-22

### Objectives
- [x] Create refactoring plan document
- [x] Remove on-demand download infrastructure (CacheMissEvent)
- [x] Simplify ComicManagementFacadeImpl
- [x] Extract repository interfaces
- [x] Clean up ComicCacher legacy code
- [x] Update STORAGE_DETAILS.md

### Tasks Completed

#### 1.1 Remove On-Demand Downloads ✅
- [x] Remove `CacheMissEvent` class (main and test)
- [x] Remove event publishing from `ComicStorageFacadeImpl`
- [x] Remove event listener from `ComicManagementFacadeImpl`
- [x] Updated tests that depend on on-demand behavior
- [x] Removed `CacheMissEvent` references from interfaces

**Commit:** `8782b1c` - Phase 1.1: Remove on-demand download infrastructure

#### 1.2 Extract Repository Interfaces ✅
- [x] Created `ComicRepository` interface with CRUD operations
- [x] Created `UserRepository` interface with authentication methods
- [x] Created `PreferenceRepository` interface with preference operations
- [x] Implemented `JsonComicRepository` delegating to `ConfigurationFacade`
- [x] Implemented `JsonUserRepository` delegating to `UserConfigWriter`
- [x] Implemented `JsonPreferenceRepository` delegating to `PreferenceConfigWriter`

**Commit:** `94f7936` - Phase 1.2: Extract repository interfaces for data persistence

#### 1.3 Clean Up Legacy Code ✅
- [x] Analyzed `ComicCacher` usage (used by DailyRunner)
- [x] Refactored `ComicCacher` to delegate to `ComicManagementFacade`
- [x] Marked `ComicCacher` as `@Deprecated` for clarity
- [x] Removed duplicate download/caching logic (156 lines removed)
- [x] Kept SSL setup and bootstrap methods for compatibility

**Commit:** `1bcb720` - Phase 1.3: Refactor ComicCacher to delegate to ComicManagementFacade

#### 1.4 Testing & Documentation ✅
- [x] All existing tests pass (verified)
- [x] Updated STORAGE_DETAILS.md with repository architecture
- [x] Documented removal of CacheMissEvent
- [x] Documented new access patterns for JSON files

**Commit:** (pending) - Phase 1.4: Update documentation

### Verification Criteria
- ✅ All existing tests pass
- ✅ No functional changes to end users
- ✅ On-demand downloads completely removed
- ✅ Repository interfaces defined and documented
- ✅ ComicCacher refactored to thin wrapper
- ✅ Documentation updated

### Summary

Phase 1 successfully completed all objectives:
1. Removed complexity of on-demand downloads (CacheMissEvent system)
2. Established clean repository abstractions for future flexibility
3. Refactored legacy ComicCacher to delegate to proper facades
4. Updated documentation to reflect architectural changes

**Next Step:** Begin Phase 2 - Extract Metrics Module

---

## Phase 2: Reorganize Metrics Package Structure

**Status:** ✅ COMPLETED
**Started:** 2025-10-22
**Completed:** 2025-10-22
**Duration:** ~4 hours

### Objectives Achieved
- [x] Reorganized metrics code into clean package structure
- [x] Renamed classes to follow consistent naming patterns
- [x] Created MetricsService facade interface
- [x] Implemented conditional metrics with NoOpMetricsService
- [x] All tests passing after reorganization

### Tasks Completed

#### 2.1-2.7 Package Reorganization ✅
- [x] Created `org.stapledon.metrics` package structure
- [x] Moved DTOs to `metrics.dto` package
- [x] Moved repositories to `metrics.repository` package
- [x] Moved services to `metrics.service` package
- [x] Created `metrics.config` with MetricsConfiguration
- [x] Renamed `ImageCacheStatsUpdater` → `StorageMetricsCollector`
- [x] Renamed `CacheUtils` → `AccessMetricsCollector`

#### 2.8-2.12 Service Layer & Testing ✅
- [x] Created `MetricsService` facade interface
- [x] Implemented `MetricsServiceImpl` with all operations
- [x] Implemented `NoOpMetricsService` for disabled state
- [x] Updated `MetricsController` to use new service
- [x] Fixed all test compilation errors
- [x] All tests passing (17 metrics tests + integration tests)

**Commits:**
- `57f021f` - Phase 2.1: Create metrics package structure and move DTOs
- `72395d5` - Phase 2.2: Move repository files to metrics.repository package
- `40d264f` - Phase 2.3: Move service files to metrics.service package
- `db566d0` - Phase 2.4: Move MetricsProperties to metrics.config package
- `58282e5` - Phase 2.5a: Rename ImageCacheStatsUpdater → StorageMetricsCollector
- `0cc98f9` - Phase 2.5b: Rename CacheUtils → AccessMetricsCollector
- `9737ba4` - Phase 2.6: Create MetricsService facade
- `6e5edd8` - Phase 2.7: Create MetricsConfiguration with conditional beans
- `3200140` - Phase 2.8: Update MetricsController to use MetricsService
- `14d0577` - Phase 2.9: Update tests for renamed metrics classes
- `851d6ff` - Phase 2.10: Fix test compilation errors (partial)
- `b93cca9` - Phase 2.11: Fix test method implementations for renamed classes
- `3734df8` - Phase 2.12: Fix MetricsControllerIT import paths

### Verification Criteria
- ✅ All code organized in `org.stapledon.metrics` package
- ✅ Consistent naming patterns across collectors and services
- ✅ MetricsService facade provides clean public API
- ✅ Conditional metrics work (can be disabled via config)
- ✅ All tests passing (no regressions)

### Summary

Phase 2 successfully reorganized metrics code:
1. Created clean package structure under `org.stapledon.metrics`
2. Established consistent naming patterns for collectors and services
3. Implemented facade pattern with MetricsService interface
4. Added conditional behavior via NoOpMetricsService
5. All tests passing, ready for module extraction

**Next Step:** Begin Phase 3 - Create Multi-Module Structure

---

## Phase 3: Create Multi-Module Structure

**Status:** ✅ COMPLETED
**Started:** 2025-10-22
**Completed:** 2025-10-22
**Duration:** ~4 hours

### Objectives
- Create separate Gradle modules for metrics and engine
- Establish clean module boundaries
- Enable independent module builds and testing
- Handle circular dependency challenges pragmatically

### Tasks

#### 3.1 Create comic-metrics Module ✅ COMPLETED
- [x] Created `comic-metrics/` directory and `build.gradle`
- [x] Copied metrics code from `org.stapledon.metrics` package
- [x] Moved metrics tests and test resources (17 tests)
- [x] Fixed package declarations and imports
- [x] All tests passing in comic-metrics module
- [x] Module builds independently

**Commits:**
- `1598ff8` - Phase 3.1a: Create comic-metrics Gradle module
- `c3329a9` - Phase 3.1b: Resolved circular dependency for comic-metrics

**Key Decision - Circular Dependency Handling:**

During extraction, discovered that `comic-metrics` depends on shared types from `ComicAPI`:
- DTOs: `ImageCacheStats`, `ComicItem`, `ComicStorageMetrics`
- Config: `CacheProperties`, `JsonConfigWriter`
- Infrastructure: `ComicStorageFacade`

**Attempted Solution:** Add `comic-metrics → ComicAPI` dependency
**Problem:** Created circular dependency (`ComicAPI → comic-metrics → ComicAPI`)

**Chosen Approach:** Temporary code duplication
- `comic-metrics` module: Standalone with copied metrics code
- `ComicAPI`: Retains original metrics code (no circular dependency)
- Both modules build and test independently
- **Future Resolution:** Phase 6 will extract shared types to `comic-common` module

**Rationale:**
- Pragmatic and incremental (avoid "big bang" refactoring)
- Allows immediate benefit (independent metrics module builds)
- Minimal risk (both code paths tested)
- Clear migration path (Phase 6 cleanup)

#### 3.2 Create comic-engine Module ✅ COMPLETED
- [x] Created `comic-engine/` directory and `build.gradle`
- [x] Copied engine code (downloaders, storage, caching, batch) - 22 files
- [x] Copied engine tests and resources - 4 test files
- [x] Fixed all package declarations
- [x] All tests passing in comic-engine module

**Commit:** `b001c6e` - Phase 3.2a: Create comic-engine Gradle module

**Components extracted:**
- `core.comic.downloader.*` → `engine.downloader` (13 files: downloaders, strategies, facades)
- `core.comic.management.*` → `engine.management` (2 files: ComicManagementFacade)
- `infrastructure.storage.*` → `engine.storage` (3 files: ComicStorageFacade, repository)
- `infrastructure.caching.*` → `engine.caching` (2 files: caching interfaces)
- `infrastructure.batch.*` → `engine.batch` (4 files: Spring Batch jobs)

**Same approach as comic-metrics:**
- Temporary code duplication (engine code in both modules and ComicAPI)
- Avoids circular dependency issues
- Both build independently
- Will be resolved in Phase 6 via comic-common

#### 3.3 Verification & Documentation ✅ COMPLETED
- [x] Both modules build independently: `:comic-metrics:build` ✅ `:comic-engine:build` ✅
- [x] ComicAPI builds and all tests pass ✅
- [x] Full project build successful ✅
- [x] Updated REFACTORING_PLAN.md with Phase 3 progress

### Verification Criteria
- ✅ comic-metrics module builds independently (DONE)
- ✅ comic-metrics tests pass (17/17) (DONE)
- ✅ comic-engine module builds independently (DONE)
- ✅ comic-engine tests pass (4/4) (DONE)
- ✅ ComicAPI builds and all tests pass (DONE)
- ✅ No functional regressions (DONE)

### Phase 3 Summary

Successfully created multi-module Gradle structure:

**Modules Created:**
1. `comic-metrics` - 15 source files, 17 tests ✅
2. `comic-engine` - 22 source files, 4 tests ✅
3. `ComicAPI` - Retains original code (41 tests passing)

**Total Changes:**
- 37 new module files
- 21 test files in modules
- All builds independent
- Zero test regressions

**Key Achievement:** Established clean module boundaries with pragmatic handling of shared dependencies. Both new modules can be built, tested, and potentially deployed independently.

**Next Step:** Phase 4 will create comic-common module to resolve circular dependencies, then Phase 5 will refactor ComicAPI to use the extracted modules

---

## Phase 4: Create comic-common Module

**Status:** ✅ COMPLETED
**Started:** 2025-10-22
**Completed:** 2025-10-22
**Duration:** ~3 hours

### Objectives Achieved
- [x] Created shared `comic-common` module with base types
- [x] Extracted DTOs, config classes, and interfaces used by multiple modules
- [x] Resolved circular dependency issues from Phase 3
- [x] Enabled clean module dependencies without duplication

### Tasks Completed

#### 4.1 Create Module Structure ✅
- [x] Created `comic-common/` directory and `build.gradle`
- [x] Updated `settings.gradle` to include comic-common
- [x] Created package structure: `org.stapledon.common`
- [x] Configured minimal dependencies (Lombok, Spring Boot basics)

#### 4.2 Extract Shared DTOs ✅
- [x] Moved `api.dto.comic.*` → `common.dto.*`
  - ComicItem, ImageCacheStats, ComicStorageMetrics (11 DTOs total)
  - RetrievalStatusDTO, UpdateStatusDTO, ImageDto
  - ComicConfig, ComicRetrievalRecord, DateRange
- [x] Fixed all package declarations and imports

#### 4.3 Extract Shared Config ✅
- [x] Moved `CacheProperties` → `common.config.*`
- [x] Removed duplicate CacheProperties from ComicAPI

#### 4.4 Extract Shared Interfaces ✅
- [x] Moved `ComicStorageFacade` → `common.service.*`
- [x] Moved `RetrievalStatusRepository` → `common.repository.*`

#### 4.5 Update Module Dependencies ✅
- [x] comic-common: Configured as base module with java-library plugin
- [x] comic-metrics: Added comic-common dependency (still has temp ComicAPI dep)
- [x] comic-engine: Added comic-common dependency (still has temp ComicAPI dep)
- [x] ComicAPI: Added comic-common dependency
- [x] Fixed all imports across all modules (~300+ import statements)

#### 4.6 Verification ✅
- [x] All modules build independently
- [x] All tests pass in all modules (comic-engine: 4, comic-metrics: 17, ComicAPI: 65+)
- [x] No circular dependencies
- [x] Clean dependency graph established

**Commits:**
- `601946c` - Reorder phases (Phase 6→4)
- `a27fa9a` - Phase 4.1-4.4: Create comic-common and extract shared code
- `eae0f79` - Phase 4.5: Update dependencies and fix imports
- `5a0c0ab` - Phase 4.6: Fix all test compilation errors

### Verification Criteria
- ✅ comic-common module builds independently (DONE)
- ✅ comic-metrics depends on comic-common (DONE - temp ComicAPI dep remains)
- ✅ comic-engine depends on comic-common (DONE - temp ComicAPI dep remains)
- ✅ No circular dependencies (DONE)
- ✅ All tests passing across all modules (DONE)

### Summary

Phase 4 successfully resolved the circular dependency issues from Phase 3:

**Module Structure Created:**
- `comic-common/`: 14 files (11 DTOs, 1 config, 2 interfaces)
- Base module with no project dependencies
- Uses java-library plugin for proper transitive dependency management

**Clean Dependency Graph:**
```
comic-common (base)
     ↑
     ├─── comic-metrics (+ temp ComicAPI dep)
     ├─── comic-engine (+ temp ComicAPI dep)
     └─── ComicAPI
```

**Key Achievement:** All modules now share common types through the comic-common base module, eliminating the circular dependency problem. Temporary ComicAPI dependencies in comic-metrics and comic-engine will be removed in Phase 5 when ComicAPI switches to use the extracted modules.

**Next Step:** Begin Phase 5 - Refactor ComicAPI to Use Modules

---

## Phase 5: Refactor ComicAPI to Use Modules

**Status:** 🟡 In Progress (Phase 5a Complete)
**Started:** 2025-10-22
**Estimated Duration:** 4-5 hours (split into 5a and 5b)

### Phase 5a: Extract Infrastructure to comic-common ✅ COMPLETED

**Objectives Achieved:**
- [x] Extract shared infrastructure classes to comic-common
- [x] Remove comic-metrics dependency on ComicAPI
- [x] Update comic-engine to use common infrastructure
- [x] Enable modules to build independently

### Tasks Completed

#### 5a.1 Extract Infrastructure Interfaces ✅
- [x] WebInspector interface → `common.infrastructure.web`
- [x] ICachable interface → `common.infrastructure.caching`
- [x] TaskExecutionTracker interface → `common.infrastructure.config`
- [x] StatsWriter interface (new) → `common.infrastructure.config`

#### 5a.2 Extract Infrastructure Implementations ✅
- [x] WebInspectorImpl → `common.infrastructure.web`
- [x] DefaultTrustManager → `common.infrastructure.web`
- [x] TaskExecutionTrackerImpl → `common.infrastructure.config`
- [x] JsonStatsWriter (new) → `common.infrastructure.config`

#### 5a.3 Extract Configuration Properties ✅
- [x] DailyRunnerProperties → `common.config.properties`
- [x] StartupReconcilerProperties → `common.config.properties`

#### 5a.4 Update Dependencies ✅
- [x] comic-common: Added jsoup dependency
- [x] comic-metrics: Removed ComicAPI dependency!
- [x] comic-engine: Updated to use common infrastructure (still needs ComicAPI for ConfigurationFacade)
- [x] Fixed all imports across modules

#### 5a.5 Verification ✅
- [x] comic-common builds successfully
- [x] comic-metrics: 17 tests pass (NO ComicAPI dependency!)
- [x] comic-engine: 4 tests pass
- [x] All modules compile

**Commits:**
- `88b8f5f` - Phase 5a.1-5a.2: Extract infrastructure to comic-common
- `6d6ed1b` - Phase 5a complete: Extract infrastructure to comic-common

**Module Structure After Phase 5a:**
```
comic-common (base)
  ├─ DTOs (11 files)
  ├─ Config (CacheProperties + 2 properties)
  ├─ Interfaces (ComicStorageFacade, RetrievalStatusRepository)
  └─ Infrastructure (WebInspector, TaskExecutionTracker, StatsWriter, etc.)
       ↑
       ├─── comic-metrics (NO ComicAPI dependency! ✨)
       ├─── comic-engine (temp ComicAPI dep for ConfigurationFacade)
       └─── ComicAPI (REST API + Auth + Scheduling)
```

---

### Phase 5b: Remove Duplicate Code from ComicAPI 🔴 NOT STARTED

**Objectives:**
- [ ] Add comic-metrics and comic-engine dependencies to ComicAPI
- [ ] Remove duplicated metrics code from ComicAPI
- [ ] Remove duplicated engine code from ComicAPI
- [ ] Update ComicAPI imports to use module classes
- [ ] Verify all tests pass

### Tasks

#### 5b.1 Add Module Dependencies
- [ ] Add comic-metrics dependency to ComicAPI
- [ ] Add comic-engine dependency to ComicAPI
- [ ] Verify modules are available

#### 5b.2 Remove Duplicated Code
- [ ] Remove `org.stapledon.metrics.*` (15 files - now in comic-metrics)
- [ ] Remove `org.stapledon.core.comic.downloader.*` (13 files - now in comic-engine)
- [ ] Remove `org.stapledon.core.comic.management.*` (2 files - now in comic-engine)
- [ ] Remove `org.stapledon.infrastructure.batch.*` (4 files - now in comic-engine)
- [ ] Remove `org.stapledon.infrastructure.storage.*` (2 files - keep JsonRetrievalStatusRepository)
- [ ] Keep API-specific code (controllers, auth, user, preferences, scheduling)

#### 5b.3 Update Imports
- [ ] Update controllers to import from comic-metrics/comic-engine
- [ ] Update DailyRunner to use engine module classes
- [ ] Update StartupReconciler to use engine module classes
- [ ] Update all tests (unit + integration)

#### 5b.4 Verification
- [ ] All ComicAPI tests pass
- [ ] Integration tests pass
- [ ] No compilation errors
- [ ] Full build successful

### Verification Criteria
- ✅ ComicAPI uses comic-metrics module
- ✅ ComicAPI uses comic-engine module
- ✅ No code duplication
- ✅ All tests passing

---

## Phase 6: Clean Up & Verification

**Status:** 🔴 Not Started
**Estimated Duration:** 1-2 hours

### Objectives
- Final cleanup of any remaining duplication
- Verify clean module boundaries
- Performance testing
- Documentation updates

### Tasks

#### 6.1 Final Cleanup
- [ ] Remove any remaining duplicated code
- [ ] Clean up unused imports
- [ ] Verify no dead code
- [ ] Update package documentation

#### 6.2 Verification
- [ ] Full build passes: `./gradlew build`
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Performance regression testing

#### 6.3 Documentation
- [ ] Update module READMEs
- [ ] Update architecture documentation
- [ ] Document dependency graph
- [ ] Update CLAUDE.md

### Verification Criteria
- ✅ Clean module boundaries
- ✅ No duplication
- ✅ All tests passing
- ✅ Documentation complete

---

## Phase 7: Refactor API Controllers (Deferred from original Phase 4)

**Status:** 🔴 Not Started
**Estimated Duration:** 2-3 days

### Objectives
- Restructure API layer controllers
- Clean up auth/user coupling
- Remove redundant orchestration layers
- Simplify controller dependencies

### Tasks

#### 7.1 Update Controllers
- [ ] Refactor `ComicController` to use engine services directly
- [ ] Refactor `UpdateController`
- [ ] Refactor `RetrievalStatusController`
- [ ] Remove `ComicManagementFacadeImpl` (if redundant)
- [ ] Update tests

#### 7.2 Clean Up Auth/User Layer
- [ ] Update `JwtUserDetailsService` to use repository
- [ ] Refactor `UserConfigWriter`
- [ ] Clean up coupling to JSON files
- [ ] Update auth tests

#### 7.3 Simplify Orchestration
- [ ] Remove redundant facade layers
- [ ] Direct controller → service calls
- [ ] Update dependency injection
- [ ] Clean up unused code

### Verification Criteria
- ✅ All REST endpoints functional
- ✅ Authentication works correctly
- ✅ User management operational
- ✅ All API tests pass

---

## Phase 8: Scheduling & Batch Jobs (Deferred from original Phase 5)

**Status:** 🔴 Not Started
**Estimated Duration:** 2 days

### Objectives
- Refactor scheduling components
- Clean up batch job dependencies
- Ensure scheduling uses modules correctly
- Add manual trigger endpoints

### Tasks

#### 8.1 Refactor Scheduling
- [ ] Review `DailyRunner` dependencies
- [ ] Review `StartupReconciler` dependencies
- [ ] Update to call engine module methods
- [ ] Extract scheduling configuration

#### 8.2 Batch Job Cleanup
- [ ] Review `BatchJobController` dependencies
- [ ] Ensure batch jobs use engine module
- [ ] Add progress tracking
- [ ] Update tests

#### 8.3 Manual Triggers
- [ ] Keep `UpdateController` for manual updates
- [ ] Add batch job status endpoints
- [ ] Document API endpoints

### Verification Criteria
- ✅ Scheduled updates work correctly
- ✅ Manual updates work correctly
- ✅ Batch jobs complete successfully
- ✅ All scheduling tests pass

---

## Phase 9: Integration & Documentation (Deferred from original Phase 7)

**Status:** 🔴 Not Started
**Estimated Duration:** 1-2 days

### Objectives
- Comprehensive integration testing
- Update all documentation
- Create architecture diagrams
- Performance testing

### Tasks

#### 7.1 Integration Testing
- [ ] Cross-module integration tests
- [ ] End-to-end workflow tests
- [ ] Performance regression tests
- [ ] Load testing

#### 7.2 Documentation Updates
- [ ] Update `CLAUDE.md` with module structure
- [ ] Update `README.md` with build instructions
- [ ] Create `comic-api/README.md`
- [ ] Create `comic-engine/README.md`
- [ ] Create `comic-metrics/README.md`
- [ ] Create `comic-common/README.md`

#### 7.3 Architecture Documentation
- [ ] Create architecture diagrams
- [ ] Document module boundaries
- [ ] Document dependency flow
- [ ] Create migration guide (if needed)

#### 7.4 Final Verification
- [ ] All tests pass
- [ ] No performance regressions
- [ ] Documentation complete
- [ ] Create final release tag

### Verification Criteria
- ✅ All integration tests pass
- ✅ Documentation comprehensive
- ✅ Performance acceptable
- ✅ Ready for production

---

## Success Criteria (Overall Project)

1. ✅ Clean module boundaries with minimal coupling
2. ✅ Metrics module can switch output formats (JSON → Victoria Metrics)
3. ✅ Engine module can be tested independently
4. ✅ All existing functionality preserved
5. ✅ No on-demand downloads (simplified architecture)
6. ✅ Auth/User integrated with API but cleanly abstracted
7. ✅ All tests pass at each phase
8. ✅ Comprehensive documentation

---

## Rollback Strategy

Each phase is committed and tagged independently:
- Tag format: `refactor-phase-N`
- Can roll back to any previous phase
- Each phase is independently testable

## Git Tags

- `refactor-phase-1`: Foundation & Cleanup complete (all commits on `refactor-phase-1-foundation` branch)
- `refactor-phase-2`: Metrics package reorganization (12 commits, all tests passing)
- `refactor-phase-3-partial`: Multi-module structure - comic-metrics created (in progress)
- `refactor-phase-3-complete`: Multi-module structure - both metrics and engine modules
- `refactor-phase-4`: comic-common module created (resolves circular dependencies)
- `refactor-phase-5`: ComicAPI refactored to use modules (code duplication removed)
- `refactor-phase-6`: Final cleanup and verification
- `refactor-phase-7`: API controllers refactored
- `refactor-phase-8`: Scheduling refactored
- `refactor-phase-9`: Integration complete

---

## Notes & Decisions

### Design Decisions

1. **Metrics as Separate Module**: Allows future pluggability (Victoria Metrics, etc.)
2. **Keep Auth in API Layer**: Simple file-based auth doesn't need separate module
3. **Remove On-Demand Downloads**: Simplifies architecture, serves only from JSON
4. **Incremental Approach**: 7 phases with testing between each
5. **Common Module**: Shared code reduces duplication across modules
6. **Temporary Code Duplication (Phase 3)**: During module extraction, keep code in both original location and new module to avoid circular dependencies. This pragmatic approach:
   - Allows independent module builds immediately
   - Avoids "big bang" refactoring risk
   - Maintains all tests passing throughout
   - Will be resolved in Phase 6 when `comic-common` extracts shared types
7. **Phase 2 Before Phase 3**: Reorganized package structure within monolith before creating modules. This made module extraction cleaner and easier to verify.

### Open Questions

- None currently

### Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking existing functionality | Comprehensive tests after each phase |
| Module dependency cycles | Careful interface design, clear boundaries |
| JSON file corruption | Add file locking in Phase 6 |
| Performance regressions | Performance testing in Phase 7 |

---

## References

- [CLAUDE.md](./CLAUDE.md) - Project overview and standards
- [STORAGE_DETAILS.md](./STORAGE_DETAILS.md) - JSON storage documentation
- [ComicAPI/CLAUDE_MEMORY.md](./ComicAPI/CLAUDE_MEMORY.md) - Coding standards

---

Last Updated: 2025-10-22
Status: Phase 5a Complete - Ready for Phase 5b

## Important Notes

### Circular Dependency Resolution Strategy

**Challenge:** During Phase 3 module extraction, we discovered that new modules depend on shared types still in ComicAPI (DTOs, config classes, infrastructure interfaces). Adding module dependencies created circular references.

**Current Approach (Phase 3):**
- Modules contain copies of extracted code
- ComicAPI retains original code (no circular dependency)
- Both code paths tested and working
- All builds pass independently

**Future Resolution (Phase 6):**
- Extract shared types to `comic-common` module
- Update all modules to depend on `comic-common`
- Remove duplicated code from modules
- Final clean architecture with proper dependencies

This incremental approach prioritizes:
1. **Safety**: All tests pass at every step
2. **Progress**: Modules can be built and tested immediately
3. **Reversibility**: Easy to roll back if needed
4. **Clarity**: Clear path to final architecture
