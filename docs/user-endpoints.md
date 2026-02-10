# User Management API Documentation

> [!IMPORTANT]
> **GraphQL-Only**: All user profile operations use GraphQL.
> There are no REST endpoints for user management.

## GraphQL Endpoint

**Endpoint:** `POST /graphql`

All user operations require authentication. Include a valid JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Queries

### Get Current User Profile

Retrieve the profile information for the authenticated user.

```graphql
query GetMe {
  me {
    username
    email
    displayName
    created
    lastLogin
    roles
  }
}
```

**Response:**
```json
{
  "data": {
    "me": {
      "username": "johndoe",
      "email": "john.doe@example.com",
      "displayName": "John Doe",
      "created": "2023-01-15T08:30:45Z",
      "lastLogin": "2023-05-01T09:20:15Z",
      "roles": ["USER"]
    }
  }
}
```

---

## Mutations

### Update User Profile

Update the current user's profile information.

```graphql
mutation UpdateProfile($input: UpdateProfileInput!) {
  updateProfile(input: $input) {
    username
    email
    displayName
    created
    lastLogin
    roles
  }
}
```

**Variables:**
```json
{
  "input": {
    "email": "new.email@example.com",
    "displayName": "Johnny Doe"
  }
}
```

**Response:**
```json
{
  "data": {
    "updateProfile": {
      "username": "johndoe",
      "email": "new.email@example.com",
      "displayName": "Johnny Doe",
      "created": "2023-01-15T08:30:45Z",
      "lastLogin": "2023-05-01T09:20:15Z",
      "roles": ["USER"]
    }
  }
}
```

---

### Update Password

Update the current user's password.

```graphql
mutation UpdatePassword($currentPassword: String!, $newPassword: String!) {
  updatePassword(currentPassword: $currentPassword, newPassword: $newPassword)
}
```

**Variables:**
```json
{
  "currentPassword": "currentSecurePassword123",
  "newPassword": "newSecurePassword456"
}
```

**Response:**
```json
{
  "data": {
    "updatePassword": true
  }
}
```

---

### Delete Account

Delete the current user's account (irreversible action).

```graphql
mutation DeleteAccount($password: String!) {
  deleteAccount(password: $password)
}
```

**Variables:**
```json
{
  "password": "currentPassword123"
}
```

**Response:**
```json
{
  "data": {
    "deleteAccount": true
  }
}
```

---

## Types

### User

| Field       | Type       | Description                          |
|-------------|------------|--------------------------------------|
| username    | String!    | Unique username                      |
| email       | String!    | User's email address                 |
| displayName | String!    | User's display name                  |
| created     | DateTime!  | Account creation timestamp           |
| lastLogin   | DateTime   | Last login timestamp                 |
| roles       | [String!]! | User's assigned roles (e.g., "USER", "ADMIN") |

### UpdateProfileInput

| Field       | Type    | Required | Description                    |
|-------------|---------|----------|--------------------------------|
| email       | String  | No       | New email address              |
| displayName | String  | No       | New display name               |

---

## Error Handling

GraphQL errors follow the standard format:

### Unauthorized

```json
{
  "data": null,
  "errors": [
    {
      "message": "Authentication required",
      "extensions": {
        "classification": "UNAUTHORIZED"
      }
    }
  ]
}
```

### Invalid Current Password

```json
{
  "data": null,
  "errors": [
    {
      "message": "Current password is incorrect",
      "extensions": {
        "classification": "BAD_REQUEST"
      }
    }
  ]
}
```

### Password Complexity Error

```json
{
  "data": null,
  "errors": [
    {
      "message": "Password must be at least 8 characters and include uppercase, lowercase, and numbers",
      "extensions": {
        "classification": "BAD_REQUEST"
      }
    }
  ]
}
```

### Email Already Exists

```json
{
  "data": null,
  "errors": [
    {
      "message": "Email address is already in use",
      "extensions": {
        "classification": "CONFLICT"
      }
    }
  ]
}
```

---

## Password Requirements

When updating passwords via `updatePassword` mutation, the following requirements must be met:

- Minimum length of 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- Optional: at least one special character

---

## Use Cases

1. **Profile Viewing**: View user profile information via `me` query
2. **Account Management**: Update profile details (email, displayName) via `updateProfile` mutation
3. **Security**: Change password for improved security via `updatePassword` mutation
4. **User Administration**: Maintain accurate user contact information
5. **Account Removal**: Allow users to delete their account via `deleteAccount` mutation (requires password confirmation)
