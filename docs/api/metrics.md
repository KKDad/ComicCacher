# Metrics API

## Queries

### storageMetrics

Get storage metrics for the comic cache.

```graphql
query {
  storageMetrics: StorageMetrics
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

**Returns:** `StorageMetrics`

```graphql
query {
  storageMetrics {
    totalBytes
    comicCount
    lastUpdated
    comics {
      comicId
      comicName
      totalBytes
      imageCount
      yearlyBreakdown {
        year
        bytes
        imageCount
      }
    }
  }
}
```

---

### accessMetrics

Get access metrics for all comics.

```graphql
query {
  accessMetrics: AccessMetrics
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

**Returns:** `AccessMetrics`

```graphql
query {
  accessMetrics {
    totalAccesses
    lastUpdated
    comics {
      comicName
      accessCount
      averageAccessTimeMs
      lastAccessed
    }
  }
}
```

---

### combinedMetrics

Get combined storage and access metrics.

```graphql
query {
  combinedMetrics: CombinedMetrics
}
```

**Auth:** `@hasRole(role: "OPERATOR")`

**Returns:** `CombinedMetrics`

```graphql
query {
  combinedMetrics {
    lastUpdated
    storage {
      totalBytes
      comicCount
    }
    access {
      totalAccesses
    }
  }
}
```

---

## Mutations

### refreshStorageMetrics

Force a refresh of storage metrics.

```graphql
mutation {
  refreshStorageMetrics: RefreshStorageMetricsPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

**Returns:** `RefreshStorageMetricsPayload!` -- `{ storageMetrics: StorageMetrics, errors: [UserError!]! }`

```graphql
mutation {
  refreshStorageMetrics {
    storageMetrics {
      totalBytes
      comicCount
      lastUpdated
    }
    errors {
      message
      code
    }
  }
}
```

---

### refreshAllMetrics

Force a refresh of all metrics (storage, access, combined).

```graphql
mutation {
  refreshAllMetrics: RefreshAllMetricsPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

**Returns:** `RefreshAllMetricsPayload!` -- `{ success: Boolean!, errors: [UserError!]! }`

```graphql
mutation {
  refreshAllMetrics {
    success
    errors {
      message
      code
    }
  }
}
```

---

## Types

### StorageMetrics

| Field | Type | Description |
|---|---|---|
| `totalBytes` | `Float` | Total storage used in bytes |
| `comicCount` | `Int` | Total number of comics being tracked |
| `comics` | `[ComicStorageMetric!]` | Per-comic storage breakdown |
| `lastUpdated` | `DateTime` | Last time metrics were calculated |

### ComicStorageMetric

| Field | Type | Description |
|---|---|---|
| `comicId` | `Int` | Comic ID |
| `comicName` | `String!` | Comic name |
| `totalBytes` | `Float!` | Total storage used by this comic in bytes |
| `imageCount` | `Int!` | Number of cached images |
| `yearlyBreakdown` | `[YearlyStorageMetric!]` | Yearly breakdown |

### YearlyStorageMetric

| Field | Type | Description |
|---|---|---|
| `year` | `Int!` | Year |
| `bytes` | `Float!` | Storage used in bytes for this year |
| `imageCount` | `Int!` | Number of images for this year |

### AccessMetrics

| Field | Type | Description |
|---|---|---|
| `totalAccesses` | `Int` | Total access events tracked |
| `comics` | `[ComicAccessMetric!]` | Per-comic access breakdown |
| `lastUpdated` | `DateTime` | Last time metrics were updated |

### ComicAccessMetric

| Field | Type | Description |
|---|---|---|
| `comicName` | `String!` | Comic name |
| `accessCount` | `Int!` | Total accesses for this comic |
| `averageAccessTimeMs` | `Float` | Average access time in ms |
| `lastAccessed` | `DateTime` | Last access timestamp |

### CombinedMetrics

| Field | Type | Description |
|---|---|---|
| `storage` | `StorageMetrics` | Storage metrics |
| `access` | `AccessMetrics` | Access metrics |
| `lastUpdated` | `DateTime` | Last time combined metrics were calculated |
