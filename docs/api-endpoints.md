# ComicCacher API Overview

This document provides a comprehensive overview of the ComicCacher API.

## API Architecture

> [!IMPORTANT]
> **GraphQL-First API**: ComicCacher uses GraphQL for all data operations.
> REST endpoints are only used for binary data (images, avatars).

---

## GraphQL Endpoint

**Primary Endpoint:** `POST /graphql`

All comic metadata, user management, preferences, metrics, batch jobs, and health operations use the GraphQL endpoint.

### GraphiQL Interface

When the application is running, an interactive GraphQL explorer is available at:

```
http://localhost:8087/graphiql
```

This interface allows you to explore the schema, test queries, and view documentation.

---

## API Categories

The ComicCacher API is organized into the following categories:

| Category           | Type     | Description                                                |
|--------------------|----------|-----------------------------------------------------------|
| Comics             | GraphQL  | Comic strips and metadata (queries, mutations)            |
| Authentication     | GraphQL  | User registration, login, token management (mutations)    |
| User Management    | GraphQL  | Profile and account management (queries, mutations)       |
| Preferences        | GraphQL  | User favorites and display settings (queries, mutations)  |
| Metrics            | GraphQL  | Storage and access statistics (queries, mutations)        |
| Retrieval Status   | GraphQL  | Comic retrieval monitoring and status records (queries, mutations) |
| Batch Jobs         | GraphQL  | Trigger and monitor comic update jobs (queries, mutations) |
| Health             | GraphQL  | System health and status information (query)              |
| Binary Assets      | REST     | Comic avatars and images (HTTP GET)                       |

---

## Documentation

For detailed information about each API category, refer to the specific documentation files:

- **[Comics API](comics-endpoints.md)**: Comic strips, metadata, and comic management
- **[Authentication API](auth-endpoints.md)**: Register, login, token refresh, password reset
- **[User Management API](user-endpoints.md)**: Profile management and account operations
- **[Preferences API](preferences-endpoints.md)**: Favorites, reading history, display settings
- **[Metrics API](metrics-endpoints.md)**: Storage metrics, access metrics, combined metrics
- **[Retrieval Status API](retrieval-status-endpoints.md)**: Monitor comic retrieval operations
- **[Batch Jobs API](update-endpoints.md)**: Trigger and monitor comic update jobs
- **[Health API](health-endpoint.md)**: System health and status checks
- **[GraphQL Scalars](graphql-scalars.md)**: Custom scalar types (Date, DateTime, JSON)

---

## Authentication

Most GraphQL operations require authentication using JWT tokens. Include the token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Public Operations (No Auth Required)

The following operations do not require authentication:
- `register` mutation - Create new user account
- `login` mutation - Authenticate and receive tokens
- `forgotPassword` mutation - Request password reset email
- `resetPassword` mutation - Reset password with token
- `health` query - Check system status

### Obtaining Tokens

Use the `register` or `login` mutations to obtain authentication tokens:

```graphql
mutation Login($input: LoginInput!) {
  login(input: $input) {
    token
    refreshToken
    username
    displayName
  }
}
```

---

## GraphQL Features

### Queries

Retrieve data from the API:

```graphql
query {
  comics(first: 10) {
    edges {
      node {
        id
        name
        description
      }
    }
  }
}
```

### Mutations

Modify data in the API:

```graphql
mutation AddFavorite($comicId: ID!) {
  addFavorite(comicId: $comicId) {
    favoriteComics
  }
}
```

### Cursor Pagination

GraphQL queries use **Relay-style cursor pagination**:

```graphql
query {
  comics(first: 10, after: "cursor_value") {
    edges {
      cursor
      node { id, name }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
    totalCount
  }
}
```

| Parameter | Type   | Description                        | Default |
|-----------|--------|------------------------------------|---------|
| `first`   | Int    | Number of items to fetch           | 20      |
| `after`   | String | Cursor to fetch items after        | -       |

---

## REST Endpoints (Binary Data Only)

### Comic Avatar

Retrieve the avatar image for a comic (binary data).

**Endpoint:** `GET /api/v1/comics/{comicId}/avatar`

**Response:** Returns image binary data with appropriate `Content-Type` header.

**Example:**
```bash
curl -X GET "http://localhost:8087/api/v1/comics/42/avatar" \
  -H "Authorization: Bearer <token>" \
  --output avatar.png
```

---

## Custom Scalars

The GraphQL API uses these custom scalar types:

| Scalar   | Format                | Example                    |
|----------|----------------------|----------------------------|
| `Date`   | ISO-8601 date        | `"2024-01-15"`             |
| `DateTime` | ISO-8601 datetime  | `"2024-01-15T10:30:00Z"`   |
| `JSON`   | Arbitrary JSON       | `{"key": "value"}`         |

See [GraphQL Scalars](graphql-scalars.md) for complete documentation.

---

## Error Handling

GraphQL errors are returned in the standard format:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Comic not found",
      "path": ["comic"],
      "extensions": {
        "classification": "NOT_FOUND"
      }
    }
  ]
}
```

### Common Error Classifications

| Classification   | HTTP Equivalent | Description                                |
|------------------|-----------------|-------------------------------------------|
| BAD_REQUEST      | 400             | Invalid parameters or request format      |
| UNAUTHORIZED     | 401             | Authentication required                   |
| FORBIDDEN        | 403             | Insufficient permissions                  |
| NOT_FOUND        | 404             | Resource not found                        |
| CONFLICT         | 409             | Resource conflict (e.g., duplicate username) |
| INTERNAL_ERROR   | 500             | Unexpected server error                   |

---

## Rate Limiting

API requests may be subject to rate limiting to ensure system stability.

**Current Limits:**
- 100 requests per minute for authenticated users
- 10 requests per minute for unauthenticated users

Rate limit information is included in response headers:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1620000000
```

---

## CORS

Cross-Origin Resource Sharing (CORS) is enabled for the GraphQL endpoint to allow browser-based clients.

---

## API Versioning

The GraphQL API uses schema evolution instead of versioning. The schema is designed to be backward-compatible, with deprecated fields marked in the schema documentation.

REST endpoints (binary data) use URL-based versioning:
- Current version: `/api/v1/`
- Future versions: `/api/v2/` (if needed)

---

## Development Tools

### GraphiQL

Interactive GraphQL explorer available at `/graphiql` when running locally.

### Schema Introspection

Query the GraphQL schema directly:

```graphql
query {
  __schema {
    types {
      name
      fields {
        name
        type {
          name
        }
      }
    }
  }
}
```

Note: Introspection may be limited in production for security reasons.

---

## Migration from REST

> **Historical Note**: Earlier versions of ComicCacher used REST endpoints for all operations. The API has been migrated to GraphQL for improved flexibility, type safety, and client efficiency.

If you're updating from an older client:
- Replace REST calls with GraphQL queries/mutations
- Update authentication to use GraphQL mutations (`login`, `register`)
- Update pagination from offset-based to cursor-based
- Update error handling to parse GraphQL error format

---

## Getting Started

1. **Explore the Schema**: Visit `/graphiql` to explore available queries and mutations
2. **Authenticate**: Use the `login` or `register` mutation to obtain tokens
3. **Query Data**: Use GraphQL queries to fetch comics, preferences, and metrics
4. **Modify Data**: Use GraphQL mutations to update preferences, trigger jobs, etc.
5. **Handle Errors**: Parse GraphQL error responses for detailed error information

For specific operation examples, see the detailed documentation for each API category linked above.
