# Health API Documentation

> [!IMPORTANT]
> **GraphQL-Only**: Health status information is available via GraphQL.
> There is no REST endpoint for health checks.

## GraphQL Endpoint

**Endpoint:** `POST /graphql`

The health query does not require authentication and is accessible to all users.

---

## Query

### Get Health Status

Retrieve the application's health status, system resources, and component health.

```graphql
query GetHealth {
  health {
    status
    timestamp
    uptime
    buildInfo {
      name
      artifact
      group
      version
      buildTime
      javaVersion
    }
    systemResources {
      availableProcessors
      memory {
        totalMemory
        freeMemory
        maxMemory
        usedPercentage
      }
      diskSpace {
        total
        free
        usable
        usedPercentage
      }
    }
    cacheStatus {
      totalComics
      totalImages
      totalStorageBytes
      oldestImage
      newestImage
      cacheLocation
    }
    components {
      name
      status
      details
      message
    }
  }
}
```

**Response:**
```json
{
  "data": {
    "health": {
      "status": "UP",
      "timestamp": "2023-05-01T10:15:30Z",
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
      "components": [
        {
          "name": "cache",
          "status": "UP",
          "details": {
            "accessible": true,
            "sufficientSpace": true,
            "path": "/var/comics/cache"
          },
          "message": "Cache is functioning normally"
        }
      ]
    }
  }
}
```

---

## Types

### HealthStatus

| Field           | Type                       | Description                          |
|-----------------|----------------------------|--------------------------------------|
| status          | HealthStatusEnum!          | Overall application status           |
| timestamp       | DateTime!                  | Current timestamp                    |
| uptime          | Float!                     | Application uptime in milliseconds   |
| buildInfo       | BuildInfo!                 | Build and version information        |
| systemResources | SystemResources            | System resource usage (optional)     |
| cacheStatus     | CacheStatus                | Comic cache status (optional)        |
| components      | [ComponentHealthEntry!]!   | Individual component health checks   |

### HealthStatusEnum

| Value    | Description                                          |
|----------|------------------------------------------------------|
| UP       | All systems are functioning normally                 |
| DEGRADED | System is functioning but with reduced capabilities  |
| DOWN     | Critical components are not functioning              |

### BuildInfo

| Field       | Type     | Description                  |
|-------------|----------|------------------------------|
| name        | String!  | Application name             |
| artifact    | String!  | Build artifact identifier    |
| group       | String!  | Grouping identifier          |
| version     | String!  | Application version          |
| buildTime   | String!  | When the application was built |
| javaVersion | String!  | Java runtime version         |

### SystemResources

| Field               | Type          | Description                          |
|---------------------|---------------|--------------------------------------|
| availableProcessors | Int!          | Number of available CPU cores        |
| memory              | MemoryInfo!   | Memory usage information             |
| diskSpace           | DiskSpaceInfo!| Disk space information               |

### MemoryInfo

| Field          | Type   | Description                          |
|----------------|--------|--------------------------------------|
| totalMemory    | Float! | Total memory in MB                   |
| freeMemory     | Float! | Free memory in MB                    |
| maxMemory      | Float! | Maximum memory in MB                 |
| usedPercentage | Float! | Percentage of memory used (0-100)    |

### DiskSpaceInfo

| Field          | Type   | Description                          |
|----------------|--------|--------------------------------------|
| total          | Float! | Total disk space in MB               |
| free           | Float! | Free disk space in MB                |
| usable         | Float! | Usable disk space in MB              |
| usedPercentage | Float! | Percentage of disk used (0-100)      |

### CacheStatus

| Field             | Type    | Description                              |
|-------------------|---------|------------------------------------------|
| totalComics       | Int!    | Number of comics in the cache            |
| totalImages       | Int!    | Total number of cached images            |
| totalStorageBytes | Float!  | Total storage used in bytes              |
| oldestImage       | String  | Path to the oldest image in the cache    |
| newestImage       | String  | Path to the newest image in the cache    |
| cacheLocation     | String! | Directory where the cache is stored      |

### ComponentHealthEntry

| Field   | Type             | Description                         |
|---------|------------------|-------------------------------------|
| name    | String!          | Component name                      |
| status  | HealthStatusEnum!| Component status (UP, DEGRADED, DOWN) |
| details | JSON             | Component-specific details          |
| message | String           | Human-readable status message       |

---

## Use Cases

1. **Monitoring**: Use the health query in monitoring systems to track application availability
2. **Diagnostics**: The detailed view provides diagnostic information for troubleshooting
3. **System Status**: Quick check to verify all components are functioning correctly
4. **Resource Planning**: Monitor resource usage trends to plan for capacity needs
5. **Uptime Tracking**: Monitor application uptime and stability over time

---

## Notes

- The `health` query is publicly accessible and does not require authentication
- System resources and cache status are included by default in the query result
- Component health checks provide detailed status for individual application subsystems
- The `UP` status indicates all components are healthy
- `DEGRADED` status indicates reduced functionality but the system is still operational
- `DOWN` status indicates critical failures requiring immediate attention
