# Spring Batch Jobs Migration Summary

**Date**: 2025-10-23
**Branch**: batch-job-reorganization

## Overview
Migrated all scheduled tasks to Spring Batch framework for better execution tracking, monitoring, and control.

## Requirements Completed

✅ **1. EST Timezone**: All jobs use `America/Toronto` timezone
✅ **2. Comic Download First**: 6:00 AM, then others spaced 15-30min after
✅ **3. Execution Time Tracking**: JsonBatchExecutionTracker logs duration
✅ **4. JSON Tracking File**: `batch-executions.json` (not H2)
✅ **5. Spring Batch Jobs**: All 6 jobs converted
✅ **6. Job Postfix Naming**: All end with "Job"
✅ **7. No Cluster Support**: Simple tasklet-based jobs

## Jobs Created (6 Total)

### 1. ComicDownloadJob
- **Schedule**: 6:00 AM EST daily
- **Purpose**: Downloads all enabled comics for today
- **Config**: `ComicRetrievalJobConfig`
- **Scheduler**: `ComicDownloadJobScheduler`
- **Property**: `batch.comic-download.enabled=true`
- **Cron**: `batch.comic-download.cron=0 0 6 * * ? America/Toronto`

### 2. ComicReconciliationJob
- **Schedule**: 6:15 AM EST daily
- **Purpose**: Reconciles comic config with bootstrap sources
- **Config**: `ComicReconciliationJobConfig`
- **Scheduler**: `ComicReconciliationJobScheduler`
- **Property**: `batch.reconciliation.enabled=true`
- **Cron**: `batch.reconciliation.cron=0 15 6 * * ? America/Toronto`
- **Replaces**: StartupReconciler (deleted)

### 3. MetricsArchiveJob
- **Schedule**: 6:30 AM EST daily
- **Purpose**: Archives yesterday's metrics to JSON
- **Config**: `MetricsArchiveJobConfig` (in comic-engine)
- **Scheduler**: `MetricsArchiveJobScheduler`
- **Property**: `batch.metrics-archive.enabled=true`
- **Cron**: `batch.metrics-archive.cron=0 30 6 * * ? America/Toronto`
- **Uses**: MetricsArchiveService.archiveMetricsForDate()

### 4. ImageMetadataBackfillJob
- **Schedule**: 6:30 AM EST daily
- **Purpose**: Backfills metadata for images without metadata files
- **Config**: `ImageMetadataBackfillJobConfig`
- **Scheduler**: `ImageMetadataBackfillJobScheduler`
- **Property**: `batch.image-backfill.enabled=true`
- **Cron**: `batch.image-backfill.cron=0 30 6 * * ? America/Toronto`
- **Batch Size**: `batch.image-backfill.batch-size=100`
- **Replaces**: ImageMetadataBackfillJob @Component (renamed to .old)

### 5. MetricsUpdateJob
- **Schedule**: Every 5 minutes (fixed delay)
- **Purpose**: Persists access metrics and rebuilds combined metrics
- **Config**: `MetricsUpdateJobConfig` (in comic-engine)
- **Scheduler**: `MetricsUpdateJobScheduler`
- **Property**: `batch.metrics-update.enabled=true`
- **Fixed Delay**: `batch.metrics-update.fixed-delay=300000` (5min)
- **Uses**: MetricsUpdateService.updateMetrics()

### 6. RetrievalRecordPurgeJob
- **Schedule**: 6:45 AM EST daily
- **Purpose**: Purges old retrieval records (default: 30 days retention)
- **Config**: `RetrievalRecordPurgeJobConfig`
- **Scheduler**: `RetrievalRecordPurgeJobScheduler`
- **Property**: `batch.record-purge.enabled=true`
- **Cron**: `batch.record-purge.cron=0 45 6 * * ? America/Toronto`
- **Retention**: `batch.record-purge.days-to-keep=30`

## Execution Tracking

### JsonBatchExecutionTracker
- **Location**: `comic-engine/src/main/java/org/stapledon/engine/batch/JsonBatchExecutionTracker.java`
- **Output File**: `${comics.cache.location}/batch-executions.json`
- **Property**: `batch.tracking.json-file`
- **Tracks**: Job name, status, start/end time, duration, exit code

### Execution Flow
```
06:00 AM - ComicDownloadJob (download all comics)
06:15 AM - ComicReconciliationJob (reconcile bootstrap)
06:30 AM - MetricsArchiveJob + ImageMetadataBackfillJob (parallel)
06:45 AM - RetrievalRecordPurgeJob (cleanup)
Every 5min - MetricsUpdateJob (metrics refresh)
```

## Architecture Changes

### Module Dependencies
- **comic-engine** now depends on **comic-metrics** (for metrics batch jobs)
- All batch jobs reside in comic-engine (the engine does the work)
- comic-api only contains API controllers

### Lombok Configuration
- Created `lombok.config` with `lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier`
- Allows @RequiredArgsConstructor to preserve Spring's @Qualifier annotations
- Fixes NoUniqueBeanDefinitionException with multiple Job/Step beans

### Integration Test Configuration
- Created `IntegrationTestConfig` with mock schedulers when jobs are disabled
- All batch jobs disabled in integration tests: `batch.*.enabled=false`
- Prevents JobLauncherApplicationRunner from auto-executing: `spring.batch.job.enabled=false`

## Files Deleted/Replaced

### Deleted
- `DailyRunner.java` → Replaced by ComicDownloadJobScheduler
- `StartupReconciler.java` → Replaced by ComicReconciliationJobScheduler
- `StartupReconcilerImpl.java`
- `StartupReconcilerImplTest.java`
- `StartupReconcilerProperties` dependency removed from ComicManagementFacadeImpl

### Modified (Removed @Scheduled)
- `MetricsArchiveService.archiveDailyMetrics()` - still used by batch job
- `MetricsUpdateService.updateMetrics()` - still used by batch job

### Renamed
- `ImageMetadataBackfillJob.java` → `ImageMetadataBackfillJobConfig.java` (Spring Batch version)
- Old file renamed to `.old` to avoid bean conflicts

## Configuration Properties

### New Properties Added
```properties
# Spring Batch Job Configuration
batch.timezone=America/Toronto

# Comic Download Job
batch.comic-download.enabled=true
batch.comic-download.cron=0 0 6 * * ? America/Toronto

# Reconciliation Job
batch.reconciliation.enabled=true
batch.reconciliation.cron=0 15 6 * * ? America/Toronto

# Metrics Archive Job
batch.metrics-archive.enabled=true
batch.metrics-archive.cron=0 30 6 * * ? America/Toronto

# Image Backfill Job
batch.image-backfill.enabled=true
batch.image-backfill.cron=0 30 6 * * ? America/Toronto
batch.image-backfill.batch-size=100

# Metrics Update Job
batch.metrics-update.enabled=true
batch.metrics-update.fixed-delay=300000

# Record Purge Job
batch.record-purge.enabled=true
batch.record-purge.cron=0 45 6 * * ? America/Toronto
batch.record-purge.days-to-keep=30

# Batch execution tracking
batch.tracking.json-file=${comics.cache.location}/batch-executions.json
```

### Deprecated Properties (Can be removed)
```properties
# Old properties no longer used
comics.metrics.archive-cron=...
comics.metrics.persist-interval-seconds=...
comics.metrics.backfill.enabled=...
comics.metrics.backfill.cron=...
comics.metrics.backfill.batch-size=...
dailyrunner.*
startup.reconcile.*
```

## Testing

### All Tests Passing ✅
- Unit tests: PASS
- Integration tests: PASS
- All 6 batch jobs created and tested

### Test Configuration
```properties
# Disable all batch jobs in integration tests
spring.batch.job.enabled=false
batch.comic-download.enabled=false
batch.reconciliation.enabled=false
batch.metrics-archive.enabled=false
batch.image-backfill.enabled=false
batch.metrics-update.enabled=false
batch.record-purge.enabled=false
```

## Commits

1. `77d9dfd` - Add Lombok configuration to preserve @Qualifier annotations
2. `54ac4ca` - Fix module architecture: Add comic-metrics dependency to comic-engine
3. `0c4ebe9` - Add MetricsArchiveJob Spring Batch implementation
4. `72c5e05` - Add trailing newline to IntegrationTestConfig
5. `5801eef` - Add ImageMetadataBackfillJob Spring Batch implementation
6. `607b269` - Add MetricsUpdateJob Spring Batch implementation
7. `c3f37a9` - Add RetrievalRecordPurgeJob Spring Batch implementation
8. `21ceb7b` - Add mock schedulers to IntegrationTestConfig for all batch jobs

## Benefits

1. **Unified Framework**: All jobs use Spring Batch
2. **Execution Tracking**: JSON file tracks duration, status, errors
3. **Better Control**: Enable/disable jobs via properties
4. **Consistent Naming**: All jobs end with "Job"
5. **Proper Timezone**: America/Toronto for all schedules
6. **Optimal Scheduling**: Comics first at 6am, others spaced after
7. **Monitoring**: JobLauncher allows manual triggering and monitoring
8. **No Cluster Overhead**: Simple tasklet-based jobs for personal project

## Future Enhancements

- Add job failure notifications
- Implement retry logic for failed jobs
- Add job execution history API endpoints
- Dashboard for monitoring job executions
