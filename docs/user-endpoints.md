# User Endpoints Documentation

The ComicCacher API provides endpoints for managing user profiles and account settings.

## Base Path

All user-related endpoints are under:

```
/api/v1/users
```

## Authentication

All user endpoints require authentication. Include a valid JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Endpoints

### Get User Profile

```
GET /api/v1/users/profile
```

Retrieves the profile information for the authenticated user.

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "username": "johndoe",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "createdAt": "2023-01-15T08:30:45",
    "lastLogin": "2023-05-01T09:20:15",
    "roles": ["USER"]
  }
}
```

### Update User Profile

```
PUT /api/v1/users/profile
```

Updates the profile information for the authenticated user.

#### Request Body

```json
{
  "email": "new.email@example.com",
  "firstName": "Johnny",
  "lastName": "Doe"
}
```

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Profile updated successfully",
  "data": {
    "id": 1,
    "username": "johndoe",
    "email": "new.email@example.com",
    "firstName": "Johnny",
    "lastName": "Doe",
    "createdAt": "2023-01-15T08:30:45",
    "lastLogin": "2023-05-01T09:20:15",
    "roles": ["USER"]
  }
}
```

### Update Password

```
PUT /api/v1/users/password
```

Updates the password for the authenticated user.

#### Request Body

```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newSecurePassword456"
}
```

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Password updated successfully",
  "data": {
    "status": "success"
  }
}
```

## Response Fields

### User Profile Fields

| Field      | Description                                  |
|------------|----------------------------------------------|
| id         | Unique identifier for the user               |
| username   | User's login name                            |
| email      | User's email address                         |
| firstName  | User's first name                            |
| lastName   | User's last name                             |
| createdAt  | When the account was created                 |
| lastLogin  | Last time the user logged in                 |
| roles      | User's assigned roles in the system          |

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

### Invalid Password (400)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 400,
  "message": "Current password is incorrect",
  "data": null
}
```

### Password Complexity Error (400)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 400,
  "message": "Password must be at least 8 characters and include uppercase, lowercase, and numbers",
  "data": null
}
```

### Email Already Exists (409)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 409,
  "message": "Email address is already in use",
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

## Password Requirements

When updating passwords, the following requirements must be met:

- Minimum length of 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- Optional: at least one special character

## Use Cases

1. **Account Management**: Allow users to view and update their profile information
2. **Security**: Enable users to change their password for improved security
3. **User Administration**: Maintain accurate user contact information
4. **Personalization**: Store user details for personalization across the application