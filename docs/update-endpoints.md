# Batch Job / Update API Documentation

> [!IMPORTANT]
> **GraphQL-Only**: All comic update/batch job operations use GraphQL.
> There are no REST endpoints for triggering updates.

## GraphQL Endpoint

**Endpoint:** `POST /graphql`

Update operations require authentication. Include a valid JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Mutations

### Trigger Batch Job

Trigger a batch job for comic retrieval. Downloads the latest comic strips from configured sources.

```graphql
mutation TriggerBatchJob {
  triggerBatchJob {
    job {
      executionId
      jobName
      status
      startTime
      endTime
      parameters
      steps {
        stepName
        status
        startTime
        endTime
        readCount
        writeCount
      }
    }
    errors {
      message
      field
    }
  }
}
```

### Trigger Backfill Job

Trigger a backfill job to retrieve missing comics.

```graphql
mutation TriggerBackfillJob {
  triggerBackfillJob {
    job {
      executionId
      jobName
      status
      startTime
    }
    errors {
      message
      field
    }
  }
}
```

### Trigger Job by Name

Trigger any batch job by name.

```graphql
mutation TriggerJob($jobName: String!) {
  triggerJob(jobName: $jobName) {
    job {
      executionId
      jobName
      status
      startTime
    }
    errors {
      message
      field
    }
  }
}
```

---

## Queries

### Recent Batch Jobs

Retrieve recent batch job executions across all known jobs.

```graphql
query GetRecentBatchJobs($count: Int!) {
  recentBatchJobs(count: $count) {
    executionId
    jobName
    status
    startTime
    endTime
    durationMs
    exitCode
    parameters
    steps {
      stepName
      status
      readCount
      writeCount
    }
  }
}
```

**Variables:**
```json
{
  "count": 10
}
```

---

### Batch Jobs by Date Range

Retrieve batch jobs within a specific date range.

```graphql
query GetBatchJobsByDateRange($startDate: Date!, $endDate: Date!) {
  batchJobsByDateRange(startDate: $startDate, endDate: $endDate) {
    executionId
    jobName
    status
    startTime
    endTime
    parameters
  }
}
```

---

### Specific Batch Job

Retrieve details of a specific batch job execution by ID. Returns data from H2 (in-flight jobs).

```graphql
query GetBatchJob($executionId: Int!) {
  batchJob(executionId: $executionId) {
    executionId
    jobName
    status
    startTime
    endTime
    durationMs
    exitCode
    exitDescription
    parameters
    steps {
      stepName
      status
      startTime
      endTime
      readCount
      writeCount
      filterCount
      skipCount
      commitCount
      rollbackCount
    }
  }
}
```

---

### Batch Job Summary

Retrieve summary statistics for batch jobs over a number of days.

```graphql
query GetBatchJobSummary($days: Int!) {
  batchJobSummary(days: $days) {
    periodDays
    totalExecutions
    successfulExecutions
    failedExecutions
    runningExecutions
    averageDurationMs
    totalItemsProcessed
    dailyBreakdown {
      date
      totalJobs
      successfulJobs
      failedJobs
    }
  }
}
```

---

## Types

### BatchJob

| Field            | Type          | Description                                |
|------------------|---------------|--------------------------------------------|
| executionId      | Int!          | Unique job execution ID                    |
| jobName          | String!       | Name of the batch job                      |
| status           | String!       | Current status of the job                  |
| startTime        | DateTime      | When the job started                       |
| endTime          | DateTime      | When the job completed (null if running)   |
| durationMs       | Float         | Duration in milliseconds                   |
| exitCode         | String        | Exit status code                           |
| exitDescription  | String        | Exit status description                    |
| parameters       | JSON          | Job execution parameters                   |
| steps            | [BatchStep!]! | Individual steps within the job            |

### BatchStep

| Field          | Type      | Description                          |
|----------------|-----------|--------------------------------------|
| stepName       | String!   | Name of the batch step               |
| status         | String!   | Current status of the step           |
| readCount      | Int!      | Number of items read                 |
| writeCount     | Int!      | Number of items written              |
| filterCount    | Int!      | Number of items filtered             |
| skipCount      | Int!      | Number of items skipped              |
| commitCount    | Int!      | Number of commits                    |
| rollbackCount  | Int!      | Number of rollbacks                  |
| startTime      | DateTime  | When the step started                |
| endTime        | DateTime  | When the step completed              |

### BatchJobSummary

| Field               | Type              | Description                        |
|----------------------|-------------------|------------------------------------|
| periodDays           | Int!              | Number of days in the period       |
| totalExecutions      | Int!              | Total number of executions         |
| successfulExecutions | Int!              | Number of successful executions    |
| failedExecutions     | Int!              | Number of failed executions        |
| runningExecutions    | Int!              | Number of currently running        |
| averageDurationMs    | Float             | Average duration in milliseconds   |
| totalItemsProcessed  | Int!              | Total items processed              |
| dailyBreakdown       | [DailyJobStats!]! | Daily breakdown of job statistics  |

### DailyJobStats

| Field          | Type   | Description                          |
|----------------|--------|--------------------------------------|
| date           | Date!  | Date of the stats                    |
| totalJobs      | Int!   | Number of jobs run on this date      |
| successfulJobs | Int!   | Number of successful jobs            |
| failedJobs     | Int!   | Number of failed jobs                |

---

## Persistence

Batch job history is persisted to `batch-executions.json` on NFS, surviving container restarts. The file stores a capped list of executions per job (default 30, configurable via `batch.tracking.max-history-per-job`).

In-flight job data (immediately after `triggerJob`) is read from the H2 in-memory database. Once a job completes, the `JsonBatchExecutionTracker` listener persists the execution to JSON.

---

## Background Scheduled Updates

ComicCacher includes scheduled batch jobs that automatically run on configured cron schedules. See `application.properties` for individual job cron expressions (e.g., `batch.comic-download.cron`).

---

## Error Handling

GraphQL mutations return a payload object with the result and a list of user-friendly errors:

```json
{
  "data": {
    "triggerJob": {
      "job": null,
      "errors": [
        {
          "message": "Job not available (disabled in configuration)",
          "field": "jobName"
        }
      ]
    }
  }
}
```
