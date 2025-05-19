# Metrics Endpoints Documentation

The ComicCacher API includes several metrics endpoints that provide detailed information about comic storage usage, access patterns, and combined metrics.

## Base Path

All metrics endpoints are under:

```
/api/v1/metrics
```

## Endpoints

### Storage Metrics

```
GET /api/v1/metrics/storage
```

Provides storage metrics for all cached comics, including data about disk usage and image counts.

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Success",
  "data": {
    "totalComics": 10,
    "totalImages": 5000,
    "totalStorageBytes": 125000000,
    "averageImageSize": 25000,
    "comicMetrics": {
      "1": {
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
      },
      "2": {
        "comicId": 2,
        "imageCount": 3500,
        "storageBytes": 85000000,
        "oldestImage": "1985-11-18",
        "newestImage": "1995-12-31",
        "averageImageSize": 24285,
        "storageByYear": {
          "1985": 3000000,
          "1986": 12000000,
          "1995": 9000000
        }
      }
    }
  }
}
```

### Access Metrics

```
GET /api/v1/metrics/access
```

Provides metrics about how comics are being accessed, including frequency and patterns.

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Success",
  "data": {
    "1": {
      "comicName": "Dilbert",
      "accessCount": 2500,
      "lastAccess": "2023-05-01T09:45:12",
      "averageAccessTime": 45.7,
      "hitRatio": 0.98
    },
    "2": {
      "comicName": "Calvin and Hobbes",
      "accessCount": 5200,
      "lastAccess": "2023-05-01T10:12:33",
      "averageAccessTime": 38.2,
      "hitRatio": 0.99
    }
  }
}
```

### Combined Metrics

```
GET /api/v1/metrics/combined
```

Provides a comprehensive view combining both storage and access metrics for all comics.

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Success",
  "data": {
    "1": {
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
      "lastAccess": "2023-05-01T09:45:12",
      "averageAccessTime": 45.7,
      "hitRatio": 0.98
    },
    "2": {
      "comicName": "Calvin and Hobbes",
      "storageBytes": 85000000,
      "imageCount": 3500,
      "averageImageSize": 24285,
      "storageByYear": {
        "1985": 3000000,
        "1986": 12000000,
        "1995": 9000000
      },
      "accessCount": 5200,
      "lastAccess": "2023-05-01T10:12:33",
      "averageAccessTime": 38.2,
      "hitRatio": 0.99
    }
  }
}
```

### Refresh Storage Metrics

```
GET /api/v1/metrics/storage/refresh
```

Forces an update of the storage metrics, useful after large changes to the cache.

#### Response Format

Returns the same format as the GET /api/v1/metrics/storage endpoint, but with freshly calculated statistics.

## Response Fields

### Storage Metrics Fields

| Field            | Description                                            |
|------------------|--------------------------------------------------------|
| totalComics      | Number of comics in the cache                          |
| totalImages      | Total number of comic images stored                    |
| totalStorageBytes| Total storage used (in bytes)                          |
| averageImageSize | Average size per image (in bytes)                      |
| comicMetrics     | Map of per-comic storage metrics (key is comic ID)     |

### Comic Storage Metrics Fields

| Field            | Description                                      |
|------------------|--------------------------------------------------|
| comicId          | Numeric identifier for the comic                 |
| imageCount       | Number of images for this comic                  |
| storageBytes     | Total storage used for this comic (in bytes)     |
| oldestImage      | Date of the oldest image (yyyy-MM-dd)            |
| newestImage      | Date of the newest image (yyyy-MM-dd)            |
| averageImageSize | Average size per image for this comic (in bytes) |
| storageByYear    | Map of storage usage broken down by year         |

### Access Metrics Fields

| Field             | Description                                      |
|-------------------|--------------------------------------------------|
| comicName         | Name of the comic                                |
| accessCount       | Total number of times images were accessed       |
| lastAccess        | Timestamp of the most recent access              |
| averageAccessTime | Average time in milliseconds to access images    |
| hitRatio          | Ratio of cache hits to total access attempts     |

### Combined Metrics Fields

| Field             | Description                                          |
|-------------------|------------------------------------------------------|
| comicName         | Name of the comic                                    |
| storageBytes      | Total storage used for this comic (in bytes)         |
| imageCount        | Number of images for this comic                      |
| averageImageSize  | Average size per image for this comic (in bytes)     |
| storageByYear     | Map of storage usage broken down by year             |
| accessCount       | Total number of times images were accessed           |
| lastAccess        | Timestamp of the most recent access                  |
| averageAccessTime | Average time in milliseconds to access images        |
| hitRatio          | Ratio of cache hits to total access attempts         |

## Error Responses

### Internal Server Error (500)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 500,
  "message": "An unexpected error occurred",
  "data": null
}
```

## Use Cases

1. **Storage Management**: Monitor comic cache size and growth over time
2. **Popularity Analysis**: Determine which comics are most frequently accessed
3. **Optimization**: Identify opportunities to optimize storage based on access patterns
4. **Capacity Planning**: Plan for storage needs based on comic download patterns
5. **Troubleshooting**: Diagnose issues related to comic caching and storage