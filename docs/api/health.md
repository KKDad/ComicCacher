# Health API

## Queries

### health

Get the health status of the application.

```graphql
query {
  health(detailed: Boolean = false): HealthStatus!
}
```

**Auth:** `@public`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `detailed` | `Boolean` | `false` | When `true`, includes `systemResources` and `cacheStatus` in the response |

**Returns:** `HealthStatus!`

**Basic health check:**

```graphql
query {
  health {
    status
    timestamp
    uptime
    buildInfo {
      version
      buildTime
      gitCommit
      gitBranch
    }
    components {
      name
      status
      details
    }
  }
}
```

**Detailed health check:**

```graphql
query {
  health(detailed: true) {
    status
    timestamp
    uptime
    buildInfo {
      version
      buildTime
      gitCommit
      gitBranch
    }
    systemResources {
      availableProcessors
      totalMemory
      freeMemory
      maxMemory
      memoryUsagePercent
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
    }
  }
}
```

---

### errorCodes

Get a list of all known error codes. Useful for clients building error handling logic.

```graphql
query {
  errorCodes: [ErrorCode!]!
}
```

**Auth:** `@public`

**Returns:** `[ErrorCode!]!`

```graphql
query {
  errorCodes
}
```

---

## Types

### HealthStatus

| Field | Type | Description |
|---|---|---|
| `status` | `HealthStatusEnum!` | Current health status |
| `timestamp` | `DateTime` | When the health check was performed |
| `uptime` | `Float` | Application uptime in milliseconds |
| `buildInfo` | `BuildInfo` | Application build information |
| `systemResources` | `SystemResources` | System resource metrics (only when `detailed=true`) |
| `cacheStatus` | `CacheStatus` | Cache status information (only when `detailed=true`) |
| `components` | `[ComponentHealthEntry!]` | Individual component health statuses |

### BuildInfo

| Field | Type | Description |
|---|---|---|
| `version` | `String` | Application version |
| `buildTime` | `String` | Build timestamp |
| `gitCommit` | `String` | Git commit hash |
| `gitBranch` | `String` | Git branch |

### SystemResources

| Field | Type | Description |
|---|---|---|
| `availableProcessors` | `Int` | Number of available CPU processors |
| `totalMemory` | `Float` | Total memory in bytes |
| `freeMemory` | `Float` | Free memory in bytes |
| `maxMemory` | `Float` | Maximum JVM memory in bytes |
| `memoryUsagePercent` | `Float` | Memory usage percentage (0-100) |

### CacheStatus

| Field | Type | Description |
|---|---|---|
| `totalComics` | `Int!` | Total number of comics cached |
| `totalImages` | `Int!` | Total number of cached images |
| `totalStorageBytes` | `Float` | Total storage used in bytes |
| `oldestImage` | `String` | Oldest image in the cache |
| `newestImage` | `String` | Newest image in the cache |
| `cacheLocation` | `String` | Directory where the cache is stored |

### ComponentHealthEntry

| Field | Type | Description |
|---|---|---|
| `name` | `String!` | Component name |
| `status` | `HealthStatusEnum!` | Component health status |
| `details` | `String` | Additional details |

### HealthStatusEnum

| Value | Description |
|---|---|
| `UP` | Healthy and operational |
| `DEGRADED` | Operational but with issues |
| `DOWN` | Not operational |

### ErrorCode Enum

| Value | Description |
|---|---|
| `UNAUTHENTICATED` | Authentication required but not provided |
| `FORBIDDEN` | User does not have permission |
| `NOT_FOUND` | Requested resource not found |
| `VALIDATION_ERROR` | Input validation failed |
| `COMIC_NOT_FOUND` | Comic with specified ID does not exist |
| `STRIP_NOT_FOUND` | Strip not available for requested date |
| `USER_NOT_FOUND` | User account not found |
| `USER_ALREADY_EXISTS` | Username or email already exists |
| `INVALID_CREDENTIALS` | Invalid credentials provided |
| `TOKEN_EXPIRED` | Token has expired |
| `INVALID_TOKEN` | Token is invalid or malformed |
| `INVALID_PASSWORD` | Password does not meet requirements |
| `RATE_LIMITED` | Rate limit exceeded |
| `INTERNAL_ERROR` | Internal server error |
