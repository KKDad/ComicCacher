# Operational State Files

Six JSON files and the daily metrics snapshots track runtime state, job history and metrics. All are located in the cache root directory (`comics.cache.location`).

## File Inventory

| File | Purpose | Module | Responsible Class | Atomic Write |
|:---|:---|:---|:---|:---|
| `batch-executions.json` | Spring Batch job execution history | `comic-engine` | `JsonBatchExecutionTracker` | Yes |
| `retrieval-status.json` | Comic retrieval attempt records | `comic-engine` | `JsonRetrievalStatusRepository` | Yes |
| `scheduler-state.json` | Scheduler pause/resume state | `comic-engine` | `SchedulerStateService` | Yes |
| `backfill-state.json` | What the comic backfill learned: given-up dates, history horizons, daily attempt counts | `comic-engine` | `BackfillStateService` | Yes |
| `source-catalog.json` | Every source's list of comics, for the Sources page | `comic-engine` | `SourceCatalogRepository` | Yes |
| `access-metrics.json` | Per-comic access statistics | `comic-metrics` | `AccessMetricsRepository` | Yes |
| `metrics-history/{yyyy-MM-dd}.json` | Daily snapshot of combined metrics | `comic-metrics` | `MetricsArchiver` | Yes |

---

## 1. batch-executions.json

Tracks Spring Batch job execution history. Written after each job completion via `JobExecutionListener`. Supports migration from a legacy single-entry-per-job format to the current list format.

**Retention:** each job keeps the executions that started in the last `batch.tracking.history-days` days (default: **30**), and always its newest one, so a job that runs rarely still shows its last run. New executions are prepended. History for a job that no longer exists (e.g. the removed `MetricsUpdateJob`) is dropped on the next write, and no job keeps more than 500 executions.

**DTO:** `Map<String, List<BatchExecutionSummary>>` (`comic-engine`)

```json
{
  "dailyDownloadJob": [
    {
      "executionId": 147,
      "jobName": "dailyDownloadJob",
      "status": "COMPLETED",
      "exitCode": "COMPLETED",
      "exitMessage": "",
      "startTime": "2025-03-18T06:30:00-04:00",
      "endTime": "2025-03-18T06:30:45-04:00",
      "errorMessage": null,
      "parameters": {
        "date": "2025-03-18"
      },
      "steps": [
        {
          "stepName": "downloadStep",
          "status": "COMPLETED",
          "readCount": 120,
          "writeCount": 118,
          "filterCount": 2,
          "skipCount": 0,
          "commitCount": 12,
          "rollbackCount": 0,
          "startTime": "2025-03-18T06:30:01-04:00",
          "endTime": "2025-03-18T06:30:44-04:00"
        }
      ]
    }
  ]
}
```

### Field Reference (BatchExecutionSummary)

| Field | Type | Description |
|:---|:---|:---|
| `executionId` | `Long` | Spring Batch execution ID |
| `jobName` | `String` | Job name (also the map key) |
| `status` | `String` | `COMPLETED`, `FAILED`, `STARTED`, etc. |
| `exitCode` | `String` | Exit status code |
| `exitMessage` | `String` | Exit status description |
| `startTime` | `OffsetDateTime` | Job start, in `batch.timezone` |
| `endTime` | `OffsetDateTime` | Job end, in `batch.timezone` |
| `errorMessage` | `String` (nullable) | First failure exception message |
| `parameters` | `Map<String, Object>` | Job parameters |
| `steps` | `List<BatchStepSummary>` | Per-step execution details |

### Field Reference (BatchStepSummary)

| Field | Type | Description |
|:---|:---|:---|
| `stepName` | `String` | Step name |
| `status` | `String` | Step status |
| `readCount` | `int` | Items read |
| `writeCount` | `int` | Items written |
| `filterCount` | `int` | Items filtered |
| `skipCount` | `int` | Items skipped |
| `commitCount` | `int` | Chunk commits |
| `rollbackCount` | `int` | Chunk rollbacks |
| `startTime` | `OffsetDateTime` | Step start, in `batch.timezone` |
| `endTime` | `OffsetDateTime` | Step end, in `batch.timezone` |

---

## 2. retrieval-status.json

Records individual comic retrieval attempts with outcomes. Used for troubleshooting and monitoring download success rates. In-memory cached after first load.

**Purging:** `RetrievalRecordPurgeJob` at 6:45 AM with configurable retention (`batch.record-purge.days-to-keep`, default 30), by `attemptedAt`, so a backfill of an old strip keeps its record for the full window. Records written before `attemptedAt` was kept are purged by `comicDate`.

**Keys:** one record per comic and date; a new attempt replaces the last. Saving a record also replaces an older one keyed by the comic's name. See [Batch Jobs Design](../design/batch-jobs.md#retrievalrecordpurgejob).

**DTO:** `ComicRetrievalRecordStorage` wrapping `List<ComicRetrievalRecord>` (`comic-common`)

```json
{
  "lastUpdated": "2025-03-18T10:30:00-04:00",
  "records": [
    {
      "id": "1234_2025-03-18",
      "comicId": 1234,
      "comicName": "Calvin and Hobbes",
      "comicDate": "2025-03-18",
      "source": "gocomics",
      "status": "SUCCESS",
      "errorMessage": null,
      "retrievalDurationMs": 1250,
      "imageSize": 145230,
      "httpStatusCode": null,
      "attemptedAt": "2025-03-18T10:30:00Z"
    },
    {
      "id": "5678_2025-03-18",
      "comicId": 5678,
      "comicName": "Garfield",
      "comicDate": "2025-03-18",
      "source": "gocomics",
      "status": "NETWORK_ERROR",
      "errorMessage": "Connection timed out",
      "retrievalDurationMs": 30000,
      "imageSize": null,
      "httpStatusCode": null,
      "attemptedAt": "2025-03-18T10:31:12Z"
    }
  ]
}
```

### Field Reference (ComicRetrievalRecord)

| Field | Type | Description |
|:---|:---|:---|
| `id` | `String` | `{comicId}_{yyyy-MM-dd}`; `{ComicName}_{yyyy-MM-dd}` on older records. Treat as opaque |
| `comicId` | `Integer` (nullable) | Comic id; null on older records |
| `comicName` | `String` | Comic name |
| `comicDate` | `LocalDate` | Target retrieval date |
| `source` | `String` | Source provider (e.g., `gocomics`, `comicskingdom`) |
| `status` | `ComicRetrievalStatus` | `SUCCESS`, `NETWORK_ERROR`, `RATE_LIMITED`, `PARSING_ERROR`, `COMIC_UNAVAILABLE`, `AUTHENTICATION_ERROR`, `STORAGE_ERROR`, `UNKNOWN_ERROR` |
| `errorMessage` | `String` (nullable) | Error details if failed |
| `retrievalDurationMs` | `long` | Operation duration in milliseconds |
| `imageSize` | `Long` (nullable) | Downloaded image size in bytes |
| `httpStatusCode` | `Integer` (nullable) | HTTP status from source |
| `attemptedAt` | `OffsetDateTime` (nullable) | When the attempt was made (UTC), stamped on save; null on older records |

---

## 3. scheduler-state.json

Persists pause/resume state for batch job schedulers so it survives application restarts. Loaded on startup, written on every state change.

Written with `NfsFileOperations.atomicWrite()`, like the other state files.

**DTO:** `Map<String, SchedulerState>` (`comic-engine`)

```json
{
  "dailyDownloadJob": {
    "paused": true,
    "lastToggled": "2025-03-18T14:30:00Z",
    "toggledBy": "admin"
  },
  "metricsCollectionJob": {
    "paused": false,
    "lastToggled": "2025-03-17T09:00:00Z",
    "toggledBy": "admin"
  }
}
```

### Field Reference (SchedulerState record)

| Field | Type | Description |
|:---|:---|:---|
| `paused` | `boolean` | Whether the job scheduler is paused |
| `lastToggled` | `OffsetDateTime` | When the state was last changed (UTC) |
| `toggledBy` | `String` | Username who made the change |

---

## 4. last_errors.json (obsolete)

Removed: it repeated the failures in `retrieval-status.json`, and nothing read it. Find a comic's recent failures with the `retrievalRecords` query (filter by `comicName` and `status`), which carry `attemptedAt`. An old copy left in the cache root is safe to delete; `utils/verify-json-files.sh` flags it.

---

## 5. access-metrics.json

Tracks per-comic access statistics (hit counts, cache performance). Loaded on startup via `@PostConstruct`. Thread-safe with `ReentrantReadWriteLock`.

**DTO:** `AccessMetricsData` (`comic-metrics`)

```json
{
  "lastUpdated": "2025-03-18T10:30:00-04:00",
  "comicMetrics": {
    "Calvin and Hobbes": {
      "comicName": "Calvin and Hobbes",
      "accessCount": 342,
      "lastAccess": "2025-03-18T10:28:00",
      "totalAccessTimeMs": 15400,
      "cacheHits": 310,
      "cacheMisses": 32
    }
  }
}
```

### Field Reference (ComicAccessMetrics)

| Field | Type | Default | Description |
|:---|:---|:---|:---|
| `comicName` | `String` | -- | Comic name (map key) |
| `accessCount` | `int` | `0` | Total access count |
| `lastAccess` | `String` | `""` | Last access timestamp |
| `totalAccessTimeMs` | `long` | `0` | Cumulative access time |
| `cacheHits` | `int` | `0` | Cache hit count |
| `cacheMisses` | `int` | `0` | Cache miss count |

Derived fields (computed, not persisted): `averageAccessTime` = `totalAccessTimeMs / accessCount`, `hitRatio` = `cacheHits / (cacheHits + cacheMisses)`.

---

## 6. metrics-history/{yyyy-MM-dd}.json

Daily snapshots of combined metrics. `MetricsArchiveJob` builds combined metrics on demand (`MetricsUpdateService.buildCombinedMetrics()`, from a storage scan plus `access-metrics.json`) and `MetricsArchiver` writes them under the previous day's date. Archives older than `comics.metrics.history-retention-days` (default 90) are deleted after each successful run. Combined metrics are never persisted anywhere else; a leftover `combined-metrics.json` in the cache root is obsolete and unused.

**DTO:** `CombinedMetricsData` (`comic-metrics`)

```json
{
  "lastUpdated": "2025-03-18T10:30:00-04:00",
  "globalMetrics": {
    "oldestImage": "/cache/CalvinandHobbes/1985/1985-11-18.png",
    "newestImage": "/cache/Garfield/2025/2025-03-18.png",
    "years": ["1985", "1986", "2024", "2025"],
    "totalStorageBytes": 52428800000,
    "totalImageCount": 145000,
    "storageByYear": {
      "2025": 1073741824,
      "2024": 2147483648
    },
    "imageCountByYear": {
      "2025": 3500,
      "2024": 14000
    }
  },
  "perComicMetrics": {
    "Calvin and Hobbes": {
      "comicName": "Calvin and Hobbes",
      "storageBytes": 524288000,
      "imageCount": 3160,
      "averageImageSize": 165975.0,
      "yearlyStorage": {
        "2025": {
          "storageBytes": 10485760,
          "imageCount": 75
        }
      },
      "accessCount": 342,
      "lastAccess": "2025-03-18T10:28:00",
      "averageAccessTime": 45.0,
      "hitRatio": 0.906,
      "cacheHits": 310,
      "cacheMisses": 32
    }
  }
}
```

### Field Reference (GlobalMetrics)

| Field | Type | Default | Description |
|:---|:---|:---|:---|
| `oldestImage` | `String` | `null` | Absolute path to oldest cached image |
| `newestImage` | `String` | `null` | Absolute path to newest cached image |
| `years` | `List<String>` | `null` | All years with cached content |
| `totalStorageBytes` | `long` | `0` | Total storage across all comics |
| `totalImageCount` | `int` | `0` | Total image count across all comics |
| `storageByYear` | `Map<String, Long>` | `null` | Storage bytes per year |
| `imageCountByYear` | `Map<String, Integer>` | `null` | Image count per year |

### Field Reference (ComicCombinedMetrics)

| Field | Type | Default | Description |
|:---|:---|:---|:---|
| `comicName` | `String` | -- | Comic name (map key) |
| `storageBytes` | `long` | `0` | Total storage for this comic |
| `imageCount` | `int` | `0` | Total images for this comic |
| `averageImageSize` | `double` | `0.0` | Average image size in bytes |
| `yearlyStorage` | `Map<String, YearlyStorageMetrics>` | `{}` | Per-year breakdown |
| `accessCount` | `int` | `0` | Total access count |
| `lastAccess` | `String` | `""` | Last access timestamp |
| `averageAccessTime` | `double` | `0.0` | Average access time in ms |
| `hitRatio` | `double` | `0.0` | Cache hit ratio (0.0-1.0) |
| `cacheHits` | `int` | `0` | Cache hit count |
| `cacheMisses` | `int` | `0` | Cache miss count |

### Field Reference (YearlyStorageMetrics)

| Field | Type | Default | Description |
|:---|:---|:---|:---|
| `storageBytes` | `long` | `0` | Storage bytes for this year |
| `imageCount` | `int` | `0` | Image count for this year |

---

## 7. source-catalog.json

Every source's catalog as `SourceCatalogJob` last read it. Created by the first catalog refresh. See [Comic Sources and Their Catalogs](../design/source-catalog.md).

```json
{
  "sources": {
    "gocomics": {
      "lastRefreshed": "2026-09-28T11:00:00Z",
      "lastAttempt": "2026-09-28T11:00:00Z",
      "lastError": null,
      "entries": {
        "calvinandhobbes": {
          "name": "Calvin and Hobbes",
          "author": "Bill Watterson",
          "thumbnailUrl": "https://gocomicscmsassets.gocomics.com/.../Badge.png",
          "thumbnailSavedAt": "2026-09-28T11:20:00Z",
          "description": "Follow the adventures of Calvin and his stuffed tiger.",
          "tags": ["Newspaper Comic Strips"],
          "detailsCheckedAt": "2026-09-28T11:10:00Z",
          "detailsExpireAt": "2026-11-29T11:10:00Z",
          "startDate": "1985-11-18",
          "startCheckedAt": "2026-09-28T11:05:00Z",
          "firstSeen": "2026-09-28T11:00:00Z",
          "lastSeen": "2026-09-28T11:00:00Z",
          "removedAt": null
        }
      }
    }
  }
}
```

| Field | Type | Description |
|:---|:---|:---|
| `lastRefreshed` | `OffsetDateTime` | When the catalog was last read successfully |
| `lastAttempt` / `lastError` | `OffsetDateTime` / `String` | The last refresh attempt, and why it failed (null when it worked) |
| `entries` | `Map<String, Entry>` | By the comic's identifier at the source |
| `thumbnailUrl` | `String` | The source's image, downloaded into `tmp/catalog-thumbnails/` |
| `thumbnailSavedAt` / `thumbnailFailedAt` | `OffsetDateTime` | When the thumbnail was saved (null when none is on disk), and when its last download failed (retried a week later) |
| `description` / `tags` | `String` / `List<String>` | The source's short description and genres or categories |
| `detailsCheckedAt` / `detailsExpireAt` | `OffsetDateTime` | When they were last read from the comic's page, and when to read them again (30–90 days, random). Null `detailsExpireAt` means due, for a source whose catalog doesn't carry them |
| `startDate` / `startStripNumber` | `LocalDate` / `Integer` | Where the source says the comic starts |
| `removedAt` | `OffsetDateTime` | When the source stopped listing the comic. Entries are never deleted |

---

## Key Source Files

| File | Module |
|:---|:---|
| `JsonBatchExecutionTracker.java` | `comic-engine` |
| `BatchExecutionSummary.java` / `BatchStepSummary.java` | `comic-engine` |
| `JsonRetrievalStatusRepository.java` | `comic-engine` |
| `ComicRetrievalRecord.java` / `ComicRetrievalRecordStorage.java` | `comic-common` |
| `SchedulerStateService.java` | `comic-engine` |
| `BackfillStateService.java` | `comic-engine` |
| `SourceCatalogRepository.java` / `SourceCatalogState.java` | `comic-engine` |
| `AccessMetricsRepository.java` | `comic-metrics` |
| `AccessMetricsData.java` | `comic-metrics` |
| `MetricsArchiver.java` / `MetricsArchiveService.java` | `comic-metrics` |
| `CombinedMetricsData.java` / `GlobalMetrics.java` / `YearlyStorageMetrics.java` | `comic-metrics` |
