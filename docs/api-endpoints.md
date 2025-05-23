# ComicCacher API Endpoints

This document provides a comprehensive overview of all available endpoints in the ComicCacher API.

## Base URL

All API endpoints are accessible via the base URL:

```
http://localhost:8080/api/v1
```

When deployed, replace `localhost:8080` with your server's address.

## Available Endpoint Groups

The ComicCacher API is organized into the following endpoint groups:

| Endpoint Group     | Base Path                | Description                                                |
|--------------------|--------------------------|-----------------------------------------------------------|
| Comics             | /api/v1/comics           | Access and manage comic strips and metadata               |
| Authentication     | /api/v1/auth             | User registration, authentication, and token management   |
| Health             | /api/v1/health           | System health and status information                      |
| Metrics            | /api/v1/metrics          | Storage and access statistics for comics                  |
| Preferences        | /api/v1/preferences      | User preferences for comics and display settings          |
| Retrieval Status   | /api/v1/retrieval-status | Comic retrieval operation monitoring and status records   |
| Updates            | /api/v1/update           | Trigger comic updates and downloads                       |
| Users              | /api/v1/users            | User profile and account management                       |

## Detailed Documentation

For detailed information about each endpoint group, refer to the specific documentation files:

- [Comics Endpoints](comics-endpoints.md): Retrieve comics, comic strips, and manage comic data
- [Authentication Endpoints](auth-endpoints.md): Register, login, and manage authentication tokens
- [Health Endpoint](health-endpoint.md): Check system health and status
- [Metrics Endpoints](metrics-endpoints.md): View storage and access statistics
- [Preferences Endpoints](preferences-endpoints.md): Manage user preferences
- [Retrieval Status Endpoints](retrieval-status-endpoints.md): Monitor comic retrieval operations and status records
- [Update Endpoints](update-endpoints.md): Trigger comic updates
- [User Endpoints](user-endpoints.md): Manage user profiles and settings

## Authentication

Most endpoints require authentication using JWT tokens. After logging in or registering, you will receive a token that must be included in the Authorization header for subsequent requests:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Public endpoints that do not require authentication include:
- `/api/v1/auth/register`
- `/api/v1/auth/login`
- `/api/v1/health`

## Response Format

All API endpoints return responses in a standardized format:

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Success",
  "data": {
    // Response data specific to the endpoint
  }
}
```

### Common Status Codes

| Status Code | Description                                           |
|-------------|-------------------------------------------------------|
| 200         | Success - Request was processed successfully          |
| 201         | Created - Resource was successfully created           |
| 400         | Bad Request - Invalid parameters or request format    |
| 401         | Unauthorized - Authentication required                |
| 403         | Forbidden - Insufficient permissions                  |
| 404         | Not Found - Resource not found                        |
| 500         | Internal Server Error - Unexpected server error       |

## Rate Limiting

API endpoints may be subject to rate limiting to ensure system stability. The current rate limits are:

- 100 requests per minute for authenticated users
- 10 requests per minute for unauthenticated users

Rate limit headers are included in all responses:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1620000000
```

## Pagination

Endpoints that return collections support pagination using the following query parameters:

| Parameter | Description                              | Default | Max    |
|-----------|------------------------------------------|---------|--------|
| page      | Page number (zero-based)                 | 0       | -      |
| size      | Number of items per page                 | 20      | 100    |
| sort      | Sort field and direction (field,direction)| -       | -      |

Example request with pagination:

```
GET /api/v1/comics?page=0&size=10&sort=name,asc
```

## API Versioning

The API uses URL-based versioning, with the current version being v1. Future versions may be introduced with new paths (e.g., `/api/v2/...`).

## API Explorer

When the application is running, an interactive OpenAPI documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

This interface allows you to explore and test all available endpoints.