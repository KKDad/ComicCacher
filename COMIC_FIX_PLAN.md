# Comic Configuration Fix Plan

**Status:** Planning
**Created:** 2025-10-24
**Goal:** Fix 7 problematic comics (wrong slugs, wrong sources, discontinued)

---

## Progress Tracker

- [ ] Phase 1: Add Slug Override Support
- [ ] Phase 2: Add Publication Schedule Support
- [ ] Phase 3: Add Comic Status Flag
- [ ] Phase 4: Migrate Comics Configuration
- [ ] Phase 5: Create Python Migration Script
- [ ] Phase 6: Testing

---

## Comics to Fix

| Comic | Issue | Fix | Status |
|-------|-------|-----|--------|
| TheDuplex | Wrong slug (`theduplex` vs `duplex`) | Override: `sourceIdentifier: "duplex"` | ⏳ Pending |
| FoxTrot | Tries daily, publishes Sunday only | Add: `publicationDays: ["SUNDAY"]` | ⏳ Pending |
| Mother Goose & Grimm | Wrong source (Kingdom → GoComics) | Move to `dailyComics`, slug: `mother-goose-and-grimm` | ⏳ Pending |
| Sherman's Lagoon | Wrong source (Kingdom → GoComics) | Move to `dailyComics`, slug: `shermanslagoon` | ⏳ Pending |
| Baby Blues | Wrong source (Kingdom → GoComics) | Move to `dailyComics`, slug: `babyblues` | ⏳ Pending |
| Committed | Ended 2006, still downloading | Mark: `active: false` | ⏳ Pending |
| PC and Pixel | Delisted from GoComics | Mark: `active: false` | ⏳ Pending |

---

## Phase 1: Add Slug Override Support

**Goal:** Allow explicit `sourceIdentifier` in `ComicCacher.json` to override auto-generated slugs

### Tasks
- [ ] Update `GoComicsBootstrap.java`
  - [ ] Add optional `sourceIdentifier` field
  - [ ] Modify `getSourceIdentifier()` to use explicit value if provided
  - [ ] Fall back to auto-generation if not specified

- [ ] Update `KingComicsBootStrap.java`
  - [ ] Add optional `sourceIdentifier` field (for consistency)
  - [ ] Modify `getSourceIdentifier()` to use explicit value if provided

### Files to Modify
- `comic-api/src/main/java/org/stapledon/infrastructure/config/GoComicsBootstrap.java`
- `comic-api/src/main/java/org/stapledon/infrastructure/config/KingComicsBootStrap.java`

### Expected Behavior
```json
// In ComicCacher.json:
{"name": "TheDuplex", "sourceIdentifier": "duplex", "startDate": {...}}

// Generated URL:
https://www.gocomics.com/duplex/2024/10/24/  (not theduplex)
```

---

## Phase 2: Add Publication Schedule Support

**Goal:** Support Sunday-only comics like FoxTrot (skip Mon-Sat downloads)

### Tasks
- [ ] Add `publicationDays` field to bootstrap classes
  - [ ] `GoComicsBootstrap.java`
  - [ ] `KingComicsBootStrap.java`
  - [ ] Define as `List<DayOfWeek>` (can be empty/null for daily)

- [ ] Add `publicationDays` to `ComicItem` DTO
  - [ ] Update `comic-common/src/main/java/org/stapledon/common/dto/ComicItem.java`
  - [ ] Add `List<DayOfWeek> publicationDays` field
  - [ ] Pass schedule info to frontend

- [ ] Update `Bootstrap.convertBootstrapToComicItem()`
  - [ ] Copy `publicationDays` from bootstrap to `ComicItem`

- [ ] Update download logic
  - [ ] Find where downloads are scheduled (batch jobs)
  - [ ] Check `publicationDays` before attempting download
  - [ ] Skip gracefully if comic doesn't publish on requested date
  - [ ] Log as "Skipped - not published today" instead of error

### Files to Modify
- `comic-api/src/main/java/org/stapledon/infrastructure/config/GoComicsBootstrap.java`
- `comic-api/src/main/java/org/stapledon/infrastructure/config/KingComicsBootStrap.java`
- `comic-common/src/main/java/org/stapledon/common/dto/ComicItem.java`
- `comic-common/src/main/java/org/stapledon/common/util/Bootstrap.java`
- Download scheduler (find file location)

### Expected Behavior
```json
// In ComicCacher.json:
{"name": "FoxTrot", "publicationDays": ["SUNDAY"], "startDate": {...}}

// Result:
// Monday-Saturday: Skip download (no error)
// Sunday: Download as normal
```

---

## Phase 3: Add Comic Status Flag

**Goal:** Mark discontinued comics but keep them visible in UI with "(Inactive)" label

### Backend Tasks
- [ ] Add `active` field to bootstrap classes
  - [ ] `GoComicsBootstrap.java` - add `Boolean active` (default `true`)
  - [ ] `KingComicsBootStrap.java` - add `Boolean active` (default `true`)
  - [ ] Implement `getActive()` method in `IComicsBootstrap` interface

- [ ] Add `active` field to `ComicItem` DTO
  - [ ] Update `ComicItem.java` with `Boolean active` field
  - [ ] Pass status to frontend

- [ ] Update `Bootstrap.convertBootstrapToComicItem()`
  - [ ] Set `ComicItem.active = bootstrap.getActive()` (default `true`)
  - [ ] Keep `ComicItem.enabled = true` (always show in UI)

- [ ] Update download scheduler
  - [ ] Skip comics where `active = false`
  - [ ] Log as "Skipped - comic inactive" instead of error
  - [ ] Don't attempt new downloads for inactive comics

### Frontend Tasks (comic-web)
- [ ] Update comic display components
  - [ ] Add logic to show "(Inactive)" or "(Finished)" badge when `active = false`
  - [ ] Use muted/gray styling for inactive comics
  - [ ] Find components that display comic names

- [ ] Keep inactive comics visible
  - [ ] Verify display in comic list
  - [ ] Verify viewing existing cached strips works
  - [ ] Just don't fetch new ones

### Files to Modify
**Backend:**
- `comic-api/src/main/java/org/stapledon/infrastructure/config/GoComicsBootstrap.java`
- `comic-api/src/main/java/org/stapledon/infrastructure/config/KingComicsBootStrap.java`
- `comic-common/src/main/java/org/stapledon/common/config/IComicsBootstrap.java`
- `comic-common/src/main/java/org/stapledon/common/dto/ComicItem.java`
- `comic-common/src/main/java/org/stapledon/common/util/Bootstrap.java`
- Download scheduler (find file location)

**Frontend:**
- `comic-web/src/app/` (find comic display components)

### Expected Behavior
```json
// In ComicCacher.json:
{"name": "Committed", "active": false, "startDate": {...}}

// Result:
// Backend: Skip all download attempts
// Frontend: Display "Committed (Inactive)" with gray styling
// Frontend: Existing cached strips still viewable
```

---

## Phase 4: Migrate Comics Configuration

**Goal:** Update `ComicCacher.json` with all fixes

### Tasks
- [ ] Update `comic-api/src/main/resources/ComicCacher.json`
  - [ ] Fix TheDuplex: Add `"sourceIdentifier": "duplex"`
  - [ ] Fix FoxTrot: Add `"publicationDays": ["SUNDAY"]`
  - [ ] Mark Committed: Add `"active": false`
  - [ ] Mark PCandPixel: Add `"active": false`
  - [ ] Move Mother Goose & Grimm from `kingComics` to `dailyComics`
  - [ ] Move Sherman's Lagoon from `kingComics` to `dailyComics`
  - [ ] Move Baby Blues from `kingComics` to `dailyComics`

### Expected Config Structure
```json
{
  "dailyComics": [
    {"name": "TheDuplex", "sourceIdentifier": "duplex", "startDate": {...}},
    {"name": "FoxTrot", "publicationDays": ["SUNDAY"], "startDate": {...}},
    {"name": "Committed", "active": false, "startDate": {...}},
    {"name": "PCandPixel", "active": false, "startDate": {...}},
    {"name": "Mother Goose & Grimm", "sourceIdentifier": "mother-goose-and-grimm", "startDate": {...}},
    {"name": "Sherman's Lagoon", "sourceIdentifier": "shermanslagoon", "startDate": {...}},
    {"name": "Baby Blues", "sourceIdentifier": "babyblues", "startDate": {...}},
    // ... other comics
  ],
  "kingComics": [
    // Remove: Mother Goose & Grimm
    // Remove: Sherman's Lagoon
    // Remove: Baby Blues
    {"name": "Beetle Bailey", "website": "...", "startDate": {...}},
    {"name": "Dustin", "website": "...", "startDate": {...}},
    {"name": "Hagar", "website": "...", "startDate": {...}},
    {"name": "Zits", "website": "...", "startDate": {...}}
  ]
}
```

---

## Phase 5: Create Python Migration Script

**Goal:** Automated migration for production `comics.json` files

### Tasks
- [ ] Create `utils/migrate-comics-fix-slugs.py`
  - [ ] Read existing `comics.json` file
  - [ ] Update sourceIdentifier for comics that moved/changed
    - TheDuplex → duplex
    - Mother Goose & Grimm → mother-goose-and-grimm
    - Sherman's Lagoon → shermanslagoon
    - Baby Blues → babyblues
  - [ ] Set `active: false` for discontinued comics
    - Committed
    - PCandPixel
  - [ ] Keep `enabled: true` (don't hide from UI)
  - [ ] Create backup before modifying
  - [ ] Support dry-run mode
  - [ ] Print detailed report of changes

### Script Features
- Backup original file (`.backup` extension)
- Dry-run mode (`--dry-run` flag)
- Detailed change report
- Error handling for missing comics

### Usage
```bash
# Dry run (no changes)
python3 utils/migrate-comics-fix-slugs.py /path/to/comics.json --dry-run

# Apply changes
python3 utils/migrate-comics-fix-slugs.py /path/to/comics.json
```

---

## Phase 6: Testing

### Unit Tests
- [ ] Test `GoComicsBootstrap.getSourceIdentifier()` with explicit override
- [ ] Test `GoComicsBootstrap.getSourceIdentifier()` with auto-generation
- [ ] Test `publicationDays` parsing from JSON
- [ ] Test `active` field parsing from JSON
- [ ] Test `Bootstrap.convertBootstrapToComicItem()` copies new fields

### Integration Tests
- [ ] Test Sunday-only download logic (FoxTrot)
  - [ ] Verify Sunday downloads work
  - [ ] Verify Mon-Sat downloads skip gracefully
- [ ] Test inactive comic skip logic
  - [ ] Verify `active: false` comics skip downloads
  - [ ] Verify still visible in API responses
- [ ] Test corrected GoComics URLs
  - [ ] Verify TheDuplex uses `/duplex/` URL
  - [ ] Verify Mother Goose uses `/mother-goose-and-grimm/` URL
  - [ ] Verify Sherman's uses `/shermanslagoon/` URL
  - [ ] Verify Baby Blues uses `/babyblues/` URL

### Frontend Tests
- [ ] Test inactive comic display
  - [ ] Verify "(Inactive)" label appears
  - [ ] Verify gray/muted styling
  - [ ] Verify existing strips still viewable
- [ ] Test normal comics unchanged
  - [ ] Verify active comics display normally

### Manual Testing
- [ ] Run migration script on test `comics.json` file
- [ ] Verify all 7 comics fixed correctly
- [ ] Start application and verify startup
- [ ] Check logs for Sunday-only skip messages
- [ ] Check logs for inactive skip messages
- [ ] View frontend and verify inactive badges
- [ ] Verify existing Committed/PC and Pixel strips viewable

---

## Implementation Notes

### Key Constraints
- **Inactive comics:** Must remain visible in UI (don't set `enabled: false`)
- **Sunday-only:** Skip gracefully (no error logs on Mon-Sat)
- **Backward compatibility:** All new fields optional (default behavior unchanged)

### Migration Impact
- **Production `comics.json`:** Use Python script to update
- **Bootstrap `ComicCacher.json`:** Manually edit (template for new installs)

---

## Completion Checklist

- [ ] All 7 comics fixed in `ComicCacher.json`
- [ ] Python migration script tested
- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Frontend displays "(Inactive)" correctly
- [ ] Download logs show graceful skips (not errors)
- [ ] Documentation updated (CLAUDE.md, CHANGELOG.md)
- [ ] PR created with changes
