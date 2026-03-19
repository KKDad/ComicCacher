# Retrieval Status API

## Queries

### retrievalRecords

Get retrieval records with optional filtering. Returns records from the last 7 days by default.

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
| `id` | `String!` | Record ID (format: `"ComicName_YYYY-MM-DD"`) |

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
| `id` | `String!` | Record ID (format: `"ComicName_YYYY-MM-DD"`) |

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
| `id` | `String!` | Unique ID (format: `"ComicName_YYYY-MM-DD"`) |
| `comicName` | `String!` | Name of the comic |
| `comicDate` | `Date!` | Date the comic was retrieved for |
| `source` | `String` | Source provider (e.g., "gocomics", "comicskingdom") |
| `status` | `RetrievalStatusEnum!` | Retrieval status |
| `errorMessage` | `String` | Error message if retrieval failed |
| `retrievalDurationMs` | `Float` | Duration in milliseconds |
| `imageSize` | `Float` | Image size in bytes (if successful) |
| `httpStatusCode` | `Int` | HTTP status code from the source |

### RetrievalSummary

| Field | Type | Description |
|---|---|---|
| `totalAttempts` | `Int!` | Total retrieval attempts |
| `successCount` | `Int!` | Successful retrievals |
| `failureCount` | `Int!` | Failed retrievals |
| `skippedCount` | `Int!` | Skipped retrievals |
| `successRate` | `Float!` | Success rate as percentage (0-100) |
| `averageDurationMs` | `Float` | Average duration in ms |
| `byComic` | `[ComicRetrievalSummary!]` | Breakdown by comic |
| `byStatus` | `[StatusCount!]` | Breakdown by status |

### ComicRetrievalSummary

| Field | Type | Description |
|---|---|---|
| `comicName` | `String!` | Comic name |
| `totalAttempts` | `Int!` | Total attempts for this comic |
| `successCount` | `Int!` | Successful retrievals |
| `failureCount` | `Int!` | Failed retrievals |

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
| `STORAGE_ERROR` | Failed to store the retrieved image |
| `SUCCESS` | Successfully retrieved |
| `UNKNOWN_ERROR` | Unknown error occurred |
