# Authentication API Documentation

> [!IMPORTANT]
> **GraphQL-Only**: All authentication operations use GraphQL mutations.
> There are no REST endpoints for authentication.

## GraphQL Endpoint

**Endpoint:** `POST /graphql`

All authentication operations use the GraphQL endpoint. These mutations do not require authentication (except for `validateToken` query).

---

## Mutations

### Register User

Create a new user account and receive authentication tokens.

```graphql
mutation Register($input: RegisterInput!) {
  register(input: $input) {
    token
    refreshToken
    username
    displayName
  }
}
```

**Variables:**
```json
{
  "input": {
    "username": "newuser",
    "password": "securePassword123",
    "email": "user@example.com",
    "displayName": "John Doe"
  }
}
```

**Response:**
```json
{
  "data": {
    "register": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "username": "newuser",
      "displayName": "John Doe"
    }
  }
}
```

---

### Login

Authenticate with username and password to receive tokens.

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

**Variables:**
```json
{
  "input": {
    "username": "existinguser",
    "password": "securePassword123"
  }
}
```

**Response:**
```json
{
  "data": {
    "login": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "username": "existinguser",
      "displayName": "Jane Smith"
    }
  }
}
```

---

### Refresh Token

Obtain a new access token using a refresh token.

```graphql
mutation RefreshToken($refreshToken: String!) {
  refreshToken(refreshToken: $refreshToken) {
    token
    refreshToken
    username
    displayName
  }
}
```

**Variables:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "data": {
    "refreshToken": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "username": "existinguser",
      "displayName": "Jane Smith"
    }
  }
}
```

---

### Forgot Password

Request a password reset email (always returns true to prevent email enumeration).

```graphql
mutation ForgotPassword($email: String!) {
  forgotPassword(email: $email)
}
```

**Variables:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "data": {
    "forgotPassword": true
  }
}
```

---

### Reset Password

Reset password using a token from the password reset email.

```graphql
mutation ResetPassword($token: String!, $newPassword: String!) {
  resetPassword(token: $token, newPassword: $newPassword) {
    token
    refreshToken
    username
    displayName
  }
}
```

**Variables:**
```json
{
  "token": "reset-token-from-email",
  "newPassword": "newSecurePassword456"
}
```

---

## Queries

### Validate Token

Validate the current JWT token from the Authorization header.

```graphql
query ValidateToken {
  validateToken
}
```

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "data": {
    "validateToken": true
  }
}
```

---

## Types

### RegisterInput

| Field       | Type    | Required | Description                    |
|-------------|---------|----------|--------------------------------|
| username    | String! | Yes      | Desired username (must be unique) |
| password    | String! | Yes      | User's password                |
| email       | String! | Yes      | User's email address           |
| displayName | String! | Yes      | User's display name            |

### LoginInput

| Field    | Type    | Required | Description         |
|----------|---------|----------|---------------------|
| username | String! | Yes      | Username            |
| password | String! | Yes      | User's password     |

### AuthPayload

| Field        | Type    | Description                                    |
|--------------|---------|------------------------------------------------|
| token        | String! | JWT access token for authenticating API requests. Include in Authorization header as: `Bearer <token>` |
| refreshToken | String! | Refresh token for obtaining new access tokens  |
| username     | String! | Username of the authenticated user             |
| displayName  | String! | Display name of the authenticated user         |

---

## Error Handling

GraphQL errors follow the standard format:

### Invalid Credentials

```json
{
  "data": null,
  "errors": [
    {
      "message": "Invalid username or password",
      "extensions": {
        "classification": "UNAUTHORIZED"
      }
    }
  ]
}
```

### Username Already Exists

```json
{
  "data": null,
  "errors": [
    {
      "message": "Username already exists",
      "extensions": {
        "classification": "BAD_REQUEST"
      }
    }
  ]
}
```

### Invalid Token

```json
{
  "data": null,
  "errors": [
    {
      "message": "Invalid or expired token",
      "extensions": {
        "classification": "UNAUTHORIZED"
      }
    }
  ]
}
```

---

## Token Management

### JWT Token Format

The system uses JSON Web Tokens (JWT) for authentication with the following claims:

- **sub**: Subject (username)
- **iat**: Issued at (timestamp)
- **exp**: Expiration time (timestamp)
- **roles**: User roles

### Token Expiration

- Access tokens expire after 1 hour (3600 seconds)
- Refresh tokens expire after 7 days

### Token Usage

Include the access token in the Authorization header for authenticated requests:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Use the refresh token to obtain a new access token when it expires. Store tokens securely on the client side (never in localStorage - use httpOnly cookies or secure session storage).

---

## Use Cases

1. **User Registration**: Allow new users to create accounts via `register` mutation
2. **User Authentication**: Authenticate users and provide secure access via `login` mutation
3. **Session Management**: Maintain user sessions with token refresh using `refreshToken` mutation
4. **Security Validation**: Validate tokens for protected endpoints using `validateToken` query
5. **Password Recovery**: Handle password reset flow via `forgotPassword` and `resetPassword` mutations
