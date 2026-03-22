# Operational State Files

Six JSON files track runtime state, job history, errors, and metrics. All are located in the cache root directory (`comics.cache.location`).

## File Inventory

| File | Purpose | Module | Responsible Class | Atomic Write |
|:---|:---|:---|:---|:---|
| `batch-executions.json` | Spring Batch job execution history | `comic-engine` | `JsonBatchExecutionTracker` | Yes |
| `retrieval-status.json` | Comic retrieval attempt records | `comic-engine` | `JsonRetrievalStatusRepository` | Yes |
| `scheduler-state.json` | Scheduler pause/resume state | `comic-engine` | `SchedulerStateService` | No |
| `last_errors.json` | Recent errors per comic | `comic-engine` | `JsonErrorTrackingRepository` | Yes |
| `access-metrics.json` | Per-comic access statistics | `comic-metrics` | `AccessMetricsRepository` | Yes |
| `combined-metrics.json` | Global + per-comic combined metrics | `comic-metrics` | `JsonMetricsRepository` | Yes |

---

## 1. batch-executions.json

Tracks Spring Batch job execution history. Written after each job completion via `JobExecutionListener`. Supports migration from a legacy single-entry-per-job format to the current list format.

**Capping:** Configurable via `batch.tracking.max-history-per-job` (default: **30**). New executions are prepended; excess entries are trimmed from the tail.

**DTO:** `Map<String, List<BatchExecutionSummary>>` (`comic-engine`)

```json
{
  "dailyDownloadJob": [
    {
      "executionId": 147,
      "jobName": "dailyDownloadJob",
      "executionTime": "2025-03-18T06:30:45",
      "status": "COMPLETED",
      "exitCode": "COMPLETED",
      "exitMessage": "",
      "startTime": "2025-03-18T06:30:00",
      "endTime": "2025-03-18T06:30:45",
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
          "startTime": "2025-03-18T06:30:01",
          "endTime": "2025-03-18T06:30:44"
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
| `executionTime` | `LocalDateTime` | Same as endTime |
| `status` | `String` | `COMPLETED`, `FAILED`, `STARTED`, etc. |
| `exitCode` | `String` | Exit status code |
| `exitMessage` | `String` | Exit status description |
| `startTime` | `LocalDateTime` | Job start timestamp |
| `endTime` | `LocalDateTime` | Job end timestamp |
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
| `startTime` | `LocalDateTime` | Step start |
| `endTime` | `LocalDateTime` | Step end |

---

## 2. retrieval-status.json

Records individual comic retrieval attempts with outcomes. Used for troubleshooting and monitoring download success rates. In-memory cached after first load.

**Purging:** `RetrievalRecordPurgeJob` at 6:45 AM with configurable retention (`batch.record-purge.days-to-keep`, default 30). See [Batch Jobs Design](../design/batch-jobs.md#retrievalrecordpurgejob).

**DTO:** `ComicRetrievalRecordStorage` wrapping `List<ComicRetrievalRecord>` (`comic-common`)

```json
{
  "lastUpdated": "2025-03-18T10:30:00-04:00",
  "records": [
    {
      "id": "CalvinandHobbes_2025-03-18",
      "comicName": "Calvin and Hobbes",
      "comicDate": "2025-03-18",
      "source": "gocomics",
      "status": "SUCCESS",
      "errorMessage": null,
      "retrievalDurationMs": 1250,
      "imageSize": 145230,
      "httpStatusCode": null
    },
    {
      "id": "Garfield_2025-03-18",
      "comicName": "Garfield",
      "comicDate": "2025-03-18",
      "source": "gocomics",
      "status": "NETWORK_ERROR",
      "errorMessage": "Connection timed out",
      "retrievalDurationMs": 30000,
      "imageSize": null,
      "httpStatusCode": null
    }
  ]
}
```

### Field Reference (ComicRetrievalRecord)

| Field | Type | Description |
|:---|:---|:---|
| `id` | `String` | Format: `{ComicName}_{yyyy-MM-dd}` |
| `comicName` | `String` | Comic name |
| `comicDate` | `LocalDate` | Target retrieval date |
| `source` | `String` | Source provider (e.g., `gocomics`, `comicskingdom`) |
| `status` | `ComicRetrievalStatus` | `SUCCESS`, `NETWORK_ERROR`, `PARSING_ERROR`, `COMIC_UNAVAILABLE`, `AUTHENTICATION_ERROR`, `STORAGE_ERROR`, `UNKNOWN_ERROR` |
| `errorMessage` | `String` (nullable) | Error details if failed |
| `retrievalDurationMs` | `long` | Operation duration in milliseconds |
| `imageSize` | `Long` (nullable) | Downloaded image size in bytes |
| `httpStatusCode` | `Integer` (nullable) | HTTP status from source |

---

## 3. scheduler-state.json

Persists pause/resume state for batch job schedulers so it survives application restarts. Loaded on startup, written on every state change.

**Note:** This file does NOT use the `NfsFileOperations.atomicWrite()` pattern. It writes directly via `Files.writeString()`.

**DTO:** `Map<String, SchedulerState>` (`comic-engine`)

```json
{
  "dailyDownloadJob": {
    "paused": true,
    "lastToggled": "2025-03-18T14:30:00",
    "toggledBy": "admin"
  },
  "metricsCollectionJob": {
    "paused": false,
    "lastToggled": "2025-03-17T09:00:00",
    "toggledBy": "admin"
  }
}
```

### Field Reference (SchedulerState record)

| Field | Type | Description |
|:---|:---|:---|
| `paused` | `boolean` | Whether the job scheduler is paused |
| `lastToggled` | `LocalDateTime` | When the state was last changed |
| `toggledBy` | `String` | Username who made the change |

---

## 4. last_errors.json

Tracks the most recent errors per comic for troubleshooting. Keyed by comic name.

**Capping:** Configurable via `comics.metrics.error-tracking.max-errors-per-comic` (default: **5**). New errors are prepended; excess entries are trimmed from the tail.

**DTO:** `Map<String, List<ComicErrorRecord>>` (`comic-common`)

```json
{
  "Garfield": [
    {
      "comicName": "Garfield",
      "comicDate": "2025-03-18",
      "source": "gocomics",
      "status": "NETWORK_ERROR",
      "errorMessage": "Connection timed out after 30s",
      "httpStatusCode": null,
      "timestamp": "2025-03-18T06:30:45-04:00",
      "retrievalDurationMs": 30000
    }
  ]
}
```

### Field Reference (ComicErrorRecord)

| Field | Type | Description |
|:---|:---|:---|
| `comicName` | `String` | Comic that failed (map key) |
| `comicDate` | `LocalDate` | Target retrieval date |
| `source` | `String` | Source provider |
| `status` | `ComicRetrievalStatus` | Error classification |
| `errorMessage` | `String` | Detailed error message |
| `httpStatusCode` | `Integer` (nullable) | HTTP status if applicable |
| `timestamp` | `OffsetDateTime` | When the error occurred |
| `retrievalDurationMs` | `long` | Failed operation duration |

The `clearOldErrors(int hoursToKeep)` method removes errors older than the specified hours.

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

## 6. combined-metrics.json

Pre-computed aggregate metrics combining storage and access data. Loaded on startup via `@PostConstruct`. Thread-safe with `ReentrantReadWriteLock`.

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

## Key Source Files

| File | Module |
|:---|:---|
| `JsonBatchExecutionTracker.java` | `comic-engine` |
| `BatchExecutionSummary.java` / `BatchStepSummary.java` | `comic-engine` |
| `JsonRetrievalStatusRepository.java` | `comic-engine` |
| `ComicRetrievalRecord.java` / `ComicRetrievalRecordStorage.java` | `comic-common` |
| `SchedulerStateService.java` | `comic-engine` |
| `JsonErrorTrackingRepository.java` | `comic-engine` |
| `ComicErrorRecord.java` | `comic-common` |
| `AccessMetricsRepository.java` | `comic-metrics` |
| `AccessMetricsData.java` | `comic-metrics` |
| `JsonMetricsRepository.java` | `comic-metrics` |
| `CombinedMetricsData.java` / `GlobalMetrics.java` / `YearlyStorageMetrics.java` | `comic-metrics` |
