# Update Endpoints Documentation

The ComicCacher API provides endpoints for triggering comic updates to download the latest comic strips from their sources.

## Base Path

All update endpoints are under:

```
/api/v1
```

## Authentication

Update operations can be configured to require authentication. If enabled, include a valid JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Endpoints

### Update All Comics

```
GET /api/v1/update
```

Triggers an update for all configured comics, downloading the latest strips.

#### Response

The endpoint returns an HTTP 200 OK status code with no response body if the update was successful. 

If the update process fails, an HTTP 404 Not Found status code is returned.

### Update Specific Comic

```
GET /api/v1/update/{comicId}
```

Triggers an update for a specific comic, downloading the latest strip.

#### Path Parameters

| Parameter | Type    | Required | Description                           |
|-----------|---------|----------|---------------------------------------|
| comicId   | Integer | Yes      | The numeric ID of the comic to update |

#### Response

The endpoint returns an HTTP 200 OK status code with no response body if the update was successful.

If the comic is not found or the update process fails, an HTTP 404 Not Found status code is returned.

## Error Responses

### Comic Not Found (404)

If the comic ID provided does not exist, a 404 Not Found status code is returned with an empty response body.

### Unauthorized (401)

If authentication is enabled and the request does not include a valid token, a 401 Unauthorized status code is returned.

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 401,
  "message": "Authentication required",
  "data": null
}
```

### Internal Server Error (500)

If an unexpected error occurs during the update process, a 500 Internal Server Error status code is returned.

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 500,
  "message": "An unexpected error occurred",
  "data": null
}
```

## Background Update Process

In addition to these manual update endpoints, ComicCacher includes a daily scheduled update process that automatically downloads new comics. The schedule for this process can be configured in the application properties.

### Scheduled Updates Configuration

The following properties control the scheduled updates:

```properties
# Enable or disable scheduled updates
app.daily-runner.enabled=true

# Cron expression for the update schedule (default: 6:00 AM daily)
app.daily-runner.cron=0 0 6 * * ?

# Maximum number of comics to update in parallel
app.daily-runner.max-parallel-updates=3

# Whether to perform a catch-up for missed days
app.daily-runner.catch-up-enabled=true

# Maximum number of days to catch up
app.daily-runner.max-catch-up-days=7
```

## Use Cases

1. **Manual Updates**: Trigger updates on demand for specific comics
2. **Batch Updates**: Update all comics in a single operation
3. **Integration**: Allow external systems to trigger comic updates
4. **Maintenance**: Refresh the comics cache after configuration changes
5. **Troubleshooting**: Force an update when a specific comic fails to download automatically