# Authentication Endpoints Documentation

The ComicCacher API provides endpoints for user registration, authentication, and token management.

## Base Path

All authentication endpoints are under:

```
/api/v1/auth
```

## Endpoints

### Register User

```
POST /api/v1/auth/register
```

Registers a new user in the system.

#### Request Body

```json
{
  "username": "newuser",
  "password": "securepassword123",
  "email": "user@example.com",
  "displayName": "John Doe"
}
```

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 201,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "newuser",
    "displayName": "John Doe"
  }
}
```

### Login

```
POST /api/v1/auth/login
```

Authenticates a user and provides access tokens.

#### Request Body

```json
{
  "username": "existinguser",
  "password": "securepassword123"
}
```

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Authentication successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "existinguser",
    "displayName": "Jane Smith"
  }
}
```

### Refresh Token

```
POST /api/v1/auth/refresh-token
```

Refreshes an existing authentication token.

#### Request Body

```json
"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Token refreshed successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "existinguser",
    "displayName": "Jane Smith"
  }
}
```

### Validate Token

```
POST /api/v1/auth/validate-token
```

Validates whether a JWT token is still valid.

#### Headers

| Header        | Value            | Required | Description                 |
|---------------|-----------------|-----------|-----------------------------|
| Authorization | Bearer {token}  | Yes       | JWT token to validate       |

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Token is valid",
  "data": true
}
```

## Error Responses

### Invalid Credentials (401)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 401,
  "message": "Invalid username or password",
  "data": null
}
```

### Registration Error (400)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 400,
  "message": "Username already exists",
  "data": null
}
```

### Invalid Token (401)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 401,
  "message": "Invalid or expired token",
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

- Include the access token in the Authorization header for authenticated requests:
  ```
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
  ```
- Use the refresh token to obtain a new access token when it expires
- Store tokens securely on the client side

## Use Cases

1. **User Registration**: Allow new users to create accounts
2. **User Authentication**: Authenticate users and provide secure access
3. **Session Management**: Maintain user sessions with token refresh
4. **Security Validation**: Validate tokens for protected endpoints