# Retrieval Status API

## Queries

### retrievalRecords

Get retrieval records with optional filtering, newest strip date first. The store keeps one record per comic and date (a retry replaces it) for `batch.record-purge.days-to-keep` days (default 30).

```graphql
query {
  retrievalRecords(
    comicName: String
    status: RetrievalStatusEnum
    fromDate: Date
    toDate: Date
    limit: Int = 100
  ): [RetrievalRecord!]!
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `comicName` | `String` | -- | Filter by comic name |
| `status` | `RetrievalStatusEnum` | -- | Filter by retrieval status |
| `fromDate` | `Date` | -- | Start date filter |
| `toDate` | `Date` | -- | End date filter |
| `limit` | `Int` | `100` | Maximum number of records |

**Returns:** `[RetrievalRecord!]!`

```graphql
query {
  retrievalRecords(status: SUCCESS, limit: 20) {
    id
    comicName
    comicDate
    source
    status
    retrievalDurationMs
    imageSize
  }
}
```

---

### retrievalHealth

The retrieval-status page's data: the latest daily run, today's results by source, today's errors, and each comic's final result for every day in the window. Built from in-memory state only (the comic config, each comic's `available-dates.json` index, the retrieval records and the batch history), so it reads nothing from storage.

```graphql
query {
  retrievalHealth(days: Int = 30, errorLimit: Int = 20): RetrievalHealth!
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `days` | `Int` | `30` | Days in the window, ending today in `batch.timezone`; capped at the record retention |
| `errorLimit` | `Int` | `20` | Most of today's errors to return |

A day's `outcome` is judged by the files first:

| Outcome | When |
|---|---|
| `ON_DISK` | The strip is on disk, whatever the record says. `recovered` is true when the record still says it failed |
| `PENDING` | Today, and no `ComicDownloadJob` run that started today has finished |
| `MISSING` | A dated comic: active, a publication day, not before its first strip on disk. A numbered (indexed) source: only when an attempt failed |
| `OFF_DAY` | Anything else: no strip was due |

`missingStreak` counts `MISSING` days back from the newest, skipping off days and a pending today. `stale` is true when the newest strip on disk is older than `expectedLatest`, the latest publication day that should have a strip by now; it catches a SUCCESS record with nothing on disk. `todaysErrors` are the failed records (neither SUCCESS nor COMIC_UNAVAILABLE) whose `attemptedAt` is today in `batch.timezone` (backfills of older strips included), newest first.

```graphql
query {
  retrievalHealth(days: 14) {
    targetDate
    lastRun { executionId status startTime durationMs }
    sources { source success unavailable rateLimited failed }
    todaysErrors { recovered record { comicName comicDate status httpStatusCode errorMessage } }
    comics {
      comicId
      comicName
      stale
      missingStreak
      days { date outcome recovered record { status errorMessage } }
    }
  }
}
```

---

### retrievalRecord

Get a specific retrieval record by ID.

```graphql
query {
  retrievalRecord(id: String!): RetrievalRecord
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

| Parameter | Type | Description |
|---|---|---|
| `id` | `String!` | Record ID; treat as opaque |

**Returns:** `RetrievalRecord` (null if not found)

```graphql
query {
  retrievalRecord(id: "Dilbert_2026-03-19") {
    id
    comicName
    comicDate
    source
    status
    errorMessage
    retrievalDurationMs
    imageSize
    httpStatusCode
  }
}
```

---

### retrievalSummary

Get summary statistics of retrieval operations.

```graphql
query {
  retrievalSummary(fromDate: Date, toDate: Date): RetrievalSummary!
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

| Parameter | Type | Description |
|---|---|---|
| `fromDate` | `Date` | Start date for the summary |
| `toDate` | `Date` | End date for the summary |

**Returns:** `RetrievalSummary!`

```graphql
query {
  retrievalSummary(fromDate: "2026-03-12", toDate: "2026-03-19") {
    totalAttempts
    successCount
    failureCount
    skippedCount
    successRate
    averageDurationMs
    byComic {
      comicName
      totalAttempts
      successCount
      failureCount
    }
    byStatus {
      status
      count
    }
  }
}
```

---

### retrievalRecordsForComic

Get retrieval records for a specific comic.

```graphql
query {
  retrievalRecordsForComic(comicName: String!, limit: Int = 20): [RetrievalRecord!]!
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `comicName` | `String!` | -- | Comic name |
| `limit` | `Int` | `20` | Maximum number of records |

**Returns:** `[RetrievalRecord!]!`

```graphql
query {
  retrievalRecordsForComic(comicName: "Dilbert", limit: 10) {
    id
    comicDate
    status
    retrievalDurationMs
    errorMessage
  }
}
```

---

## Mutations

### deleteRetrievalRecord

Delete a specific retrieval record.

```graphql
mutation {
  deleteRetrievalRecord(id: String!): DeleteRetrievalRecordPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

| Parameter | Type | Description |
|---|---|---|
| `id` | `String!` | Record ID; treat as opaque |

**Returns:** `DeleteRetrievalRecordPayload!` -- `{ success: Boolean!, errors: [UserError!]! }`

```graphql
mutation {
  deleteRetrievalRecord(id: "Dilbert_2026-03-19") {
    success
    errors {
      message
      code
    }
  }
}
```

---

### purgeRetrievalRecords

Purge retrieval records older than a specified number of days.

```graphql
mutation {
  purgeRetrievalRecords(daysToKeep: Int = 7): PurgeRetrievalRecordsPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `daysToKeep` | `Int` | `7` | Keep records newer than this many days |

**Returns:** `PurgeRetrievalRecordsPayload!` -- `{ purgedCount: Int!, errors: [UserError!]! }`

```graphql
mutation {
  purgeRetrievalRecords(daysToKeep: 14) {
    purgedCount
    errors {
      message
    }
  }
}
```

---

## Types

### RetrievalRecord

| Field | Type | Description |
|---|---|---|
| `id` | `String!` | Unique ID; treat as opaque (`"{comicId}_YYYY-MM-DD"`, or `"ComicName_YYYY-MM-DD"` on older records) |
| `comicId` | `Int` | Comic id (null on older records) |
| `comicName` | `String!` | Name of the comic |
| `comicDate` | `Date!` | Date the comic was retrieved for |
| `source` | `String` | Source provider (e.g., "gocomics", "comicskingdom") |
| `status` | `RetrievalStatusEnum!` | Retrieval status |
| `errorMessage` | `String` | Error message if retrieval failed |
| `retrievalDurationMs` | `Float` | Duration in milliseconds |
| `imageSize` | `Float` | Image size in bytes (if successful) |
| `httpStatusCode` | `Int` | HTTP status code from the source |
| `attemptedAt` | `DateTime` | When the attempt was made (null on older records) |

### RetrievalSummary

| Field | Type | Description |
|---|---|---|
| `totalAttempts` | `Int!` | Total retrieval attempts |
| `successCount` | `Int!` | Successful retrievals |
| `failureCount` | `Int!` | Failed retrievals, not counting `COMIC_UNAVAILABLE` |
| `skippedCount` | `Int!` | Retrievals where the source had no strip (`COMIC_UNAVAILABLE`) |
| `successRate` | `Float!` | Success rate as a percentage (0-100) of the attempts that weren't `COMIC_UNAVAILABLE` |
| `averageDurationMs` | `Float` | Average duration in ms |
| `byComic` | `[ComicRetrievalSummary!]` | Breakdown by comic |
| `byStatus` | `[StatusCount!]` | Breakdown by status |

### ComicRetrievalSummary

| Field | Type | Description |
|---|---|---|
| `comicName` | `String!` | Comic name |
| `totalAttempts` | `Int!` | Total attempts for this comic |
| `successCount` | `Int!` | Successful retrievals |
| `failureCount` | `Int!` | Failed retrievals, not counting `COMIC_UNAVAILABLE` |

### StatusCount

| Field | Type | Description |
|---|---|---|
| `status` | `RetrievalStatusEnum!` | Retrieval status |
| `count` | `Int!` | Number of records with this status |

### RetrievalStatusEnum

| Value | Description |
|---|---|
| `AUTHENTICATION_ERROR` | Authentication failed with the source |
| `COMIC_UNAVAILABLE` | Comic not available at the source |
| `NETWORK_ERROR` | Network error during retrieval |
| `PARSING_ERROR` | Failed to parse the source response |
| `RATE_LIMITED` | The source answered HTTP 429 (Too Many Requests); the source backs off and the retries ran out |
| `STORAGE_ERROR` | Failed to store the retrieved image |
| `SUCCESS` | Successfully retrieved |
| `UNKNOWN_ERROR` | Unknown error occurred |
