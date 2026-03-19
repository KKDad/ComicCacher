# API Overview

ComicCacher exposes a **GraphQL-first** API for all metadata operations, with REST endpoints reserved for binary image streaming.

## Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/graphql` | POST | GraphQL query/mutation endpoint |
| `/graphiql` | GET | Interactive GraphQL IDE (browser) |
| `/api/v1/comics/{id}/avatar` | GET | Binary avatar image |
| `/api/v1/comics/{id}/strip/{date}` | GET | Binary strip image |

All GraphQL requests are `POST /graphql` with a JSON body:

```json
{
  "query": "query { comics(first: 10) { edges { node { name } } } }",
  "variables": {}
}
```

## Authentication Model

Authentication uses JWT bearer tokens passed in the `Authorization` header:

```
Authorization: Bearer <token>
```

### Roles

| Role | Description |
|---|---|
| `USER` | Standard product access. Can browse comics, manage favorites, update own profile. |
| `OPERATOR` | Read-only operational access. Can view metrics, retrieval status, and batch job info. |
| `ADMIN` | Full control. Can manage comics, trigger jobs, purge records, delete accounts. |

### Schema Directives

The schema uses three directives to declare authorization requirements on each field:

| Directive | Meaning |
|---|---|
| `@public` | No authentication required. Accessible to anonymous requests. |
| `@authenticated` | Requires a valid JWT token (any role). |
| `@hasRole(role: "ROLE")` | Requires a valid JWT token with the specified role. |

## Custom Scalars

| Scalar | Format | Example |
|---|---|---|
| `Date` | ISO-8601 date (`YYYY-MM-DD`) | `"2026-03-19"` |
| `DateTime` | ISO-8601 with offset | `"2026-03-19T14:30:00-04:00"` |
| `JSON` | Arbitrary JSON object | `{"theme": "dark"}` |

## Relay Cursor Pagination

List queries use Relay-style cursor-based pagination. The `comics` query returns a `ComicConnection`:

```graphql
query {
  comics(first: 10, after: "cursor_abc") {
    edges {
      node {
        id
        name
      }
      cursor
    }
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
    totalCount
  }
}
```

**Parameters:**
- `first` -- Number of items to return (max 50, default 20).
- `after` -- Cursor from a previous response's `endCursor` to fetch the next page.

**PageInfo fields:**
- `hasNextPage` / `hasPreviousPage` -- Whether more results exist in either direction.
- `startCursor` / `endCursor` -- Cursors for the first and last edges in the current page.

## Mutation Payload Pattern

All mutations return a payload type containing the result object and an `errors` array:

```graphql
type CreateComicPayload {
  comic: Comic          # null if errors occurred
  errors: [UserError!]! # empty array on success
}

type UserError {
  message: String!       # Human-readable error message
  field: String          # Field path that caused the error (e.g., "input.email")
  code: ErrorCode        # Machine-readable error code
}
```

Clients should always check `errors` before reading the result field.

## Error Handling

### GraphQL Errors

Errors appear in two places:

1. **`errors` array in the GraphQL response** -- For transport/auth-level errors (UNAUTHENTICATED, FORBIDDEN).
2. **`errors` field inside mutation payloads** -- For domain-level validation errors (UserError objects).

### ErrorCode Enum

The `ErrorCode` enum provides machine-readable error codes:

| Code | Description |
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

You can query all error codes at runtime:

```graphql
query {
  errorCodes
}
```

## CORS

The API allows cross-origin requests with the following configuration (defined in `SecurityConfig`):

| Setting | Value |
|---|---|
| Allowed origins | `*` |
| Allowed methods | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| Allowed headers | `authorization`, `content-type`, `x-auth-token` |
| Exposed headers | `x-auth-token` |
