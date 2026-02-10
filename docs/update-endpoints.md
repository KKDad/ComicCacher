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
mutation TriggerBatchJob($jobName: String, $parameters: JSON) {
  triggerBatchJob(jobName: $jobName, parameters: $parameters) {
    id
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
      recordsProcessed
      errorMessage
    }
  }
}
```

**Variables:**
```json
{
  "jobName": "dailyComicRetrieval",
  "parameters": {
    "targetDate": "2023-05-01",
    "comicIds": [1, 2, 3]
  }
}
```

**Response:**
```json
{
  "data": {
    "triggerBatchJob": {
      "id": "job-12345",
      "jobName": "dailyComicRetrieval",
      "status": "STARTED",
      "startTime": "2023-05-01T10:15:30Z",
      "endTime": null,
      "parameters": {
        "targetDate": "2023-05-01",
        "comicIds": [1, 2, 3]
      },
      "steps": []
    }
  }
}
```

---

### Trigger Backfill Job

Trigger a backfill job to retrieve missing comics from a date range.

```graphql
mutation TriggerBackfillJob($comicId: ID!, $startDate: Date!, $endDate: Date!) {
  triggerBackfillJob(comicId: $comicId, startDate: $startDate, endDate: $endDate) {
    id
    jobName
    status
    startTime
    parameters
  }
}
```

**Variables:**
```json
{
  "comicId": "1",
  "startDate": "2023-04-01",
  "endDate": "2023-04-30"
}
```

**Response:**
```json
{
  "data": {
    "triggerBackfillJob": {
      "id": "backfill-67890",
      "jobName": "comicBackfill",
      "status": "STARTED",
      "startTime": "2023-05-01T10:20:00Z",
      "parameters": {
        "comicId": 1,
        "startDate": "2023-04-01",
        "endDate": "2023-04-30"
      }
    }
  }
}
```

---

## Queries

### Recent Batch Jobs

Retrieve recent batch job executions.

```graphql
query GetRecentBatchJobs($limit: Int) {
  recentBatchJobs(limit: $limit) {
    id
    jobName
    status
    startTime
    endTime
    parameters
  }
}
```

**Variables:**
```json
{
  "limit": 10
}
```

---

### Batch Jobs by Date Range

Retrieve batch jobs within a specific date range.

```graphql
query GetBatchJobsByDateRange($startDate: Date!, $endDate: Date!) {
  batchJobsByDateRange(startDate: $startDate, endDate: $endDate) {
    id
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

Retrieve details of a specific batch job execution.

```graphql
query GetBatchJob($id: ID!) {
  batchJob(id: $id) {
    id
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
      recordsProcessed
      errorMessage
    }
  }
}
```

---

### Batch Job Summary

Retrieve summary statistics for batch jobs.

```graphql
query GetBatchJobSummary($days: Int) {
  batchJobSummary(days: $days) {
    totalJobs
    successfulJobs
    failedJobs
    averageDuration
    dailyStats {
      date
      jobsRun
      successRate
    }
  }
}
```

---

## Types

### BatchJob

| Field      | Type          | Description                                |
|------------|---------------|--------------------------------------------|
| id         | ID!           | Unique job execution ID                    |
| jobName    | String!       | Name of the batch job                      |
| status     | BatchStatus!  | Current status of the job                  |
| startTime  | DateTime!     | When the job started                       |
| endTime    | DateTime      | When the job completed (null if running)   |
| parameters | JSON          | Job execution parameters                   |
| steps      | [BatchStep!]! | Individual steps within the job            |

### BatchStep

| Field             | Type          | Description                          |
|-------------------|---------------|--------------------------------------|
| stepName          | String!       | Name of the batch step               |
| status            | BatchStatus!  | Current status of the step           |
| startTime         | DateTime!     | When the step started                |
| endTime           | DateTime      | When the step completed              |
| recordsProcessed  | Int           | Number of records processed          |
| errorMessage      | String        | Error message if step failed         |

### BatchStatus (Enum)

| Value      | Description                     |
|------------|---------------------------------|
| PENDING    | Job is queued but not started   |
| STARTED    | Job is currently running        |
| COMPLETED  | Job completed successfully      |
| FAILED     | Job failed with errors          |
| STOPPED    | Job was manually stopped        |

### BatchJobSummary

| Field           | Type              | Description                        |
|-----------------|-------------------|------------------------------------|
| totalJobs       | Int!              | Total number of jobs               |
| successfulJobs  | Int!              | Number of successful jobs          |
| failedJobs      | Int!              | Number of failed jobs              |
| averageDuration | Float             | Average job duration in ms         |
| dailyStats      | [DailyJobStats!]! | Daily breakdown of job statistics  |

### DailyJobStats

| Field       | Type    | Description                        |
|-------------|---------|------------------------------------|
| date        | Date!   | Date of the stats                  |
| jobsRun     | Int!    | Number of jobs run on this date    |
| successRate | Float!  | Success rate percentage (0-100)    |

---

## Background Scheduled Updates

ComicCacher includes a daily scheduled update process that automatically downloads new comics. The schedule is configured in the application properties.

**Configuration Properties:**

```properties
# Enable or disable scheduled updates
app.daily-runner.enabled=true

# Cron expression for the update schedule (default: 6:00 AM daily)
app.daily-runner.cron=0 0 6 * * ?

# Maximum number of comics to update in parallel
app.daily-runner.max-parallel-updates=3

# Whether to perform a catch-up for missed days
app.daily-runner.catch-up-enabled=true

# Maximum number of days to catch up
app.daily-runner.max-catch-up-days=7
```

---

## Error Handling

GraphQL errors follow the standard format:

### Job Not Found

```json
{
  "data": null,
  "errors": [
    {
      "message": "Batch job not found: job-12345",
      "extensions": {
        "classification": "NOT_FOUND"
      }
    }
  ]
}
```

### Job Execution Failed

```json
{
  "data": null,
  "errors": [
    {
      "message": "Failed to start batch job",
      "extensions": {
        "classification": "INTERNAL_ERROR"
      }
    }
  ]
}
```

---

## Use Cases

1. **Manual Updates**: Trigger updates on demand for specific comics via `triggerBatchJob` mutation
2. **Batch Updates**: Update all comics in a single operation
3. **Backfill Missing Data**: Retrieve missing comics for a date range via `triggerBackfillJob` mutation
4. **Integration**: Allow external systems to trigger comic updates
5. **Monitoring**: Track batch job status and history via `recentBatchJobs` and `batchJob` queries
6. **Troubleshooting**: View job execution details and errors via `batchJob` query with steps
