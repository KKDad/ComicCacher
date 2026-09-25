# Batch Jobs

## Shared Infrastructure

All batch jobs are built on Spring Batch and share a common scheduler framework in `comic-engine`.

### AbstractJobScheduler

Base class for all schedulers. Provides:

- Job execution via `JobOperator.start(Job, JobParameters)` (Spring Batch 6 API)
- Automatic `runId` parameter generation with timestamp for unique executions
- `trigger` parameter tracking (`SCHEDULED`, `MANUAL`, `STARTUP_MAKEUP`)
- Logging of initialization and execution start/completion

Two concrete subclasses:

| Subclass | Schedule Type | Key Behavior |
|----------|--------------|--------------|
| `DailyJobScheduler` | `DAILY` | Cron-based, missed execution detection, duplicate run prevention, pause/resume |
| `PeriodicJobScheduler` | `PERIODIC` | Fixed-delay interval, no missed execution logic |

### DailyJobScheduler

Constructor signature:

```java
public DailyJobScheduler(
    Job job,
    String cronExpression,
    String timezone,
    JobOperator jobOperator,
    JsonBatchExecutionTracker executionTracker)

public DailyJobScheduler(
    Job job,
    String cronExpression,
    String timezone,
    JobOperator jobOperator,
    JsonBatchExecutionTracker executionTracker,
    String description)
```

Key methods:

- `executeScheduled()` -- Called by `SchedulerTriggers`. Checks pause state and whether the job already ran today before executing.
- `triggerManually()` -- For API-driven manual runs. Bypasses the "already ran today" check.
- `runMissedExecutionIfNeeded()` -- Called by `StartupJobRunner` on application startup. Compares current time against the cron schedule; if past the scheduled time and job hasn't run today, triggers a `STARTUP_MAKEUP` run.

### PeriodicJobScheduler

Constructor signature:

```java
public PeriodicJobScheduler(
    Job job,
    long fixedDelayMs,
    JobOperator jobOperator)
```

Available infrastructure for fixed-delay jobs. Currently unused -- all 6 jobs use `DailyJobScheduler`.

### JsonBatchExecutionTracker

Implements `JobExecutionListener`. Automatically captures execution data after each job completion and persists it to `batch-executions.json` in the cache directory.

- Stores a capped list of executions per job (configurable via `batch.tracking.max-history-per-job`, default 30)
- Handles migration from legacy single-entry format to list format
- Uses atomic write (`NfsFileOperations.atomicWrite`) for NFS safety
- Sets MDC context (`batchJobName`, `batchJobExecutionId`, `batchLogPath`) for structured logging
- Provides query methods: `getLastExecution()`, `getExecutionHistory()`, `getAllExecutionHistory()`, `hasJobRunToday()`, `hasJobRunSince()`

Each execution is captured as a `BatchExecutionSummary` containing: execution ID, job name, status, exit code, start/end times, parameters, step summaries (`BatchStepSummary` with read/write/filter/skip/commit/rollback counts), and error messages.

### SchedulerStateService

Manages runtime pause/resume state for schedulers. State is persisted to `scheduler-state.json` so it survives restarts. Uses a `SchedulerState` record containing `paused`, `lastToggled`, and `toggledBy` fields.

### SchedulerStateWiring

A `@PostConstruct` component that injects `SchedulerStateService` into all `DailyJobScheduler` beans after construction. This avoids circular dependency issues.

### StartupJobRunner

Listens for `ApplicationReadyEvent` (ordered at 100) to check for missed job executions. Iterates all `DailyJobScheduler` beans and calls `runMissedExecutionIfNeeded()`. This runs after all beans are fully initialized, avoiding race conditions with strategy registration.

### SchedulerTriggers

Centralized `@Component` that holds `@Scheduled` methods for all 6 jobs. Each method delegates to the corresponding `DailyJobScheduler.executeScheduled()`. Individual triggers are gated by `@ConditionalOnProperty`.

### BatchJobBaseConfig

Constants class containing:

- `BATCH_TIMEZONE` = `"America/Toronto"`
- `KNOWN_JOBS` set (used by health checks to detect missing/unexpected schedulers)
- `CronSchedules` inner class with default cron expressions
- `PropertyKeys` inner class with property key constants

## Job Configurations

All jobs follow the same pattern: a `@Configuration` class that defines a `Job` bean, one or more `Step` beans, and a `DailyJobScheduler` bean. All jobs register `JsonBatchExecutionTracker` as a listener. Job instance uniqueness is handled by `AbstractJobScheduler.buildJobParameters()`, which generates a timestamp-based `runId` for each execution.

### ComicDownloadJob

**Purpose:** Downloads today's comic strips from all enabled sources.

**Config class:** `ComicRetrievalJobConfig`

**Pattern:** Chunk-oriented (Reader/Processor/Writer) with chunk size 1.

- **Reader:** `ListItemReader<LocalDate>` providing `LocalDate.now()`
- **Processor:** Calls `managementFacade.updateComicsForDate(date)` which iterates all active comics, filters by publication day, and downloads each strip
- **Writer:** Logs success/failure per comic

**Data source:** `ManagementFacade` -> `DownloaderFacade` -> GoComics/ComicsKingdom web scraping

### ComicBackfillJob

**Purpose:** Backfills missing comic strips, recent days first, then older gaps.

**Config class:** `ComicBackfillJobConfig`

**Schedule:** Runs at every cron time (default `0 30 7-19/2 * * ?`, every 2 h from 90 minutes after the daily download until evening). A run that would overlap one still in progress is not launched. Before each scheduled run, `ComicBackfillService.hasMissingStrips()` checks local storage only. With `batch.comic-backfill.remember-cached-strips=true` (off when unset), strips it finds on disk are remembered for the rest of the day, so later checks and the run's own scan only look at the gaps again. This memory only decides which dates are scanned; it never serves images. The recent window is always rechecked on disk. Logging:
  - A remembered strip that has gone missing logs `Backfill cached-strip memory was wrong: ...` at WARN, and all of that comic's remembered strips are forgotten.
  - Each scan logs `Backfill scan: N dates checked on disk, M skipped as remembered on disk, K memory mismatches (...)`, at INFO for a run and at DEBUG for the pre-run check.
  - The daily reset logs `Backfill cached-strip memory reset for ...` at INFO.
  - Set the property to `false` to check every date on disk on every scan. If nothing is missing, the run is skipped: no web requests, and no execution recorded. There is no catch-up run at startup; the next cron time picks up the work. Manual triggers always run.

**Pattern:** Chunk-oriented with configurable chunk size (`batch.comic-backfill.chunk-size`, default 10).

- **Reader:** `ListItemReader<BackfillTask>` from `ComicBackfillService.findMissingStrips()` (step-scoped, evaluated at job run time). Per source, up to `max-per-run` tasks (capped by what's left of the optional `max-per-day`):
  1. **Recent pass:** the last `recent-days` (default 7) days, newest first, across every comic.
  2. **History pass:** older gaps, round-robin one per comic per round, so comics early in the alphabet can't take the whole budget.
- **Processor:** Calls `managementFacade.downloadComicForDate(comic, date, true)` per task, with a delay between downloads (`batch.comic-backfill.delay-between-comics-ms`, 10000 ms in `application.properties`). The `true` makes an HTTP 429 fail at once. The source still backs off through `SourceThrottleService`, and the processor skips that source's remaining tasks for the rest of the run. Strip-number tasks (Freefall) stop the same way on a 429.
- **Writer:** Logs per-chunk success, duplicate and failure counts, then flushes `BackfillStateService` (also flushed when the step ends)
- **Job parameters:** `source` (filter), `resetState=true` (forget learned state before the run)

**Learned state (`backfill-state.json`, `BackfillStateService`):**
- A date that comes back unavailable, or as a duplicate of another date's image, `give-up-after` times (default 2) is skipped.
- `horizon-consecutive-failures` (default 3) different dates in a row, older than the recent window, coming back unavailable set a **comic horizon**: dates on or before the newest of them are no longer scanned.
- When `horizon-min-comics` (default 3) comics on one source have horizons within `horizon-tolerance-days` (default 2) of each other, that age becomes the **source horizon**, for example a paywall after about 7 days.
- Given-up dates and horizons expire after `retry-given-up-after-days` (default 30). A successful download older than a horizon clears it.
- Transient errors and 429s are never recorded. An HTTP 404 or 410 counts as unavailable.
- The state is kept in memory and written once per chunk; expired entries are dropped when it is written.

**Data source:** `ComicBackfillService` identifies gaps; `ManagementFacade` downloads individual strips; `BackfillStateService` remembers what didn't work

### AvatarBackfillJob

**Purpose:** Downloads missing avatar images for all comics that have a source configured.

**Config class:** `AvatarBackfillJobConfig`

**Pattern:** Tasklet (single step).

- Delegates to `managementFacade.downloadMissingAvatars()`
- Configurable delay between downloads (`batch.avatar-backfill.delay-between-downloads-ms`, default 2000ms)

**Data source:** `ManagementFacade` -> `DownloaderFacade` avatar download

### ImageMetadataBackfillJob

**Purpose:** Recalculates image dimensions and format metadata for existing images that lack metadata files.

**Config class:** `ImageMetadataBackfillJobConfig`

**Pattern:** Tasklet (single step) with streaming file walk.

- Walks the cache directory tree, filters for image files without metadata
- Validates each image via `ImageValidationService`
- Analyzes via `ImageAnalysisService` (color mode detection)
- Saves metadata via `ImageMetadataRepository`
- Processes up to `batchSize * 100` images per run (configurable via `batch.image-backfill.batch-size`, default 100)
- Lazy-initializes a comic directory map from `ComicConfigurationService` for O(1) comic ID lookups

**Data source:** Filesystem walk of cache directory

### MetricsArchiveJob

**Purpose:** Archives yesterday's access and storage metrics to persistent JSON storage.

**Config class:** `MetricsArchiveJobConfig`

**Pattern:** Tasklet (single step).

- Delegates to `metricsArchiveService.archiveMetricsForDate(yesterday)`
- Throws `IllegalStateException` on failure to mark the job as FAILED

**Data source:** `MetricsArchiveService` (from comic-metrics module), which builds combined metrics on demand via `MetricsUpdateService.buildCombinedMetrics()` and prunes archives past `comics.metrics.history-retention-days`

### RetrievalRecordPurgeJob

**Purpose:** Purges old retrieval records and batch log files beyond the retention window to prevent unbounded growth.

**Config class:** `RetrievalRecordPurgeJobConfig`

**Pattern:** Tasklet (two steps).

- **Step 1 — recordPurgeStep:** Delegates to `comicManagementFacade.purgeOldRetrievalRecords(daysToKeep)`
- **Step 2 — logPurgeStep:** Delegates to `batchJobLogService.purgeOldLogFiles(daysToKeep)` to delete old per-execution log files from `batch-logs/`
- Configurable retention via `batch.record-purge.days-to-keep` (default 30)

**Data source:** `ManagementFacade` -> `RetrievalStatusService`, `BatchJobLogService`

## Comparison Table

| Job | Pattern | Default Cron | Enabled Default | Data Source | Key Dependencies |
|-----|---------|-------------|----------------|-------------|-----------------|
| ComicDownloadJob | Chunk (R/P/W) | `0 0 6 * * ?` | `true` | Web scraping (GoComics, ComicsKingdom) | `ManagementFacade` |
| ComicBackfillJob | Chunk (R/P/W) | `0 30 7-19/2 * * ?` (several runs a day) | `true` | `ComicBackfillService` gap detection | `ManagementFacade`, `ComicBackfillService` |
| AvatarBackfillJob | Tasklet | `0 15 7 * * ?` | `false` | Web scraping (avatar pages) | `ManagementFacade` |
| ImageMetadataBackfillJob | Tasklet | `0 30 6 * * ?` | `true` | Filesystem walk | `ValidationService`, `AnalysisService`, `ImageMetadataRepository` |
| MetricsArchiveJob | Tasklet | `0 30 6 * * ?` | `true` | Combined metrics built on demand | `MetricsArchiveService` |
| RetrievalRecordPurgeJob | Tasklet (2 steps) | `0 45 6 * * ?` | `true` | JSON retrieval records, batch log files | `ManagementFacade`, `BatchJobLogService` |

All jobs run in `America/Toronto` timezone. Cron expressions are configurable via `batch.<job-key>.cron` properties.

## Execution Flow

```mermaid
sequenceDiagram
    participant Spring as Spring Scheduler
    participant ST as SchedulerTriggers
    participant DJS as DailyJobScheduler
    participant SSS as SchedulerStateService
    participant JBET as JsonBatchExecutionTracker
    participant JO as JobOperator
    participant Job as Spring Batch Job

    Spring->>ST: @Scheduled triggers
    ST->>DJS: executeScheduled()
    DJS->>SSS: isPaused(jobName)?
    alt Paused
        DJS-->>ST: skip
    else Not paused
        DJS->>JBET: hasJobRunToday(jobName)?
        alt Already ran
            DJS-->>ST: skip
        else Not yet
            DJS->>JO: start(job, parameters)
            JO->>Job: Execute steps
            Job-->>JBET: afterJob(execution)
            JBET->>JBET: Write to batch-executions.json
        end
    end
```

## Startup Makeup Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant SJR as StartupJobRunner
    participant DJS as DailyJobScheduler
    participant JBET as JsonBatchExecutionTracker

    App->>SJR: ApplicationReadyEvent
    loop For each DailyJobScheduler
        SJR->>DJS: runMissedExecutionIfNeeded()
        DJS->>JBET: hasJobRunToday(jobName)?
        alt Already ran today
            DJS-->>SJR: no action
        else Hasn't run
            DJS->>DJS: Parse cron, check if past scheduled time
            alt Past scheduled time
                DJS->>DJS: runJob("STARTUP_MAKEUP")
            end
        end
    end
```
