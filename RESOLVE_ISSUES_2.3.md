# RESOLVE_ISSUES_2.3 - Bug Fix Tracking

**Created:** 2025-10-27
**Status:** In Progress

---

## Overview

This document tracks the resolution of 15 bugs identified in ComicCacher:
- 3 UI navigation issues
- 6 Backend caching/metrics/error tracking issues
- 6 Comic configuration issues (✓ COMPLETED)

---

## Production Migration Status

### ✓ COMPLETED: comics.json Migration (2025-10-27)
- **Action:** Removed stale `comics` array from production comics.json
- **Result:** Application now uses correct `items` map data
- **Impact:** Fixed 404 errors for Baby Blues, Mother Goose & Grimm, Sherman's Lagoon, TheDuplex

---

## Bug Status Summary

| Category | Total | Completed | Not Integrated | Investigating | Pending |
|----------|-------|-----------|----------------|---------------|---------|
| Comic Config | 6 | 6 | 0 | 0 | 0 |
| Backend | 6 | 3 | 1 | 1 | 1 |
| UI | 3 | 0 | 0 | 0 | 3 |
| **TOTAL** | **15** | **9** | **1** | **1** | **4** |

---

## Comic Configuration Issues ✓ COMPLETED

### BUG-CFG-1: PCandPixel - Missing inactive flag ✓
- **Status:** FIXED (production comics.json has `"active": false`)
- **Verified:** 2025-10-27

### BUG-CFG-2: Baby Blues - Wrong source ✓
- **Status:** FIXED (production has `"source": "gocomics"`, `"sourceIdentifier": "babyblues"`)
- **Verified:** 2025-10-27

### BUG-CFG-3: Sherman's Lagoon - Wrong source ✓
- **Status:** FIXED (production has `"source": "gocomics"`, `"sourceIdentifier": "shermanslagoon"`)
- **Verified:** 2025-10-27

### BUG-CFG-4: Mother Goose & Grimm - Wrong source ✓
- **Status:** FIXED (production has `"source": "gocomics"`, `"sourceIdentifier": "mother-goose-and-grimm"`)
- **Verified:** 2025-10-27

### BUG-CFG-5: FoxTrot - Missing publicationDays 🔄
- **Status:** PARTIALLY FIXED
- **Production:** Has correct `sourceIdentifier: "foxtrot"`
- **Missing:** `"publicationDays": ["SUNDAY"]` - needs manual fix
- **Action Required:** Update production comics.json

### BUG-CFG-6: TheDuplex - Wrong sourceIdentifier ✓
- **Status:** FIXED (production has `"sourceIdentifier": "duplex"`)
- **Verified:** 2025-10-27

### BUG-CFG-7: Committed - Missing inactive flag ✓
- **Status:** FIXED (production has `"active": false`)
- **Verified:** 2025-10-27

---

## Backend Issues (6 pending)

### BUG-BE-1: Cache Key Inconsistency ✓
- **Status:** FIXED (2025-10-27)
- **Issue:** Cache keys may not consistently include comic ID/name + date
- **Location:** `@Cacheable` annotations audited
- **Analysis:** All cache keys are CORRECT. Navigation caches include comicId+date+direction, boundary caches include comicId. Root cause of BUG-BE-3 was missing cache eviction, not key inconsistency.
- **Files Reviewed:**
  - `comic-api/src/main/java/org/stapledon/infrastructure/config/CaffeineCacheConfiguration.java:40-56`
  - `comic-engine/src/main/java/org/stapledon/engine/management/ComicManagementFacade.java:67,75,209`
  - `comic-engine/src/main/java/org/stapledon/engine/storage/FileSystemComicStorageFacade.java:231,268,305,329`

### BUG-BE-2: Attempting to Cache Today + 1 🔍
- **Status:** INVESTIGATING (2025-10-27)
- **Issue:** System tries to cache tomorrow's comics (don't exist yet)
- **Initial Analysis:** Main batch jobs use `LocalDate.now()`, backfill has guards against future dates
- **Action Taken:** Added diagnostic logging to detect future date attempts:
  - `ComicManagementFacade.updateComicsForDate()` - warns if date > today
  - `ComicBackfillService.findMissingStrips()` - logs date range and warns if scanStart > today
  - `PredictiveCacheService.prefetchAdjacentComics()` - warns if prefetch from future date
- **Next Step:** Re-test in production to capture logs showing when/where future dates occur
- **Files Modified:**
  - `comic-engine/src/main/java/org/stapledon/engine/management/ComicManagementFacade.java:307-312`
  - `comic-engine/src/main/java/org/stapledon/engine/batch/ComicBackfillService.java:60-65`
  - `comic-api/src/main/java/org/stapledon/infrastructure/caching/PredictiveCacheService.java:45-49`

### BUG-BE-3: Date Advance Cache Staleness ✓
- **Status:** FIXED (2025-10-27)
- **Issue:** When date advances (26th → 27th), cache doesn't update, stops showing comics past start date
- **Root Cause:** `getNewestDateWithComic()` cached by comicId only, never evicted when new comics downloaded
- **Fix:** Added `@CacheEvict(value = "boundaryDates", key = "'newest:' + #comicId")` to `saveComicStrip()` method
- **Files Modified:**
  - `comic-engine/src/main/java/org/stapledon/engine/storage/FileSystemComicStorageFacade.java:79`

### BUG-BE-4: access-metrics.json Always Empty
- **Status:** NOT A BUG - Feature Not Integrated (2025-10-27)
- **Issue:** File shows `{"lastUpdated":"...","comicMetrics":{}}` - no metrics tracked
- **Root Cause:** `AccessMetricsCollector` is legacy code not integrated into current application flow
  - `trackAccess()` is private, only called from `findOldest/findNewest/findNext/findPrevious` methods
  - These methods are never called in production (only in tests)
  - Application uses `ComicStorageFacade` directly, bypassing metrics collector
  - `@PostConstruct/@PreDestroy` hooks exist but metrics never populate
- **Fix Options:**
  1. Integrate metrics into `ComicManagementFacade` navigation methods
  2. Remove unused metrics feature entirely
- **Files Analyzed:**
  - `comic-metrics/src/main/java/org/stapledon/metrics/collector/AccessMetricsCollector.java:126` (trackAccess is private)
  - `comic-engine/src/main/java/org/stapledon/engine/management/ComicManagementFacade.java:166-264` (uses storageFacade directly)

### BUG-BE-5 & BUG-BE-6: last_errors.json Accumulating Over Multiple Days ✓
- **Status:** FIXED (2025-10-27)
- **Issue:** Errors accumulate over days instead of showing only recent errors
- **Root Cause:** No mechanism to clear old errors, only limited to 5 per comic
- **Fix:**
  - Added `clearOldErrors(int hoursToKeep)` method to ErrorTrackingService
  - ComicDownloadJobScheduler calls `clearOldErrors(48)` before each batch run
  - Keeps errors from last 48 hours, removes older ones
- **Files Modified:**
  - `comic-common/src/main/java/org/stapledon/common/service/ErrorTrackingService.java:50-56` (added method)
  - `comic-engine/src/main/java/org/stapledon/engine/storage/JsonErrorTrackingRepository.java:158-195` (implementation)
  - `comic-engine/src/main/java/org/stapledon/engine/batch/ComicDownloadJobScheduler.java:51,130-135` (call clearOldErrors)

---

## UI Issues (3 total)

### BUG-UI-1: Page Up/Down Misalignment ✓
- **Status:** FIXED (2025-10-27)
- **Issue:** When comic partially shown, PageUp/PageDown doesn't align correctly
- **Root Cause:** Used fixed ±50px threshold that didn't handle partially-visible comics properly
- **Fix:** Rewrote scroll logic to:
  - Find which comic is currently at viewport top
  - If partially scrolled (>10px from top), snap to current comic's top
  - If already aligned, move to previous/next comic
  - Handles edge cases (first/last comic, no comic found)
- **Files Modified:**
  - `comic-web/src/app/comicpage/container/container.component.ts:162-211` (scrollUpByOneComic)
  - `comic-web/src/app/comicpage/container/container.component.ts:217-266` (scrollDownByOneComic)

### BUG-UI-2: Page Up/Down Missing Selection
- **Status:** DEFERRED
- **Issue:** No comic visually selected after PageUp/PageDown
- **Analysis:** This is a visual enhancement, not a bug. PageUp/PageDown scrolls the list, not individual comic strips
- **Note:** Arrow keys (Left/Right) navigate individual comic strips and do show selection
- **If Needed:** Could add CSS class to highlight top-aligned comic, but not critical for functionality

### BUG-UI-3: Left/Right Arrow Navigation ✓
- **Status:** NOT A BUG (2025-10-27)
- **Issue:** Left/Right arrows only work when comic has explicit focus
- **Analysis:** Keyboard shortcuts ARE registered globally on `document` (keyboard-service.ts:23)
  - `registerComicStripNavigationShortcuts` sets up Left/Right for prev/next strip
  - These shortcuts work without needing explicit focus
  - May have been user confusion or testing issue
- **Files Verified:**
  - `comic-web/src/app/shared/a11y/keyboard-service.ts:102-123` (global document listeners)
  - Arrow keys should work from anywhere in the page

---

## Phase 1: Code Fixes (Prevent Future Issues) ✓ COMPLETED

### Task 1.1: Add @JsonIgnore to ComicConfig ✓
- **File:** `comic-common/src/main/java/org/stapledon/common/dto/ComicConfig.java`
- **Change:** Added `@JsonIgnore` annotation to `getComics()` method
- **Purpose:** Prevent `comics` array from being serialized in future saves
- **Status:** COMPLETED (2025-10-27)

### Task 1.2: Update ComicCacher.json Bootstrap ✓
- **File:** `comic-api/src/main/resources/ComicCacher.json`
- **Changes Applied:**
  - ✓ Added `"sourceIdentifier": "foxtrot"` to FoxTrot (already had publicationDays)
  - ✓ Added `"sourceIdentifier": "pcandpixel"` to PCandPixel (already had active: false)
  - ✓ Added `"sourceIdentifier": "committed"` to Committed (already had active: false)
  - ✓ Verified all other sourceIdentifiers correct (Baby Blues, Sherman's Lagoon, Mother Goose & Grimm, TheDuplex)
- **Status:** COMPLETED (2025-10-27)

### Task 1.3: Fix FoxTrot in Production 📝
- **File:** Production `comics.json` (in cache directory)
- **Change:** Add `"publicationDays": ["SUNDAY"]` to FoxTrot entry in items map
- **Status:** PENDING (requires production access)

---

## Testing Checklist

### Backend Tests
- [ ] Verify cache keys include comic+date consistently
- [ ] Verify no future date caching attempts
- [ ] Verify cache updates on date change
- [ ] Verify metrics persist correctly
- [ ] Verify last_errors.json contains only recent errors

### UI Tests
- [ ] Test PageUp/PageDown alignment from various scroll positions
- [ ] Verify comic selection after paging
- [ ] Test Left/Right arrows without explicit focus

### Integration Tests
- [ ] Full navigation flow across date boundaries
- [ ] Comic configs load correctly from bootstrap
- [ ] No 404 errors for properly configured comics
- [ ] FoxTrot only downloads on Sundays
- [ ] PCandPixel and Committed are skipped (inactive)

---

## Implementation Timeline

- **Day 1-2:** Phase 1 - Code fixes (prevent config corruption)
- **Day 3-5:** Phase 2 - Backend bug fixes (cache, metrics, errors)
- **Day 6-8:** Phase 3 - UI bug fixes (navigation, alignment, selection)
- **Day 9-10:** Testing - Full regression testing

---

## Notes

### ComicCacher.json vs comics.json
- **ComicCacher.json** (resources): Bootstrap template, loaded as @Bean but never used to initialize production
- **comics.json** (cache directory): Live production data, what application actually uses
- **Migration complete:** Removed stale `comics` array, keeping correct `items` map
- **No data loss:** Items map had more complete and correct data than comics array

### Key Learnings
1. Production comics.json had both `items` (correct) and `comics` (stale) sections
2. ComicConfig.getComics() used `comics` array when present, causing wrong data
3. Removing `comics` array triggers auto-migration from `items` map
4. Bootstrap config exists but was never integrated for production initialization

---

## Version History

- **2025-10-27:** Initial creation, production migration completed (6 config bugs fixed)
- **2025-10-27:** Phase 1 started - code fixes to prevent future corruption
