# Health Endpoint Documentation

The ComicCacher API includes a health endpoint that provides detailed information about the application's status, system resources, and component health.

## Endpoint

```
GET /api/v1/health
```

This endpoint is accessible without authentication.

## Query Parameters

| Parameter | Type    | Required | Default | Description                                 |
|-----------|---------|----------|---------|---------------------------------------------|
| detailed  | boolean | No       | false   | Whether to include detailed metrics         |

## Response Format

The health endpoint returns a standard API response with the health status data.

### Basic Health Response (detailed=false)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Success",
  "data": {
    "status": "UP",
    "timestamp": "2023-05-01T10:15:30",
    "uptime": 3600000,
    "buildInfo": {
      "name": "ComicAPI",
      "artifact": "comic-api",
      "group": "org.stapledon",
      "version": "1.0.0",
      "buildTime": "2023-05-01T10:15:30Z",
      "javaVersion": "17.0.6"
    }
  }
}
```

### Detailed Health Response (detailed=true)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Success",
  "data": {
    "status": "UP",
    "timestamp": "2023-05-01T10:15:30",
    "uptime": 3600000,
    "buildInfo": {
      "name": "ComicAPI",
      "artifact": "comic-api",
      "group": "org.stapledon",
      "version": "1.0.0",
      "buildTime": "2023-05-01T10:15:30Z",
      "javaVersion": "17.0.6"
    },
    "systemResources": {
      "availableProcessors": 4,
      "memory": {
        "totalMemory": 1024,
        "freeMemory": 512,
        "maxMemory": 2048,
        "usedPercentage": 50.0
      },
      "diskSpace": {
        "total": 10240,
        "free": 5120,
        "usable": 5120,
        "usedPercentage": 50.0
      }
    },
    "cacheStatus": {
      "totalComics": 5,
      "totalImages": 100,
      "totalStorageBytes": 10485760,
      "oldestImage": "/path/to/oldest.png",
      "newestImage": "/path/to/newest.png",
      "cacheLocation": "/var/comics/cache"
    },
    "components": {
      "cache": {
        "status": "UP",
        "details": {
          "accessible": true,
          "sufficientSpace": true,
          "path": "/var/comics/cache"
        },
        "message": "Cache is functioning normally"
      }
    }
  }
}
```

## Status Values

The health endpoint reports the overall status of the application with one of the following values:

- **UP**: All systems are functioning normally
- **DEGRADED**: The system is functioning but with reduced capabilities or performance
- **DOWN**: Critical components are not functioning, and the application is not operational

## Use Cases

1. **Monitoring**: Use the health endpoint in monitoring systems to track application availability
2. **Diagnostics**: The detailed view provides diagnostic information for troubleshooting
3. **System Status**: Use as a quick check to verify all components are functioning correctly
4. **Resource Planning**: Monitor resource usage trends to plan for capacity needs

## Response Fields

### Build Info

| Field       | Description                      |
|-------------|----------------------------------|
| name        | Application name                 |
| artifact    | Build artifact identifier        |
| group       | Grouping identifier              |
| version     | Application version             |
| buildTime   | When the application was built   |
| javaVersion | Java runtime version             |

### System Resources

| Field              | Description                              |
|--------------------|------------------------------------------|
| availableProcessors| Number of available CPU cores            |
| memory             | Memory usage information (in MB)         |
| diskSpace          | Disk space information (in MB)           |

### Cache Status

| Field            | Description                              |
|------------------|------------------------------------------|
| totalComics      | Number of comics in the cache            |
| totalImages      | Total number of cached images            |
| totalStorageBytes| Total storage used in bytes              |
| oldestImage      | Path to the oldest image in the cache    |
| newestImage      | Path to the newest image in the cache    |
| cacheLocation    | Directory where the cache is stored      |

### Components

The components section reports the health of individual application components. Each component includes:

| Field    | Description                                 |
|----------|---------------------------------------------|
| status   | Component status (UP, DEGRADED, DOWN)       |
| details  | Component-specific details                  |
| message  | Human-readable status message               |