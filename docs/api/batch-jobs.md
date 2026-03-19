# Batch Jobs API

## Queries

### recentBatchJobs

Get recent batch job executions.

```graphql
query {
  recentBatchJobs(count: Int = 10): [BatchJob!]!
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `count` | `Int` | `10` | Number of recent jobs to return |

**Returns:** `[BatchJob!]!`

```graphql
query {
  recentBatchJobs(count: 5) {
    executionId
    jobName
    status
    startTime
    endTime
    durationMs
    exitCode
    steps {
      stepName
      status
      readCount
      writeCount
    }
  }
}
```

---

### batchJobsByDateRange

Get batch jobs within a date range.

```graphql
query {
  batchJobsByDateRange(startDate: Date!, endDate: Date!): [BatchJob!]!
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

| Parameter | Type | Description |
|---|---|---|
| `startDate` | `Date!` | Start date (YYYY-MM-DD) |
| `endDate` | `Date!` | End date (YYYY-MM-DD) |

**Returns:** `[BatchJob!]!`

```graphql
query {
  batchJobsByDateRange(startDate: "2026-03-12", endDate: "2026-03-19") {
    executionId
    jobName
    status
    startTime
    durationMs
  }
}
```

---

### batchJob

Get a specific batch job execution by ID.

```graphql
query {
  batchJob(executionId: Int!): BatchJob
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

| Parameter | Type | Description |
|---|---|---|
| `executionId` | `Int!` | Execution ID |

**Returns:** `BatchJob` (null if not found)

```graphql
query {
  batchJob(executionId: 1234) {
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
      readCount
      writeCount
      filterCount
      skipCount
      commitCount
      rollbackCount
      startTime
      endTime
    }
  }
}
```

---

### batchJobSummary

Get summary statistics for batch jobs.

```graphql
query {
  batchJobSummary(days: Int = 7): BatchJobSummary!
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `days` | `Int` | `7` | Number of days to include in the summary |

**Returns:** `BatchJobSummary!`

```graphql
query {
  batchJobSummary(days: 14) {
    daysIncluded
    totalExecutions
    successCount
    failureCount
    runningCount
    averageDurationMs
    totalItemsProcessed
    dailyBreakdown {
      date
      executionCount
      successCount
      failureCount
    }
  }
}
```

---

### batchSchedulers

Get scheduler info for all batch jobs, including pause state.

```graphql
query {
  batchSchedulers: [BatchSchedulerInfo!]!
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

**Returns:** `[BatchSchedulerInfo!]!`

```graphql
query {
  batchSchedulers {
    jobName
    cronExpression
    description
    timezone
    nextRunTime
    enabled
    paused
    lastToggled
    toggledBy
  }
}
```

---

### batchJobLog

Get the execution log for a specific batch job run.

```graphql
query {
  batchJobLog(executionId: Int!, jobName: String!): String
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

| Parameter | Type | Description |
|---|---|---|
| `executionId` | `Int!` | Execution ID |
| `jobName` | `String!` | Job name |

**Returns:** `String` (null if no log available)

```graphql
query {
  batchJobLog(executionId: 1234, jobName: "comicRetrievalJob")
}
```

---

## Mutations

### triggerBatchJob

Trigger a batch job for comic retrieval.

```graphql
mutation {
  triggerBatchJob: TriggerBatchJobPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

**Returns:** `TriggerBatchJobPayload!` -- `{ batchJob: BatchJob, errors: [UserError!]! }`

```graphql
mutation {
  triggerBatchJob {
    batchJob {
      executionId
      jobName
      status
      startTime
    }
    errors {
      message
      code
    }
  }
}
```

---

### triggerBackfillJob

Trigger a backfill job to retrieve missing comics.

```graphql
mutation {
  triggerBackfillJob: TriggerBatchJobPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

**Returns:** `TriggerBatchJobPayload!` -- `{ batchJob: BatchJob, errors: [UserError!]! }`

```graphql
mutation {
  triggerBackfillJob {
    batchJob {
      executionId
      jobName
      status
      startTime
    }
    errors {
      message
    }
  }
}
```

---

### triggerJob

Trigger any batch job by name.

```graphql
mutation {
  triggerJob(jobName: String!): TriggerBatchJobPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

| Parameter | Type | Description |
|---|---|---|
| `jobName` | `String!` | Name of the batch job to trigger |

**Returns:** `TriggerBatchJobPayload!` -- `{ batchJob: BatchJob, errors: [UserError!]! }`

```graphql
mutation {
  triggerJob(jobName: "comicRetrievalJob") {
    batchJob {
      executionId
      status
    }
    errors {
      message
    }
  }
}
```

---

### toggleJobScheduler

Pause or resume a batch job scheduler.

```graphql
mutation {
  toggleJobScheduler(jobName: String!, paused: Boolean!): ToggleJobSchedulerPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

| Parameter | Type | Description |
|---|---|---|
| `jobName` | `String!` | Job name to toggle |
| `paused` | `Boolean!` | `true` to pause, `false` to resume |

**Returns:** `ToggleJobSchedulerPayload!` -- `{ scheduler: BatchSchedulerInfo, errors: [UserError!]! }`

```graphql
mutation {
  toggleJobScheduler(jobName: "ComicDownloadJob", paused: true) {
    scheduler {
      jobName
      paused
      lastToggled
      toggledBy
    }
    errors {
      message
    }
  }
}
```

---

## Types

### BatchJob

| Field | Type | Description |
|---|---|---|
| `executionId` | `Int!` | Unique execution ID |
| `jobName` | `String!` | Job name (e.g., "comicRetrievalJob") |
| `status` | `BatchStatusEnum!` | Execution status |
| `startTime` | `DateTime!` | When the job started |
| `endTime` | `DateTime` | When the job ended (null if running) |
| `durationMs` | `Float` | Duration in milliseconds |
| `exitCode` | `String` | Exit status code |
| `exitDescription` | `String` | Exit description |
| `parameters` | `JSON` | Job parameters (key-value pairs) |
| `steps` | `[BatchStep!]` | Step execution details |

### BatchStep

| Field | Type | Description |
|---|---|---|
| `stepName` | `String!` | Step name |
| `status` | `BatchStatusEnum!` | Step status |
| `readCount` | `Int!` | Items read |
| `writeCount` | `Int!` | Items written |
| `filterCount` | `Int!` | Items filtered/skipped |
| `skipCount` | `Int!` | Items that failed |
| `commitCount` | `Int!` | Number of commits |
| `rollbackCount` | `Int!` | Number of rollbacks |
| `startTime` | `DateTime` | Step start time |
| `endTime` | `DateTime` | Step end time |

### BatchJobSummary

| Field | Type | Description |
|---|---|---|
| `daysIncluded` | `Int!` | Number of days in the summary |
| `totalExecutions` | `Int!` | Total job executions |
| `successCount` | `Int!` | Successful executions |
| `failureCount` | `Int!` | Failed executions |
| `runningCount` | `Int!` | Currently running jobs |
| `averageDurationMs` | `Float` | Average duration in ms |
| `totalItemsProcessed` | `Int` | Total items processed |
| `dailyBreakdown` | `[DailyJobStats!]` | Per-day breakdown |

### DailyJobStats

| Field | Type | Description |
|---|---|---|
| `date` | `Date!` | Date |
| `executionCount` | `Int!` | Executions on this date |
| `successCount` | `Int!` | Successful executions |
| `failureCount` | `Int!` | Failed executions |

### BatchSchedulerInfo

| Field | Type | Description |
|---|---|---|
| `jobName` | `String!` | Job name |
| `cronExpression` | `String!` | Cron expression |
| `description` | `String` | Human-readable description |
| `timezone` | `String!` | Timezone for cron evaluation |
| `nextRunTime` | `DateTime` | Next scheduled run time |
| `enabled` | `Boolean!` | Whether enabled in configuration |
| `paused` | `Boolean!` | Whether paused at runtime |
| `lastToggled` | `DateTime` | When pause state was last toggled |
| `toggledBy` | `String` | Who toggled the pause state |

### BatchStatusEnum

`STARTING`, `STARTED`, `STOPPING`, `STOPPED`, `COMPLETED`, `FAILED`, `ABANDONED`, `UNKNOWN`
