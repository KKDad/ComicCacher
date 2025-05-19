# Preferences Endpoints Documentation

The ComicCacher API provides endpoints for managing user preferences, including favorite comics and reading progress.

## Base Path

All preferences endpoints are under:

```
/api/v1/preferences
```

## Authentication

All preferences endpoints require authentication. Include a valid JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Endpoints

### Get User Preferences

```
GET /api/v1/preferences
```

Retrieves all preferences for the authenticated user.

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Success",
  "data": {
    "userId": "user123",
    "favoriteComics": [1, 2, 3],
    "lastReadDates": {
      "1": "2023-04-30",
      "2": "2023-04-25",
      "3": "2023-05-01"
    },
    "displaySettings": {
      "theme": "dark",
      "layout": "grid",
      "imagesPerPage": 7,
      "autoAdvance": true
    }
  }
}
```

### Add Comic to Favorites

```
POST /api/v1/preferences/comics/{comicId}/favorite
```

Adds a comic to the user's favorites list.

#### Path Parameters

| Parameter | Type    | Required | Description                              |
|-----------|---------|----------|------------------------------------------|
| comicId   | Integer | Yes      | The numeric ID of the comic to add to favorites  |

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Comic added to favorites",
  "data": {
    "userId": "user123",
    "favoriteComics": [1, 2, 3, 4],
    "lastReadDates": {
      "1": "2023-04-30",
      "2": "2023-04-25",
      "3": "2023-05-01"
    },
    "displaySettings": {
      "theme": "dark",
      "layout": "grid",
      "imagesPerPage": 7,
      "autoAdvance": true
    }
  }
}
```

### Remove Comic from Favorites

```
DELETE /api/v1/preferences/comics/{comicId}/favorite
```

Removes a comic from the user's favorites list.

#### Path Parameters

| Parameter | Type    | Required | Description                                  |
|-----------|---------|----------|----------------------------------------------|
| comicId   | Integer | Yes      | The numeric ID of the comic to remove from favorites |

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Comic removed from favorites",
  "data": {
    "userId": "user123",
    "favoriteComics": [1, 3],
    "lastReadDates": {
      "1": "2023-04-30",
      "2": "2023-04-25",
      "3": "2023-05-01"
    },
    "displaySettings": {
      "theme": "dark",
      "layout": "grid",
      "imagesPerPage": 7,
      "autoAdvance": true
    }
  }
}
```

### Update Last Read Date

```
POST /api/v1/preferences/comics/{comicId}/lastread
```

Updates the last read date for a specific comic.

#### Path Parameters

| Parameter | Type    | Required | Description                             |
|-----------|---------|----------|-----------------------------------------|
| comicId   | Integer | Yes      | The numeric ID of the comic to update   |

#### Request Body

```json
{
  "date": "2023-05-01"
}
```

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Last read date updated",
  "data": {
    "userId": "user123",
    "favoriteComics": [1, 2, 3],
    "lastReadDates": {
      "1": "2023-04-30",
      "2": "2023-05-01",
      "3": "2023-05-01"
    },
    "displaySettings": {
      "theme": "dark",
      "layout": "grid",
      "imagesPerPage": 7,
      "autoAdvance": true
    }
  }
}
```

### Update Display Settings

```
POST /api/v1/preferences/display-settings
```

Updates the user's display settings.

#### Request Body

```json
{
  "theme": "light",
  "layout": "list",
  "imagesPerPage": 10,
  "autoAdvance": false
}
```

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Display settings updated",
  "data": {
    "userId": "user123",
    "favoriteComics": [1, 2, 3],
    "lastReadDates": {
      "1": "2023-04-30",
      "2": "2023-04-25",
      "3": "2023-05-01"
    },
    "displaySettings": {
      "theme": "light",
      "layout": "list",
      "imagesPerPage": 10,
      "autoAdvance": false
    }
  }
}
```

## Response Fields

### User Preference Fields

| Field           | Description                                         |
|-----------------|-----------------------------------------------------|
| userId          | ID of the user whose preferences are being returned |
| favoriteComics  | Array of numeric comic IDs that the user has favorited |
| lastReadDates   | Map of comic IDs to their last read dates           |
| displaySettings | User interface display preferences                  |

### Display Settings Fields

| Field          | Description                                    | Possible Values                 |
|----------------|------------------------------------------------|---------------------------------|
| theme          | UI color theme preference                      | "light", "dark", "system"       |
| layout         | How comics should be displayed                 | "grid", "list", "single"        |
| imagesPerPage  | Number of images to show on a page             | Integer                         |
| autoAdvance    | Whether to automatically advance to next comic | Boolean                         |

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

### Comic Not Found (404)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 404,
  "message": "Comic not found with ID: 999",
  "data": null
}
```

### Bad Request (400)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 400,
  "message": "Invalid date format. Use yyyy-MM-dd",
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

1. **Personalization**: Allow users to customize their comic viewing experience
2. **Reading Tracking**: Help users keep track of which comics they've read
3. **Collections**: Enable users to build collections of their favorite comics
4. **Consistent Experience**: Maintain the same preferences across different devices
5. **User Engagement**: Improve user experience with personalized settings