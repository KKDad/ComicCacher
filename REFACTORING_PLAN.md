# ComicAPI Modularization - Refactoring Plan

## Status: Phase 1 - Foundation & Cleanup (✅ COMPLETED)

Last Updated: 2025-10-22
Completed: 2025-10-22

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

## Phase 2: Extract Metrics Module

**Status:** 🔴 Not Started
**Estimated Duration:** 2-3 days

### Objectives
- Create new Gradle submodule: `comic-metrics`
- Move all metrics-related code to new module
- Define pluggable metrics output interface
- Make metrics scheduling optional

### Tasks

#### 2.1 Create Metrics Module Structure
- [ ] Create `comic-metrics/` directory
- [ ] Create `comic-metrics/build.gradle`
- [ ] Set up module dependencies
- [ ] Create package structure

#### 2.2 Move Metrics Components
- [ ] Move `ImageCacheStatsUpdater`
- [ ] Move `CacheUtils`
- [ ] Move `MetricsUpdateService`
- [ ] Move `MetricsController`
- [ ] Move all metrics DTOs
- [ ] Move all metrics repositories

#### 2.3 Define Metrics Interfaces
- [ ] Create `MetricsCollector` interface
- [ ] Implement `JsonMetricsCollector`
- [ ] Create `MetricsProvider` interface
- [ ] Document metrics plugin architecture

#### 2.4 Configuration & Properties
- [ ] Add metrics enable/disable property
- [ ] Make scheduling optional
- [ ] Update configuration files
- [ ] Update tests

### Verification Criteria
- ✅ Metrics module builds independently
- ✅ Metrics can be disabled via properties
- ✅ JSON output unchanged when enabled
- ✅ All metrics tests pass

---

## Phase 3: Extract Comic Engine Module

**Status:** 🔴 Not Started
**Estimated Duration:** 3-4 days

### Objectives
- Create new Gradle submodule: `comic-engine`
- Move download, cache, and storage components
- Define clean public API
- Ensure engine can be used standalone

### Tasks

#### 3.1 Create Engine Module Structure
- [ ] Create `comic-engine/` directory
- [ ] Create `comic-engine/build.gradle`
- [ ] Set up module dependencies
- [ ] Create package structure

#### 3.2 Move Engine Components
- [ ] Move all downloader strategies
- [ ] Move `ComicDownloaderFacade`
- [ ] Move `ComicStorageFacade`
- [ ] Move `ComicCacher`
- [ ] Move caching infrastructure
- [ ] Move `RetrievalStatusService`
- [ ] Move `TaskExecutionTracker`

#### 3.3 Define Public API
- [ ] Create `ComicEngineService` interface
- [ ] Create `ComicStorageService` interface
- [ ] Create `ComicDownloadService` interface
- [ ] Implement service facades
- [ ] Document public API

#### 3.4 Configuration & Properties
- [ ] Move `CacheProperties`
- [ ] Move bootstrap classes
- [ ] Update configuration loading
- [ ] Update tests

### Verification Criteria
- ✅ Engine module builds independently
- ✅ Engine can be used without API layer
- ✅ All engine tests pass
- ✅ Download and caching work correctly

---

## Phase 4: Refactor API Module

**Status:** 🔴 Not Started
**Estimated Duration:** 2-3 days

### Objectives
- Restructure API layer to use engine module
- Clean up auth/user coupling
- Remove redundant orchestration layer
- Simplify controller dependencies

### Tasks

#### 4.1 Update Controllers
- [ ] Refactor `ComicController` to use `ComicEngineService`
- [ ] Refactor `UpdateController`
- [ ] Refactor `RetrievalStatusController`
- [ ] Remove `ComicManagementFacadeImpl` (if redundant)
- [ ] Update tests

#### 4.2 Clean Up Auth/User Layer
- [ ] Implement `JsonUserRepository`
- [ ] Update `JwtUserDetailsService` to use repository
- [ ] Refactor `UserConfigWriter`
- [ ] Clean up coupling to JSON files
- [ ] Update auth tests

#### 4.3 Simplify Orchestration
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

## Phase 5: Scheduling & Batch Jobs

**Status:** 🔴 Not Started
**Estimated Duration:** 2 days

### Objectives
- Move scheduling to API layer
- Create batch job service in engine
- Add manual trigger endpoints
- Clean separation of concerns

### Tasks

#### 5.1 Refactor Scheduling
- [ ] Move `DailyRunner` to API layer
- [ ] Move `StartupReconciler` to API layer
- [ ] Update to call `ComicEngineService` methods
- [ ] Extract scheduling configuration

#### 5.2 Create Batch Service
- [ ] Create `BatchJobService` in engine module
- [ ] Implement batch operations
- [ ] Add progress tracking
- [ ] Update tests

#### 5.3 Manual Triggers
- [ ] Keep `UpdateController` for manual updates
- [ ] Add batch job status endpoints
- [ ] Document API endpoints

### Verification Criteria
- ✅ Scheduled updates work correctly
- ✅ Manual updates work correctly
- ✅ Batch jobs complete successfully
- ✅ All scheduling tests pass

---

## Phase 6: Configuration & Properties

**Status:** 🔴 Not Started
**Estimated Duration:** 2 days

### Objectives
- Create shared `comic-common` module
- Consolidate configuration management
- Add file locking for concurrent writes
- Clean up JSON persistence

### Tasks

#### 6.1 Create Common Module
- [ ] Create `comic-common/` directory
- [ ] Create `comic-common/build.gradle`
- [ ] Move shared DTOs
- [ ] Move common utilities
- [ ] Move configuration properties

#### 6.2 Consolidate Configuration
- [ ] Create `ComicConfigPersistence`
- [ ] Create `UserConfigPersistence`
- [ ] Create `PreferenceConfigPersistence`
- [ ] Add file locking mechanism
- [ ] Update all modules to use common

#### 6.3 Clean Up JSON Writers
- [ ] One writer per concern
- [ ] Remove duplication
- [ ] Add concurrent write protection
- [ ] Update tests

### Verification Criteria
- ✅ Common module builds independently
- ✅ No configuration regressions
- ✅ File locking prevents corruption
- ✅ All configuration tests pass

---

## Phase 7: Integration & Documentation

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

- `refactor-phase-1`: Foundation & Cleanup complete
- `refactor-phase-2`: Metrics module extracted
- `refactor-phase-3`: Engine module extracted
- `refactor-phase-4`: API module refactored
- `refactor-phase-5`: Scheduling refactored
- `refactor-phase-6`: Configuration consolidated
- `refactor-phase-7`: Integration complete

---

## Notes & Decisions

### Design Decisions

1. **Metrics as Separate Module**: Allows future pluggability (Victoria Metrics, etc.)
2. **Keep Auth in API Layer**: Simple file-based auth doesn't need separate module
3. **Remove On-Demand Downloads**: Simplifies architecture, serves only from JSON
4. **Incremental Approach**: 7 phases with testing between each
5. **Common Module**: Shared code reduces duplication across modules

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
Status: Phase 1 In Progress
