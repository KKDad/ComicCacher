# Authentication API

## Queries

### validateToken

Validate the current JWT token from the Authorization header.

```graphql
query {
  validateToken: Boolean!
}
```

**Auth:** `@public`

**Returns:** `Boolean!` -- `true` if the token is valid, `false` otherwise.

```graphql
query {
  validateToken
}
```

---

## Mutations

### register

Register a new user account. Returns an authentication payload with JWT tokens on success.

```graphql
mutation {
  register(input: RegisterInput!): AuthPayload!
}
```

**Auth:** `@public`

**RegisterInput fields:**

| Field | Type | Description |
|---|---|---|
| `username` | `String!` | Desired username (must be unique) |
| `password` | `String!` | Password |
| `email` | `String!` | Email address |
| `displayName` | `String` | Display name |

**Returns:** `AuthPayload!`

```graphql
mutation {
  register(input: {
    username: "reader1"
    password: "s3cureP@ss"
    email: "reader1@example.com"
    displayName: "Comic Reader"
  }) {
    token
    refreshToken
    username
    displayName
  }
}
```

---

### login

Authenticate with username and password.

```graphql
mutation {
  login(input: LoginInput!): AuthPayload!
}
```

**Auth:** `@public`

**LoginInput fields:**

| Field | Type | Description |
|---|---|---|
| `username` | `String!` | Username or email address |
| `password` | `String!` | Password |

**Returns:** `AuthPayload!`

```graphql
mutation {
  login(input: {
    username: "reader1"
    password: "s3cureP@ss"
  }) {
    token
    refreshToken
    username
    displayName
  }
}
```

---

### refreshToken

Refresh an expired JWT access token using a refresh token.

```graphql
mutation {
  refreshToken(refreshToken: String!): AuthPayload!
}
```

**Auth:** `@public`

| Parameter | Type | Description |
|---|---|---|
| `refreshToken` | `String!` | Refresh token from a previous login/register response |

**Returns:** `AuthPayload!`

```graphql
mutation {
  refreshToken(refreshToken: "eyJhbGciOi...") {
    token
    refreshToken
    username
  }
}
```

---

### forgotPassword

Request a password reset email. Always returns `true` to prevent email enumeration.

```graphql
mutation {
  forgotPassword(email: String!): Boolean!
}
```

**Auth:** `@public`

| Parameter | Type | Description |
|---|---|---|
| `email` | `String!` | Email address associated with the account |

**Returns:** `Boolean!` -- Always `true`.

```graphql
mutation {
  forgotPassword(email: "reader1@example.com")
}
```

---

### resetPassword

Reset password using a token from the password reset email.

```graphql
mutation {
  resetPassword(token: String!, newPassword: String!): AuthPayload!
}
```

**Auth:** `@public`

| Parameter | Type | Description |
|---|---|---|
| `token` | `String!` | Token from the password reset email |
| `newPassword` | `String!` | New password |

**Returns:** `AuthPayload!`

```graphql
mutation {
  resetPassword(token: "reset-token-abc", newPassword: "n3wP@ssword") {
    token
    refreshToken
    username
  }
}
```

---

## JWT Lifecycle

1. **Obtain tokens** via `login` or `register`. Both return an `AuthPayload` containing `token` (access) and `refreshToken`.
2. **Use the access token** in the `Authorization` header for authenticated requests: `Bearer <token>`.
3. **When the access token expires**, the API returns an `UNAUTHENTICATED` error with code `TOKEN_EXPIRED`.
4. **Refresh the token** by calling `refreshToken` with the refresh token. A new `AuthPayload` with fresh tokens is returned.
5. **Password reset flow**: Call `forgotPassword` with the user's email, then `resetPassword` with the emailed token and the new password. A new `AuthPayload` is returned on success.

## Types

### AuthPayload

| Field | Type | Description |
|---|---|---|
| `token` | `String!` | JWT access token for API requests |
| `refreshToken` | `String!` | Refresh token for obtaining new access tokens |
| `username` | `String!` | Username of the authenticated user |
| `displayName` | `String` | Display name of the authenticated user |
