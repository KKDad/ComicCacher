# Preferences API Documentation

> [!IMPORTANT]
> **GraphQL-Only**: All user preference operations use GraphQL.
> There are no REST endpoints for preferences.

## GraphQL Endpoint

**Endpoint:** `POST /graphql`

All preference operations require authentication. Include a valid JWT token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Queries

### Get User Preferences

Retrieve all preferences for the authenticated user.

```graphql
query GetPreferences {
  preferences {
    username
    favoriteComics
    lastReadDates
    displaySettings
  }
}
```

**Response:**
```json
{
  "data": {
    "preferences": {
      "username": "johndoe",
      "favoriteComics": [1, 2, 3],
      "lastReadDates": {
        "1": {
          "comicId": 1,
          "lastReadDate": "2023-05-01",
          "currentStrip": "2023-05-01"
        },
        "2": {
          "comicId": 2,
          "lastReadDate": "2023-04-30",
          "currentStrip": "2023-04-30"
        }
      },
      "displaySettings": {
        "theme": "dark",
        "layout": "grid",
        "imagesPerPage": 7,
        "autoAdvance": true
      }
    }
  }
}
```

---

## Mutations

### Add Comic to Favorites

Add a comic to the user's favorites list.

```graphql
mutation AddFavorite($comicId: ID!) {
  addFavorite(comicId: $comicId) {
    username
    favoriteComics
  }
}
```

**Variables:**
```json
{
  "comicId": "4"
}
```

**Response:**
```json
{
  "data": {
    "addFavorite": {
      "username": "johndoe",
      "favoriteComics": [1, 2, 3, 4]
    }
  }
}
```

---

### Remove Comic from Favorites

Remove a comic from the user's favorites list.

```graphql
mutation RemoveFavorite($comicId: ID!) {
  removeFavorite(comicId: $comicId) {
    username
    favoriteComics
  }
}
```

**Variables:**
```json
{
  "comicId": "2"
}
```

**Response:**
```json
{
  "data": {
    "removeFavorite": {
      "username": "johndoe",
      "favoriteComics": [1, 3, 4]
    }
  }
}
```

---

### Update Last Read Date

Update the last read date for a specific comic.

```graphql
mutation UpdateLastRead($comicId: ID!, $date: Date!, $currentStrip: Date) {
  updateLastRead(comicId: $comicId, date: $date, currentStrip: $currentStrip) {
    username
    lastReadDates
  }
}
```

**Variables:**
```json
{
  "comicId": "1",
  "date": "2023-05-02",
  "currentStrip": "2023-05-02"
}
```

**Response:**
```json
{
  "data": {
    "updateLastRead": {
      "username": "johndoe",
      "lastReadDates": {
        "1": {
          "comicId": 1,
          "lastReadDate": "2023-05-02",
          "currentStrip": "2023-05-02"
        }
      }
    }
  }
}
```

---

### Update Display Settings

Update the user's display settings.

```graphql
mutation UpdateDisplaySettings($settings: JSON!) {
  updateDisplaySettings(settings: $settings) {
    username
    displaySettings
  }
}
```

**Variables:**
```json
{
  "settings": {
    "theme": "light",
    "layout": "list",
    "imagesPerPage": 10,
    "autoAdvance": false
  }
}
```

**Response:**
```json
{
  "data": {
    "updateDisplaySettings": {
      "username": "johndoe",
      "displaySettings": {
        "theme": "light",
        "layout": "list",
        "imagesPerPage": 10,
        "autoAdvance": false
      }
    }
  }
}
```

---

## Types

### UserPreference

| Field           | Type              | Description                                         |
|-----------------|-------------------|-----------------------------------------------------|
| username        | String!           | Username this preference belongs to                 |
| favoriteComics  | [Int!]            | List of comic IDs marked as favorites               |
| lastReadDates   | JSON              | Map of comic IDs to LastReadEntry objects           |
| displaySettings | JSON              | Display settings as key-value pairs                 |

### LastReadEntry

| Field         | Type    | Description                          |
|---------------|---------|--------------------------------------|
| comicId       | Int!    | Comic ID                             |
| lastReadDate  | Date!   | Last date the user read this comic   |
| currentStrip  | Date    | Current strip date in reading flow   |

### Display Settings (JSON)

Common display settings fields:

| Field          | Type    | Description                                    | Possible Values                 |
|----------------|---------|------------------------------------------------|---------------------------------|
| theme          | String  | UI color theme preference                      | "light", "dark", "system"       |
| layout         | String  | How comics should be displayed                 | "grid", "list", "single"        |
| imagesPerPage  | Number  | Number of images to show on a page             | Integer (1-50)                  |
| autoAdvance    | Boolean | Whether to automatically advance to next comic | true, false                     |

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

### Comic Not Found

```json
{
  "data": null,
  "errors": [
    {
      "message": "Comic not found with ID: 999",
      "extensions": {
        "classification": "NOT_FOUND"
      }
    }
  ]
}
```

### Invalid Settings

```json
{
  "data": null,
  "errors": [
    {
      "message": "Invalid display settings format",
      "extensions": {
        "classification": "BAD_REQUEST"
      }
    }
  ]
}
```

---

## Use Cases

1. **Personalization**: Allow users to customize their comic viewing experience via `updateDisplaySettings`
2. **Reading Tracking**: Help users keep track of which comics they've read via `updateLastRead`
3. **Collections**: Enable users to build collections of their favorite comics via `addFavorite` / `removeFavorite`
4. **Consistent Experience**: Maintain the same preferences across different devices via `preferences` query
5. **User Engagement**: Improve user experience with personalized settings stored in `displaySettings`
