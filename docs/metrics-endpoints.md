# Metrics API Documentation

> [!IMPORTANT]
> **GraphQL-Only**: All metrics operations use GraphQL.
> There are no REST endpoints for metrics.

## GraphQL Endpoint

**Endpoint:** `POST /graphql`

Metrics queries require authentication. Include a valid JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Queries

### Storage Metrics

Retrieve storage metrics for all cached comics, including disk usage and image counts.

```graphql
query GetStorageMetrics {
  storageMetrics {
    totalBytes
    comicCount
    comics {
      comicName
      totalBytes
      imageCount
      yearlyBreakdown {
        year
        bytes
        imageCount
      }
    }
    lastUpdated
  }
}
```

**Response:**
```json
{
  "data": {
    "storageMetrics": {
      "totalBytes": 125000000,
      "comicCount": 10,
      "comics": [
        {
          "comicName": "Garfield",
          "totalBytes": 40000000,
          "imageCount": 1500,
          "yearlyBreakdown": [
            { "year": 1989, "bytes": 5000000, "imageCount": 0 },
            { "year": 2023, "bytes": 2000000, "imageCount": 0 }
          ]
        }
      ],
      "lastUpdated": "2024-01-15T10:30:00Z"
    }
  }
}
```

---

### Access Metrics

Retrieve metrics about how comics are being accessed, including frequency and patterns.

```graphql
query GetAccessMetrics {
  accessMetrics {
    totalAccesses
    comics {
      comicName
      accessCount
      averageAccessTimeMs
      lastAccessed
    }
    lastUpdated
  }
}
```

**Response:**
```json
{
  "data": {
    "accessMetrics": {
      "totalAccesses": 7700,
      "comics": [
        {
          "comicName": "Dilbert",
          "accessCount": 2500,
          "averageAccessTimeMs": 45.7,
          "lastAccessed": "2024-01-15T09:45:12Z"
        },
        {
          "comicName": "Calvin and Hobbes",
          "accessCount": 5200,
          "averageAccessTimeMs": 38.2,
          "lastAccessed": "2024-01-15T10:12:33Z"
        }
      ],
      "lastUpdated": "2024-01-15T10:30:00Z"
    }
  }
}
```

---

### Combined Metrics

Retrieve comprehensive metrics combining both storage and access data.

```graphql
query GetCombinedMetrics {
  combinedMetrics {
    storage {
      totalBytes
      comicCount
      comics {
        comicName
        totalBytes
        imageCount
      }
    }
    access {
      totalAccesses
      comics {
        comicName
        accessCount
        averageAccessTimeMs
        lastAccessed
      }
    }
    lastUpdated
  }
}
```

**Response:**
```json
{
  "data": {
    "combinedMetrics": {
      "storage": {
        "totalBytes": 125000000,
        "comicCount": 10,
        "comics": [
          {
            "comicName": "Garfield",
            "totalBytes": 40000000,
            "imageCount": 1500
          }
        ]
      },
      "access": {
        "totalAccesses": 7700,
        "comics": [
          {
            "comicName": "Garfield",
            "accessCount": 2500,
            "averageAccessTimeMs": 45.7,
            "lastAccessed": "2024-01-15T09:45:12Z"
          }
        ]
      },
      "lastUpdated": "2024-01-15T10:30:00Z"
    }
  }
}
```

---

## Mutations

### Refresh Storage Metrics

Force an update of storage metrics, useful after large changes to the cache.

```graphql
mutation RefreshStorageMetrics {
  refreshStorageMetrics {
    totalBytes
    comicCount
    lastUpdated
  }
}
```

**Response:**
```json
{
  "data": {
    "refreshStorageMetrics": {
      "totalBytes": 126000000,
      "comicCount": 10,
      "lastUpdated": "2024-01-15T11:00:00Z"
    }
  }
}
```

---

### Refresh All Metrics

Force a refresh of all metrics (storage, access, and combined).

```graphql
mutation RefreshAllMetrics {
  refreshAllMetrics
}
```

**Response:**
```json
{
  "data": {
    "refreshAllMetrics": true
  }
}
```

---

## Types

### StorageMetrics

| Field       | Type                   | Description                           |
|-------------|------------------------|---------------------------------------|
| totalBytes  | Float                  | Total storage used (in bytes)         |
| comicCount  | Int                    | Number of comics being tracked        |
| comics      | [ComicStorageMetric!]  | Per-comic storage breakdown           |
| lastUpdated | DateTime               | Last time metrics were calculated     |

### ComicStorageMetric

| Field           | Type                    | Description                              |
|-----------------|-------------------------|------------------------------------------|
| comicName       | String!                 | Name of the comic                        |
| totalBytes      | Float!                  | Total storage used for this comic        |
| imageCount      | Int!                    | Number of cached images for this comic   |
| yearlyBreakdown | [YearlyStorageMetric!]  | Storage broken down by year              |

### YearlyStorageMetric

| Field      | Type  | Description                          |
|------------|-------|--------------------------------------|
| year       | Int!  | Year                                 |
| bytes      | Float!| Storage used in bytes for this year  |
| imageCount | Int!  | Number of images for this year       |

### AccessMetrics

| Field         | Type                   | Description                          |
|---------------|------------------------|--------------------------------------|
| totalAccesses | Int                    | Total number of access events        |
| comics        | [ComicAccessMetric!]   | Per-comic access breakdown           |
| lastUpdated   | DateTime               | Last time metrics were updated       |

### ComicAccessMetric

| Field              | Type     | Description                                   |
|--------------------|----------|-----------------------------------------------|
| comicName          | String!  | Name of the comic                             |
| accessCount        | Int!     | Total number of accesses for this comic       |
| averageAccessTimeMs| Float    | Average access time in milliseconds           |
| lastAccessed       | DateTime | Timestamp of the most recent access           |

### CombinedMetrics

| Field       | Type           | Description                          |
|-------------|----------------|--------------------------------------|
| storage     | StorageMetrics | Storage metrics                      |
| access      | AccessMetrics  | Access metrics                       |
| lastUpdated | DateTime       | Last time combined metrics were calculated |

---

## Error Handling

GraphQL errors follow the standard format:

### Unauthorized

```json
{
  "data": null,
  "errors": [
    {
      "message": "Authentication required",
      "extensions": {
        "classification": "UNAUTHORIZED"
      }
    }
  ]
}
```

---

## Use Cases

1. **Storage Management**: Monitor comic cache size and growth over time via `storageMetrics` query
2. **Popularity Analysis**: Determine which comics are most frequently accessed via `accessMetrics` query
3. **Optimization**: Identify opportunities to optimize storage based on access patterns via `combinedMetrics` query
4. **Capacity Planning**: Plan for storage needs based on comic download patterns
5. **Troubleshooting**: Diagnose issues related to comic caching and storage
6. **Cache Refresh**: Force metrics recalculation after bulk operations via `refreshStorageMetrics` or `refreshAllMetrics` mutations
