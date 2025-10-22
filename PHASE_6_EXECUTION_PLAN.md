# Phase 6: Break Circular Dependency - Complete Execution Plan

**Date:** 2025-10-22
**Status:** Ready to Execute
**Confidence:** HIGH - Clear path identified, no blockers

---

## Executive Summary

### Problem
- comic-engine depends on ComicAPI (creates circular dependency)
- ComicAPI cannot use comic-engine module without circularity
- Code is duplicated between both modules

### Root Cause Analysis
comic-engine imports 3 classes from ComicAPI:
1. **ConfigurationFacade** - Used to load ComicConfig and Bootstrap
2. **RetrievalStatusService** - Used to record retrieval results
3. **IComicsBootstrap** - Used to configure comic downloaders

### Solution
Extract these 3 dependencies to comic-common, enabling:
```
comic-common (base)
     ↑
     ├─── comic-metrics (✅ independent)
     ├─── comic-engine (✅ independent - no ComicAPI dependency)
     └─── ComicAPI (uses both comic-metrics and comic-engine)
```

### Key Insight
ConfigurationFacade contains both comic-related methods (used by engine) AND user/preference methods (API-only). We will SPLIT this interface.

---

## Dependency Analysis Results

### Files Affected in comic-engine
1. `ComicManagementFacadeImpl.java` - Uses: ConfigurationFacade, RetrievalStatusService, IComicsBootstrap
2. `ComicDownloaderFacadeImpl.java` - Uses: RetrievalStatusService
3. `ComicCacher.java` - Uses: IComicsBootstrap
4. `ComicRetrievalJobConfig.java` - Uses: ConfigurationFacade

### What comic-engine Actually Uses

**ConfigurationFacade usage:**
- `loadComicConfig()` → returns ComicConfig (✓ in common)
- `saveComicConfig(config)` → takes ComicConfig (✓ in common)
- `loadBootstrapConfig()` → returns Bootstrap (⚠️ needs extraction)
- Does NOT use: loadUserConfig, loadPreferenceConfig (API-specific)

**RetrievalStatusService usage:**
- `recordRetrievalResult(record)` → takes ComicRetrievalRecord (✓ in common)
- Clean interface, no API-specific dependencies

**IComicsBootstrap usage:**
- Interface for comic bootstrap configurations
- Depends on: IDailyComic (✓ already in comic-engine)

---

## Extraction Plan - 4 Waves

### Wave 1: Utilities & Exceptions (ZERO dependencies)
**Risk: LOW** - Simple value objects

#### 1.1 Extract Direction enum
- **From:** `ComicAPI/src/main/java/org/stapledon/common/util/Direction.java`
- **To:** `comic-common/src/main/java/org/stapledon/common/util/Direction.java`
- **Dependencies:** None
- **Action:** Copy file, update package if needed

#### 1.2 Extract ImageUtils
- **From:** `ComicAPI/src/main/java/org/stapledon/common/util/ImageUtils.java`
- **To:** `comic-common/src/main/java/org/stapledon/common/util/ImageUtils.java`
- **Dependencies:** Check for Spring dependencies
- **Action:** Copy file, verify dependencies

#### 1.3 Extract Exception classes
- **From:** `ComicAPI/src/main/java/org/stapledon/core/comic/model/`
  - `ComicCachingException.java`
  - `ComicImageNotFoundException.java`
  - `ComicNotFoundException.java`
- **To:** `comic-common/src/main/java/org/stapledon/common/model/`
- **Action:** Copy files, update package declarations

#### 1.4 Build and verify
```bash
./gradlew :comic-common:build
```

---

### Wave 2: Bootstrap Infrastructure
**Risk: MEDIUM** - IComicsBootstrap has default methods with instanceof checks

#### 2.1 Extract IComicsBootstrap interface
- **From:** `ComicAPI/src/main/java/org/stapledon/infrastructure/config/IComicsBootstrap.java`
- **To:** `comic-common/src/main/java/org/stapledon/common/config/IComicsBootstrap.java`
- **Dependencies:**
  - `IDailyComic` - Already in comic-engine, need to reference correctly
- **Special handling:** Default method uses `instanceof` checks for GoComics/ComicsKingdom
  - These are implementation details, interface can stay generic
  - Or: Remove default method, make abstract

**Decision: Keep default methods AS-IS** - They reference engine classes, which is acceptable for an interface in common that's used by engine.

#### 2.2 Extract Bootstrap class
- **From:** `ComicAPI/src/main/java/org/stapledon/common/util/Bootstrap.java`
- **To:** `comic-common/src/main/java/org/stapledon/common/util/Bootstrap.java`
- **Dependencies:**
  - `IComicsBootstrap` (✓ moved in step 2.1)
  - `ComicConfig` (✓ already in common)
  - `ComicItem` (✓ already in common)

#### 2.3 Update imports in ComicAPI
- `GoComicsBootstrap` - Update to use `common.config.IComicsBootstrap`
- `KingComicsBootStrap` - Update to use `common.config.IComicsBootstrap`
- Any other files importing Bootstrap or IComicsBootstrap

#### 2.4 Build and verify
```bash
./gradlew :comic-common:build
./gradlew :ComicAPI:compileJava
```

---

### Wave 3: Service Interfaces
**Risk: LOW** - Pure interface with common DTOs

#### 3.1 Extract RetrievalStatusService
- **From:** `ComicAPI/src/main/java/org/stapledon/core/comic/service/RetrievalStatusService.java`
- **To:** `comic-common/src/main/java/org/stapledon/common/service/RetrievalStatusService.java`
- **Dependencies:**
  - `ComicRetrievalRecord` (✓ already in common)
  - `ComicRetrievalStatus` (✓ already in common)

#### 3.2 Keep implementation in ComicAPI
- **File:** `RetrievalStatusServiceImpl.java` (stays in ComicAPI)
- **Action:** Update to implement `common.service.RetrievalStatusService`

#### 3.3 Update imports in ComicAPI
- Files importing RetrievalStatusService → use `common.service.RetrievalStatusService`

#### 3.4 Build and verify
```bash
./gradlew :comic-common:build
./gradlew :ComicAPI:compileJava
```

---

### Wave 4: Configuration Facade Split
**Risk: MEDIUM** - Interface refactoring, multiple implementations

#### 4.1 Create ComicConfigurationService interface
- **Location:** `comic-common/src/main/java/org/stapledon/common/service/ComicConfigurationService.java`
- **Content:**
```java
package org.stapledon.common.service;

import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.util.Bootstrap;
import java.io.File;

/**
 * Service interface for comic and bootstrap configuration operations.
 * This interface contains ONLY comic-related config (no user/preference).
 */
public interface ComicConfigurationService {
    // Comic configuration methods
    ComicConfig loadComicConfig();
    boolean saveComicConfig(ComicConfig config);

    // Bootstrap configuration
    Bootstrap loadBootstrapConfig();
    boolean saveBootstrapConfig(Bootstrap config);

    // Configuration utility methods
    String getConfigPath(String configName);
    boolean configExists(String configName);
    File getConfigFile(String configName);
}
```

#### 4.2 Create ApplicationConfigurationFacade (extends ComicConfigurationService)
- **Location:** `ComicAPI/src/main/java/org/stapledon/infrastructure/config/ApplicationConfigurationFacade.java`
- **Content:**
```java
package org.stapledon.infrastructure.config;

import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.api.dto.preference.PreferenceConfig;
import org.stapledon.api.dto.user.UserConfig;

/**
 * Full configuration facade including user and preference config.
 * Extends ComicConfigurationService to include API-specific methods.
 */
public interface ApplicationConfigurationFacade extends ComicConfigurationService {
    // User configuration
    UserConfig loadUserConfig();
    boolean saveUserConfig(UserConfig config);

    // Preference configuration
    PreferenceConfig loadPreferenceConfig();
    boolean savePreferenceConfig(PreferenceConfig config);
}
```

#### 4.3 Update ConfigurationFacadeImpl
- **Action:** Change to implement `ApplicationConfigurationFacade` (which extends ComicConfigurationService)
- **File:** `ComicAPI/src/main/java/org/stapledon/infrastructure/config/ConfigurationFacadeImpl.java`
- **Change:** `implements ConfigurationFacade` → `implements ApplicationConfigurationFacade`

#### 4.4 Rename ConfigurationFacade → ApplicationConfigurationFacade
- **Action:** Rename the OLD interface file
- **From:** `ConfigurationFacade.java`
- **To:** Delete (replaced by ApplicationConfigurationFacade)

#### 4.5 Update all imports in ComicAPI
- Controllers, services, etc. using ConfigurationFacade
- **Change:** `ConfigurationFacade` → `ApplicationConfigurationFacade`
- **Note:** Most will continue to use the full interface (no changes needed to behavior)

#### 4.6 Build and verify
```bash
./gradlew :comic-common:build
./gradlew :ComicAPI:compileJava
./gradlew :ComicAPI:test
```

---

## Phase 6.3: Update comic-engine

### 3.1 Update imports in comic-engine files

**File 1: ComicManagementFacadeImpl.java**
```java
// OLD imports:
import org.stapledon.core.comic.downloader.ComicDownloaderFacade;
import org.stapledon.core.comic.service.RetrievalStatusService;
import org.stapledon.infrastructure.config.ConfigurationFacade;
import org.stapledon.infrastructure.config.IComicsBootstrap;

// NEW imports:
import org.stapledon.engine.downloader.ComicDownloaderFacade;  // Already correct!
import org.stapledon.common.service.RetrievalStatusService;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.common.config.IComicsBootstrap;
```

**File 2: ComicDownloaderFacadeImpl.java**
```java
// OLD:
import org.stapledon.core.comic.service.RetrievalStatusService;

// NEW:
import org.stapledon.common.service.RetrievalStatusService;
```

**File 3: ComicCacher.java**
```java
// OLD:
import org.stapledon.infrastructure.config.IComicsBootstrap;

// NEW:
import org.stapledon.common.config.IComicsBootstrap;
```

**File 4: ComicRetrievalJobConfig.java**
```java
// OLD:
import org.stapledon.core.comic.downloader.ComicDownloaderFacade;
import org.stapledon.infrastructure.config.ConfigurationFacade;

// NEW:
import org.stapledon.engine.downloader.ComicDownloaderFacade;  // Already correct!
import org.stapledon.common.service.ComicConfigurationService;
```

### 3.2 Update field types in classes

**In ComicManagementFacadeImpl:**
```java
// OLD:
private final ConfigurationFacade configFacade;

// NEW:
private final ComicConfigurationService configFacade;
```

**In ComicRetrievalJobConfig:**
```java
// OLD:
private final ConfigurationFacade configurationFacade;

// NEW:
private final ComicConfigurationService configurationFacade;
```

### 3.3 Remove ComicAPI dependency from build.gradle

**File:** `comic-engine/build.gradle`

```gradle
// REMOVE this line:
implementation project(':ComicAPI')

// Keep:
implementation project(':comic-common')
```

### 3.4 Build comic-engine independently
```bash
./gradlew :comic-engine:clean :comic-engine:build
```

**Expected:** ✅ SUCCESS (no compilation errors)

**If FAILS:** STOP and analyze errors with developer

---

## Phase 6.4: Update ComicAPI

### 4.1 Remove extracted interface files
**Delete these files from ComicAPI** (implementations stay):
- `core/comic/service/RetrievalStatusService.java` (interface only)
- `infrastructure/config/ConfigurationFacade.java` (replaced by ApplicationConfigurationFacade)
- `infrastructure/config/IComicsBootstrap.java` (moved to common)
- `common/util/Bootstrap.java` (moved to common)
- `common/util/Direction.java` (moved to common)
- `common/util/ImageUtils.java` (moved to common)
- `core/comic/model/*Exception.java` (moved to common)

### 4.2 Update imports throughout ComicAPI
Use find/replace for bulk updates:

```bash
# RetrievalStatusService
find ComicAPI/src -name "*.java" -exec sed -i '' \
  's/org\.stapledon\.core\.comic\.service\.RetrievalStatusService/org.stapledon.common.service.RetrievalStatusService/g' {} \;

# IComicsBootstrap
find ComicAPI/src -name "*.java" -exec sed -i '' \
  's/org\.stapledon\.infrastructure\.config\.IComicsBootstrap/org.stapledon.common.config.IComicsBootstrap/g' {} \;

# Bootstrap
find ComicAPI/src -name "*.java" -exec sed -i '' \
  's/org\.stapledon\.common\.util\.Bootstrap/org.stapledon.common.util.Bootstrap/g' {} \;

# Direction (if needed)
# ImageUtils (if needed)
# Exceptions (if needed)
```

### 4.3 Build and test ComicAPI
```bash
./gradlew :ComicAPI:clean :ComicAPI:build
./gradlew :ComicAPI:test
```

**Expected:** ✅ All tests pass

---

## Phase 6.5: THE MOMENT OF TRUTH - Add comic-engine Dependency

### 5.1 Update ComicAPI build.gradle

**File:** `ComicAPI/build.gradle`

```gradle
dependencies {
    // Module dependencies
    implementation project(':comic-common')
    implementation project(':comic-metrics')
    implementation project(':comic-engine')  // ← ADD THIS LINE

    // ... rest of dependencies
}
```

### 5.2 Attempt build
```bash
./gradlew clean build -x integrationTest
```

### 5.3 Outcome Analysis

#### ✅ OUTCOME A: SUCCESS
**Build completes with no errors**

**Action:** Proceed to Phase 6.6 (remove duplication)

#### ❌ OUTCOME B: Circular Dependency Error
**Gradle reports:** `Circular dependency between :ComicAPI and :comic-engine`

**Action:** STOP and analyze with developer

**Questions to investigate:**
1. What specific class/interface creates the cycle?
2. Run: `./gradlew :comic-engine:dependencies --configuration compileClasspath`
3. Check: Does comic-engine still import something from ComicAPI?
4. Check: Did we miss extracting an interface?
5. Is there an IMPLEMENTATION (not interface) that comic-engine needs?

**Document findings and discuss with developer before proceeding.**

#### ❌ OUTCOME C: Compilation Errors (but no circular dependency)
**Build fails with missing classes**

**Action:** Identify what's missing
1. List all compilation errors
2. Categorize: Missing from common? Wrong package?
3. Fix systematically
4. This suggests extraction was incomplete

---

## Phase 6.6: Remove Code Duplication (ONLY if 6.5 succeeds)

### 6.1 Remove duplicated engine code from ComicAPI

**Delete these directories:**
```bash
rm -rf ComicAPI/src/main/java/org/stapledon/core/comic/downloader
rm -rf ComicAPI/src/main/java/org/stapledon/core/comic/management
rm -rf ComicAPI/src/main/java/org/stapledon/infrastructure/batch
rm ComicAPI/src/main/java/org/stapledon/infrastructure/storage/ComicStorageFacadeImpl.java
rm ComicAPI/src/main/java/org/stapledon/infrastructure/storage/ComicStorageFacade.java
```

**Note:** Keep JsonRetrievalStatusRepository (API-specific)

### 6.2 Update imports in ComicAPI to use comic-engine

**Files to update:**
- Controllers (ComicController, UpdateController, BatchJobController)
- Services (ComicsServiceImpl, UpdateServiceImpl)
- Scheduling (DailyRunner, StartupReconcilerImpl)
- Config (ComicDownloaderConfig)

**Import changes:**
```java
// OLD:
import org.stapledon.core.comic.downloader.*;
import org.stapledon.core.comic.management.*;
import org.stapledon.infrastructure.batch.*;
import org.stapledon.infrastructure.storage.ComicStorageFacadeImpl;

// NEW:
import org.stapledon.engine.downloader.*;
import org.stapledon.engine.management.*;
import org.stapledon.engine.batch.*;
import org.stapledon.engine.storage.ComicStorageFacadeImpl;
```

### 6.3 Build and test
```bash
./gradlew clean build
./gradlew test integrationTest
```

### 6.4 Verify no duplication
```bash
# Check no engine code remains in ComicAPI
find ComicAPI/src/main/java -name "*Downloader*.java" -o -name "*Management*.java"
# Should return empty or only API-specific files
```

---

## Phase 6.7: Documentation & Verification

### 7.1 Update REFACTORING_PLAN.md
- Mark Phase 6 as complete
- Document final architecture
- Note any remaining technical debt

### 7.2 Update CLAUDE.md
- Document new module structure
- Update dependency graph
- Add build commands for each module

### 7.3 Create architecture diagram
Document final structure:
```
comic-common (DTOs, config, services, utilities)
     ↑
     ├─── comic-metrics (independent - metrics collection)
     ├─── comic-engine (independent - download, storage, batch)
     │         ↑
     └─── ComicAPI (REST API, auth, scheduling)
              Uses: comic-metrics, comic-engine
```

### 7.4 Run full verification
```bash
# Build all modules independently
./gradlew :comic-common:build
./gradlew :comic-metrics:build
./gradlew :comic-engine:build
./gradlew :ComicAPI:build

# Full build
./gradlew clean build

# All tests
./gradlew test integrationTest
```

### 7.5 Git commit
```bash
git add -A
git commit -m "Phase 6: Break circular dependency - extract shared interfaces to comic-common

- Extracted RetrievalStatusService, IComicsBootstrap to common
- Split ConfigurationFacade into ComicConfigurationService (common) and ApplicationConfigurationFacade (API)
- Extracted Bootstrap, Direction, ImageUtils, exceptions to common
- comic-engine now depends ONLY on comic-common
- ComicAPI successfully uses comic-engine module
- Removed all code duplication
- All tests passing (86+ tests across all modules)

🎉 Generated with Claude Code"
```

---

## Success Criteria

### Minimum Success
- ✅ All interfaces extracted to comic-common
- ✅ comic-engine builds independently (no ComicAPI dependency)
- ✅ All tests pass in all modules

### Full Success
- ✅ ComicAPI uses comic-engine as a module dependency
- ✅ No circular dependencies
- ✅ No code duplication
- ✅ Clean architecture achieved

---

## Rollback Plan

If Phase 6.5 fails (circular dependency persists):

1. **Do NOT proceed with Phase 6.6**
2. Keep current state:
   - Interfaces extracted to common ✓
   - comic-engine still has temp ComicAPI dep
   - Code remains duplicated
3. Document findings in REFACTORING_PLAN.md
4. Mark Phase 6 as "Partial Success - Interfaces Extracted"
5. Schedule architectural discussion

**Rollback command:**
```bash
git checkout -- comic-engine/build.gradle  # Remove comic-engine dep from ComicAPI
```

---

## Estimated Timeline

| Phase | Task | Time | Risk |
|-------|------|------|------|
| 6.2 Wave 1 | Extract utilities & exceptions | 30 min | LOW |
| 6.2 Wave 2 | Extract bootstrap infrastructure | 1 hour | MED |
| 6.2 Wave 3 | Extract service interfaces | 30 min | LOW |
| 6.2 Wave 4 | Split configuration facade | 1-2 hours | MED |
| 6.3 | Update comic-engine | 30 min | LOW |
| 6.4 | Update ComicAPI | 1 hour | LOW |
| 6.5 | Test integration | 15 min - 2 hours* | HIGH |
| 6.6 | Remove duplication | 1 hour | LOW |
| 6.7 | Documentation | 30 min | LOW |

**Total: 6-9 hours**

*Phase 6.5 time varies: 15 min if success, 2+ hours if circular dependency issues require investigation

---

## Key Files Reference

### Files to Create
1. `comic-common/src/main/java/org/stapledon/common/service/ComicConfigurationService.java`
2. `comic-common/src/main/java/org/stapledon/common/service/RetrievalStatusService.java`
3. `comic-common/src/main/java/org/stapledon/common/config/IComicsBootstrap.java`
4. `ComicAPI/src/main/java/org/stapledon/infrastructure/config/ApplicationConfigurationFacade.java`

### Files to Move
- `Direction.java`, `ImageUtils.java`, `Bootstrap.java` → common/util
- Exception classes → common/model

### Files to Delete (from ComicAPI after Phase 6.6)
- All engine code (downloader, management, batch, storage impl)
- Extracted interfaces (now in common)

---

## Contact Points for Issues

If stuck at any phase:
1. **Phase 6.2-6.4:** Compilation errors → Check package names, imports
2. **Phase 6.5:** Circular dependency → STOP, analyze dependencies, discuss with developer
3. **Phase 6.6:** Missing classes → Check if extraction was complete
4. **Phase 6.7:** Test failures → Rollback and investigate

**Remember:** Better to stop and analyze than to forge ahead with unresolved circular dependencies!
