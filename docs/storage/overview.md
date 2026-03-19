# Storage Overview

ComicCacher uses a flat-file JSON storage model on an NFS-mounted filesystem. There is no relational database. The cache root directory is configured via the `comics.cache.location` property in `CacheProperties`.

## Directory Tree Layout

```
{comics.cache.location}/                    # Cache root
  comics.json                               # Comic registry (configurable: comics.cache.config)
  users.json                                # User accounts (configurable: comics.cache.usersConfig)
  preferences.json                          # User preferences (configurable: comics.cache.preferencesConfig)
  batch-executions.json                     # Spring Batch job history
  retrieval-status.json                     # Comic retrieval attempt records
  scheduler-state.json                      # Scheduler pause/resume state
  last_errors.json                          # Recent errors per comic
  access-metrics.json                       # Per-comic access counts
  combined-metrics.json                     # Global + per-comic storage/access metrics
  {ComicDirName}/                           # One directory per comic
    avatar.png                              # Comic avatar image
    available-dates.json                    # Date index for fast navigation
    {Year}/                                 # One directory per year (e.g., 2025)
      {yyyy-MM-dd}.png                      # Strip image
      {yyyy-MM-dd}.json                     # Metadata sidecar
      image-hashes.json                     # Perceptual hashes for duplicate detection
```

## Comic Directory Naming

Directory names are derived from the comic name with spaces stripped. The logic lives in two places:

- **`ComicIdentifier.getDirectoryName()`** (`comic-common`) -- used by `FileSystemComicStorageFacade` for all read/write operations. Strips spaces from `name`. Falls back to `comic_{id}` if the name is null or empty.
- **`ComicIndexService.sanitizeComicName()`** (`comic-engine`) -- used for index file paths. Validates against the pattern `^[a-zA-Z0-9 _-]+$`. Falls back to `comic_{id}` if the name contains invalid characters.

Examples:

| Comic Name | Directory |
|:---|:---|
| `Adam At Home` | `AdamAtHome` |
| `Calvin and Hobbes` | `CalvinandHobbes` |
| `null` or `""` | `comic_42` (where 42 is the comic ID) |
| `../../etc/passwd` | `comic_42` (fails pattern validation) |

## Avatar Path

Each comic stores its avatar at `{CacheRoot}/{ComicDirName}/avatar.png`. The constant is defined as `AVATAR_FILE = "avatar.png"` in `FileSystemComicStorageFacade`. Avatar images are validated before saving (format and dimensions) but have no minimum dimension requirement unlike strip images (which require at least 100x50 pixels).

## NFS Atomic Write Pattern

All JSON persistence uses `NfsFileOperations.atomicWrite()` from `comic-common` to prevent corruption during NFS network hiccups. The pattern:

1. Write content to a temporary file in the same directory: `{target}.tmp.{System.nanoTime()}`
2. Atomic move to the target path using `Files.move()` with `ATOMIC_MOVE` and `REPLACE_EXISTING` flags
3. If the move fails, delete the temp file (best-effort cleanup)

```
NfsFileOperations.atomicWrite(target, content)
  -> Files.writeString(target.tmp.{nanos}, content, UTF_8)
  -> Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)
```

This pattern is used by: `ComicIndexService`, `JsonBatchExecutionTracker`, `JsonRetrievalStatusRepository`, `JsonErrorTrackingRepository`, `DuplicateImageHashRepository`, `ImageMetadataRepository`, `AccessMetricsRepository`, and `JsonMetricsRepository`.

**Exception:** `SchedulerStateService` uses plain `Files.writeString()` without the atomic pattern. `ApplicationConfigurationFacade` (for `comics.json`, `users.json`, `preferences.json`) uses `FileWriter` directly.

## `@eaDir` Exclusion

The cache is typically hosted on a Synology NAS. Synology creates `@eaDir` metadata directories inside every directory for thumbnail indexing. These are excluded from all filesystem scans:

- `FileSystemComicStorageFacade` skips directories named exactly `@eaDir` (constant `EXCLUDED_SYNOLOGY_DIR`)
- `ComicIndexService` skips any directory whose name starts with `@` (constant `SYNOLOGY_METADATA_PREFIX`)

## Key Source Files

| File | Module |
|:---|:---|
| `FileSystemComicStorageFacade.java` | `comic-engine` |
| `NfsFileOperations.java` | `comic-common` |
| `CacheProperties.java` | `comic-common` |
| `ComicIdentifier.java` | `comic-common` |
| `ComicIndexService.java` | `comic-engine` |
| `ApplicationConfigurationFacade.java` | `comic-api` |
