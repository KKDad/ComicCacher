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
    totalComics
    totalImages
    totalStorageBytes
    averageImageSize
    comicMetrics {
      comicId
      imageCount
      storageBytes
      oldestImage
      newestImage
      averageImageSize
      storageByYear
    }
  }
}
```

**Response:**
```json
{
  "data": {
    "storageMetrics": {
      "totalComics": 10,
      "totalImages": 5000,
      "totalStorageBytes": 125000000,
      "averageImageSize": 25000,
      "comicMetrics": [
        {
          "comicId": 1,
          "imageCount": 1500,
          "storageBytes": 40000000,
          "oldestImage": "1989-04-16",
          "newestImage": "2023-05-01",
          "averageImageSize": 26666,
          "storageByYear": {
            "1989": 5000000,
            "1990": 8000000,
            "2023": 2000000
          }
        }
      ]
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
    comicMetrics {
      comicId
      comicName
      accessCount
      lastAccess
      averageAccessTime
      hitRatio
    }
  }
}
```

**Response:**
```json
{
  "data": {
    "accessMetrics": {
      "comicMetrics": [
        {
          "comicId": 1,
          "comicName": "Dilbert",
          "accessCount": 2500,
          "lastAccess": "2023-05-01T09:45:12Z",
          "averageAccessTime": 45.7,
          "hitRatio": 0.98
        },
        {
          "comicId": 2,
          "comicName": "Calvin and Hobbes",
          "accessCount": 5200,
          "lastAccess": "2023-05-01T10:12:33Z",
          "averageAccessTime": 38.2,
          "hitRatio": 0.99
        }
      ]
    }
  }
}
```

---

### Combined Metrics

Retrieve comprehensive metrics combining both storage and access data for all comics.

```graphql
query GetCombinedMetrics {
  combinedMetrics {
    comicMetrics {
      comicId
      comicName
      storageBytes
      imageCount
      averageImageSize
      storageByYear
      accessCount
      lastAccess
      averageAccessTime
      hitRatio
    }
  }
}
```

**Response:**
```json
{
  "data": {
    "combinedMetrics": {
      "comicMetrics": [
        {
          "comicId": 1,
          "comicName": "Dilbert",
          "storageBytes": 40000000,
          "imageCount": 1500,
          "averageImageSize": 26666,
          "storageByYear": {
            "1989": 5000000,
            "1990": 8000000,
            "2023": 2000000
          },
          "accessCount": 2500,
          "lastAccess": "2023-05-01T09:45:12Z",
          "averageAccessTime": 45.7,
          "hitRatio": 0.98
        }
      ]
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
    totalComics
    totalImages
    totalStorageBytes
    averageImageSize
  }
}
```

**Response:**
```json
{
  "data": {
    "refreshStorageMetrics": {
      "totalComics": 10,
      "totalImages": 5050,
      "totalStorageBytes": 126000000,
      "averageImageSize": 24950
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

| Field             | Type                    | Description                          |
|-------------------|-------------------------|--------------------------------------|
| totalComics       | Int!                    | Number of comics in the cache        |
| totalImages       | Int!                    | Total number of comic images stored  |
| totalStorageBytes | Float!                  | Total storage used (in bytes)        |
| averageImageSize  | Float!                  | Average size per image (in bytes)    |
| comicMetrics      | [ComicStorageMetric!]!  | Per-comic storage metrics            |

### ComicStorageMetric

| Field            | Type    | Description                                      |
|------------------|---------|--------------------------------------------------|
| comicId          | Int!    | Numeric identifier for the comic                 |
| imageCount       | Int!    | Number of images for this comic                  |
| storageBytes     | Float!  | Total storage used for this comic (in bytes)     |
| oldestImage      | Date    | Date of the oldest image (yyyy-MM-dd)            |
| newestImage      | Date    | Date of the newest image (yyyy-MM-dd)            |
| averageImageSize | Float!  | Average size per image for this comic (in bytes) |
| storageByYear    | JSON    | Map of storage usage broken down by year         |

### AccessMetrics

| Field         | Type                    | Description               |
|---------------|-------------------------|---------------------------|
| comicMetrics  | [ComicAccessMetric!]!   | Per-comic access metrics  |

### ComicAccessMetric

| Field             | Type      | Description                                      |
|-------------------|-----------|--------------------------------------------------|
| comicId           | Int!      | Numeric identifier for the comic                 |
| comicName         | String!   | Name of the comic                                |
| accessCount       | Int!      | Total number of times images were accessed       |
| lastAccess        | DateTime  | Timestamp of the most recent access              |
| averageAccessTime | Float     | Average time in milliseconds to access images    |
| hitRatio          | Float     | Ratio of cache hits to total access attempts     |

### CombinedMetrics

| Field         | Type                     | Description                            |
|---------------|--------------------------|----------------------------------------|
| comicMetrics  | [CombinedComicMetric!]!  | Combined storage and access metrics    |

### CombinedComicMetric

Combines all fields from both `ComicStorageMetric` and `ComicAccessMetric`.

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
