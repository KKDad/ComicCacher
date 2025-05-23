# Retrieval Status Endpoints Documentation

The ComicCacher API provides endpoints for monitoring comic retrieval operations and their status records.

## Base Path

All retrieval status endpoints are under:

```
/api/v1/retrieval-status
```

## Authentication

All retrieval status endpoints require authentication. Include a valid JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Endpoints

### Get Retrieval Records

```
GET /api/v1/retrieval-status
```

Returns retrieval records from the last 7 days with optional filters. If date parameters are not provided, all available records are returned.

#### Query Parameters

| Parameter | Type                     | Required | Default | Description                              |
|-----------|--------------------------|----------|---------|------------------------------------------|
| comicName | String                   | No       | -       | Filter by comic name                     |
| status    | ComicRetrievalStatus     | No       | -       | Filter by status                         |
| fromDate  | String (yyyy-MM-dd)      | No       | -       | Filter by from date (inclusive, within the 7-day window) |
| toDate    | String (yyyy-MM-dd)      | No       | -       | Filter by to date (inclusive, within the 7-day window) |
| limit     | Integer                  | No       | 100     | Maximum number of records to return      |

#### Retrieval Status Values

- SUCCESS
- NETWORK_ERROR
- PARSING_ERROR
- COMIC_UNAVAILABLE
- AUTHENTICATION_ERROR
- STORAGE_ERROR
- UNKNOWN_ERROR

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Retrieved status records successfully",
  "data": [
    {
      "id": "rec-123",
      "comicName": "Dilbert",
      "comicDate": "2023-05-01",
      "source": "gocomics",
      "status": "SUCCESS",
      "errorMessage": null,
      "retrievalDurationMs": 1500,
      "imageSize": 25000,
      "httpStatusCode": 200
    }
  ]
}
```

### Get Specific Retrieval Record

```
GET /api/v1/retrieval-status/{recordId}
```

Get a specific retrieval record by ID.

#### Path Parameters

| Parameter | Type   | Required | Description |
|-----------|--------|----------|-------------|
| recordId  | String | Yes      | Record ID   |

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Retrieved status record successfully",
  "data": {
    "id": "rec-123",
    "comicName": "Dilbert",
    "comicDate": "2023-05-01",
    "source": "gocomics",
    "status": "SUCCESS",
    "errorMessage": null,
    "retrievalDurationMs": 1500,
    "imageSize": 25000,
    "httpStatusCode": 200
  }
}
```

### Get Retrieval Summary

```
GET /api/v1/retrieval-status/summary
```

Returns aggregated statistics for all retrieval operations. Date parameters are optional and will further filter results within the available 7-day window.

#### Query Parameters

| Parameter | Type                | Required | Description                     |
|-----------|---------------------|----------|---------------------------------|
| fromDate  | String (yyyy-MM-dd) | No       | Filter by from date (inclusive, within the 7-day window) |
| toDate    | String (yyyy-MM-dd) | No       | Filter by to date (inclusive, within the 7-day window) |

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Retrieved status summary successfully",
  "data": {
    "totalRecords": 1000,
    "successfulRetrievals": 950,
    "failedRetrievals": 50,
    "successRate": 0.95,
    "avgRetrievalTime": 1200,
    "totalDataTransferred": 125000000
  }
}
```

### Get Retrieval Records for Comic

```
GET /api/v1/retrieval-status/comics/{comicName}
```

Get retrieval records for a specific comic.

#### Path Parameters

| Parameter | Type   | Required | Description |
|-----------|--------|----------|-------------|
| comicName | String | Yes      | Comic name  |

#### Query Parameters

| Parameter | Type    | Required | Default | Description                              |
|-----------|---------|----------|---------|------------------------------------------|
| limit     | Integer | No       | 20      | Maximum number of records to return      |

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Retrieved status records for comic successfully",
  "data": [
    {
      "id": "rec-123",
      "comicName": "Dilbert",
      "comicDate": "2023-05-01",
      "source": "gocomics",
      "status": "SUCCESS",
      "errorMessage": null,
      "retrievalDurationMs": 1500,
      "imageSize": 25000,
      "httpStatusCode": 200
    }
  ]
}
```

### Delete Retrieval Record

```
DELETE /api/v1/retrieval-status/{recordId}
```

Delete a specific retrieval record.

#### Path Parameters

| Parameter | Type   | Required | Description |
|-----------|--------|----------|-------------|
| recordId  | String | Yes      | Record ID   |

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Retrieval record deleted",
  "data": "Record deleted successfully"
}
```

### Purge Old Records

```
DELETE /api/v1/retrieval-status
```

Purge all retrieval records older than specified days.

#### Query Parameters

| Parameter   | Type    | Required | Default | Description                    |
|-------------|---------|----------|---------|--------------------------------|
| daysToKeep  | Integer | No       | 7       | Days to keep (default is 7)    |

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Old retrieval records purged successfully",
  "data": "Purged 150 records older than 7 days"
}
```

## Response Fields

### Retrieval Record Fields

| Field                | Description                                           |
|----------------------|-------------------------------------------------------|
| id                   | Unique identifier for the retrieval record           |
| comicName            | Name of the comic                                     |
| comicDate            | Date of the comic strip                               |
| source               | Source of the comic (e.g., "gocomics")               |
| status               | Status of the retrieval operation                     |
| errorMessage         | Error message if the retrieval failed (null if successful) |
| retrievalDurationMs  | Duration of the retrieval operation in milliseconds   |
| imageSize            | Size of the retrieved image in bytes                  |
| httpStatusCode       | HTTP status code returned by the source              |

## Error Responses

### Unauthorized (401)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 401,
  "message": "Authentication required",
  "data": null
}
```

### Record Not Found (404)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 404,
  "message": "Retrieval record not found with ID: recordId",
  "data": null
}
```

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

1. **Monitoring**: Track the success and failure rates of comic downloads
2. **Troubleshooting**: Identify comics that are failing to download
3. **Performance Analysis**: Monitor download times and identify bottlenecks
4. **Maintenance**: Clean up old retrieval records to manage storage
5. **Reporting**: Generate reports on comic retrieval operations