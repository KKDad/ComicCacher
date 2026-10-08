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
| `OPERATOR` | Read-only operational access. Can view metrics, retrieval status, batch jobs and the Sources page. |
| `ADMIN` | Full control. Can manage comics, trigger jobs, purge records, delete accounts. |

The roles are ranked `ADMIN > OPERATOR > USER` (`RoleHierarchy` in `SecurityConfig`; `ROLE_RANK` in comic-hub's `lib/roles.ts`), so each role can do everything the ones below it can. A check for `OPERATOR` lets admins through too.

**OPERATOR is never assigned by the app.** New accounts get `["USER"]`, and no mutation or page changes a user's roles. Every `OPERATOR` check is passed in practice by an `ADMIN`. To make an operator, stop the API, edit the user's `roles` in `users.json` (see [Configuration Files](../storage/configuration-files.md#2-usersjson-user-accounts)), and start it again. The API holds the users in memory and writes them back on every save (each login updates `lastLogin`), so an edit made while it runs is lost. Roles are also copied into the JWT, so the user gets the new role at their next token refresh (within 15 minutes) or sign-in. The role is kept so that read-only access can be given out later without changing the checks.

### Schema Directives

The schema uses three directives to declare authorization requirements on each field. They are declarations only: nothing in the API reads them. The `@PreAuthorize` annotation on each resolver method enforces the rule, so keep the two in step when changing either.

| Directive | Meaning |
|---|---|
| `@public` | No authentication required. Accessible to anonymous requests. |
| `@authenticated` | Requires a valid JWT token (any role). |
| `@hasRole(role: "ROLE")` | Requires a valid JWT token with the specified role or a higher one. |

### Dev Tokens (dev instance only)

For testing against the dev instance, the `devToken` mutation issues tokens for an existing user without their password:

```graphql
mutation { devToken(secret: "<secret>", username: "admin") { token refreshToken } }
```

`username` is optional and defaults to `comics.dev-token.default-username`. The mutation and its schema file (`graphql-dev/dev-token.graphql`, outside the scanned schema locations) are only loaded when `comics.dev-token.enabled=true`, so it doesn't exist in production or in comic-hub codegen. Startup fails if `comics.dev-token.secret` is under 32 characters. Each issued token logs an `AUDIT` line. On the dev instance, `utils/dev/docker-compose.yml` sets these variables from `dev-token.env` in `/root/comics-deploy-dev` on the Docker host. `utils/remote/run.sh dev` (run by `utils/deploy.sh dev`) creates that file with a random secret on first run, and adds `COMICS_DEVTOKEN_DEFAULTUSERNAME=uireview0927` (a USER-role test account) if it's missing; pass `username` for another account, such as an admin.

| Property | Environment variable |
|---|---|
| `comics.dev-token.enabled` | `COMICS_DEVTOKEN_ENABLED` |
| `comics.dev-token.secret` | `COMICS_DEVTOKEN_SECRET` |
| `comics.dev-token.default-username` | `COMICS_DEVTOKEN_DEFAULTUSERNAME` |

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
