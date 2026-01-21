# Retrieval Status API Documentation

> [!IMPORTANT]
> **GraphQL Migration Complete**: All retrieval status operations now use GraphQL.
> There are no REST endpoints for retrieval status operations.

## GraphQL Endpoint

**Endpoint:** `POST /graphql`

All retrieval status queries and mutations use the GraphQL endpoint.

---

## Queries

### Get Retrieval Records

Query retrieval records with optional filtering. Returns records from the last 7 days.

```graphql
query GetRetrievalRecords($comicName: String, $status: RetrievalStatusEnum, $fromDate: Date, $toDate: Date, $limit: Int) {
  retrievalRecords(
    comicName: $comicName
    status: $status
    fromDate: $fromDate
    toDate: $toDate
    limit: $limit
  ) {
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

**Variables:**
| Variable   | Type                 | Required | Default | Description                       |
|------------|----------------------|----------|---------|-----------------------------------|
| comicName  | String               | No       | -       | Filter by comic name              |
| status     | RetrievalStatusEnum  | No       | -       | Filter by status                  |
| fromDate   | Date                 | No       | -       | Filter by from date (inclusive)   |
| toDate     | Date                 | No       | -       | Filter by to date (inclusive)     |
| limit      | Int                  | No       | 100     | Maximum records to return         |

---

### Get Specific Retrieval Record

Query a single retrieval record by ID.

```graphql
query GetRetrievalRecord($id: String!) {
  retrievalRecord(id: $id) {
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

### Get Retrieval Summary

Query aggregated statistics for retrieval operations.

```graphql
query GetRetrievalSummary($fromDate: Date, $toDate: Date) {
  retrievalSummary(fromDate: $fromDate, toDate: $toDate) {
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

### Get Retrieval Records for Comic

Query retrieval records for a specific comic.

```graphql
query GetRetrievalRecordsForComic($comicName: String!, $limit: Int) {
  retrievalRecordsForComic(comicName: $comicName, limit: $limit) {
    id
    comicDate
    status
    errorMessage
    retrievalDurationMs
  }
}
```

---

## Mutations

### Delete Retrieval Record

Delete a specific retrieval record. Requires ADMIN role.

```graphql
mutation DeleteRetrievalRecord($id: String!) {
  deleteRetrievalRecord(id: $id)
}
```

**Returns:** `Boolean!` - true if deleted successfully

---

### Purge Old Records

Purge retrieval records older than specified days. Requires ADMIN role.

```graphql
mutation PurgeRetrievalRecords($daysToKeep: Int) {
  purgeRetrievalRecords(daysToKeep: $daysToKeep)
}
```

**Variables:**
| Variable    | Type | Required | Default | Description                  |
|-------------|------|----------|---------|------------------------------|
| daysToKeep  | Int  | No       | 7       | Days of records to keep      |

**Returns:** `Int!` - number of records purged

---

## Retrieval Status Enum

```graphql
enum RetrievalStatusEnum {
  SUCCESS
  FAILURE
  SKIPPED
  ERROR
  NOT_FOUND
  RATE_LIMITED
}
```

| Value        | Description                                      |
|--------------|--------------------------------------------------|
| SUCCESS      | Comic retrieved successfully                     |
| FAILURE      | General retrieval failure                        |
| SKIPPED      | Retrieval skipped (e.g., already exists)         |
| ERROR        | Error during retrieval process                   |
| NOT_FOUND    | Comic not found at source                        |
| RATE_LIMITED | Request was rate limited by source               |

---

## Response Types

### RetrievalRecord

| Field               | Type                | Description                              |
|---------------------|---------------------|------------------------------------------|
| id                  | String!             | Unique ID (format: "ComicName_YYYY-MM-DD")|
| comicName           | String!             | Name of the comic                        |
| comicDate           | Date!               | Date of the comic strip                  |
| source              | String              | Source provider (e.g., "gocomics")       |
| status              | RetrievalStatusEnum!| Retrieval status                         |
| errorMessage        | String              | Error message if failed                  |
| retrievalDurationMs | Float               | Duration in milliseconds                 |
| imageSize           | Float               | Image size in bytes                      |
| httpStatusCode      | Int                 | HTTP status from source                  |

### RetrievalSummary

| Field             | Type                     | Description                     |
|-------------------|--------------------------|---------------------------------|
| totalAttempts     | Int!                     | Total retrieval attempts        |
| successCount      | Int!                     | Successful retrievals           |
| failureCount      | Int!                     | Failed retrievals               |
| skippedCount      | Int!                     | Skipped retrievals              |
| successRate       | Float!                   | Success rate (0-100)            |
| averageDurationMs | Float                    | Average duration in ms          |
| byComic           | [ComicRetrievalSummary!] | Breakdown by comic              |
| byStatus          | [StatusCount!]           | Breakdown by status             |

---

## Error Handling

GraphQL errors follow the standard format:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Retrieval record not found",
      "path": ["retrievalRecord"],
      "extensions": {
        "classification": "NOT_FOUND"
      }
    }
  ]
}
```

---

## Authorization

- **Queries**: Require authentication (valid JWT token)
- **Mutations**: Require ADMIN role

Include JWT token in the Authorization header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```