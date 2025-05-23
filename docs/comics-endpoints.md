# Comics Endpoints Documentation

The ComicCacher API provides several endpoints for retrieving and managing comics and their associated images.

## Base Path

All Comics endpoints are under:

```
/api/v1/comics
```

## Endpoints

### List All Comics

```
GET /api/v1/comics
```

Returns a list of all available comics in the system.

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "name": "Dilbert",
      "author": "Scott Adams",
      "description": "A comic strip about office politics",
      "source": "gocomics",
      "sourceIdentifier": "dilbert",
      "oldest": "1989-04-16",
      "newest": "2023-05-01",
      "enabled": true,
      "avatarAvailable": true
    },
    {
      "id": 2,
      "name": "Calvin and Hobbes",
      "author": "Bill Watterson",
      "description": "A comic strip about a boy and his tiger",
      "source": "gocomics",
      "sourceIdentifier": "calvinandhobbes",
      "oldest": "1985-11-18",
      "newest": "1995-12-31",
      "enabled": true,
      "avatarAvailable": true
    }
  ]
}
```

### Get Comic Details

```
GET /api/v1/comics/{comic}
```

Returns details about a specific comic.

#### Path Parameters

| Parameter | Type    | Required | Description                |
|-----------|---------|----------|----------------------------|
| comic     | Integer | Yes      | The numeric ID of the comic to get |

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "Dilbert",
    "author": "Scott Adams",
    "description": "A comic strip about office politics",
    "source": "gocomics",
    "sourceIdentifier": "dilbert",
    "oldest": "1989-04-16",
    "newest": "2023-05-01",
    "enabled": true,
    "avatarAvailable": true
  }
}
```

### Create or Update Comic

```
POST /api/v1/comics/{comic}
```

Creates a new comic with the specified ID.

#### Path Parameters

| Parameter | Type    | Required | Description                         |
|-----------|---------|----------|-------------------------------------|
| comic     | Integer | Yes      | The numeric ID to assign to the new comic   |

#### Request Body

```json
{
  "name": "Dilbert",
  "author": "Scott Adams",
  "description": "A comic strip about office politics",
  "source": "gocomics",
  "sourceIdentifier": "dilbert",
  "enabled": true
}
```

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 201,
  "message": "Comic created successfully",
  "data": {
    "id": 1,
    "name": "Dilbert",
    "author": "Scott Adams",
    "description": "A comic strip about office politics",
    "source": "gocomics",
    "sourceIdentifier": "dilbert",
    "oldest": null,
    "newest": null,
    "enabled": true,
    "avatarAvailable": false
  }
}
```

### Update Comic Details

```
PATCH /api/v1/comics/{comic}
```

Updates details for an existing comic.

#### Path Parameters

| Parameter | Type    | Required | Description                      |
|-----------|---------|----------|----------------------------------|
| comic     | Integer | Yes      | The numeric ID of the comic to update    |

#### Request Body

```json
{
  "description": "Updated description for this comic"
}
```

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Comic updated successfully",
  "data": {
    "id": 1,
    "name": "Dilbert",
    "description": "Updated description for this comic",
    "source": "gocomics",
    "sourceIdentifier": "dilbert",
    "startDate": "1989-04-16"
  }
}
```

### Delete Comic

```
DELETE /api/v1/comics/{comic}
```

Deletes a comic and all associated images.

#### Path Parameters

| Parameter | Type    | Required | Description                      |
|-----------|---------|----------|----------------------------------|
| comic     | Integer | Yes      | The numeric ID of the comic to delete    |

#### Response Format

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 200,
  "message": "Comic deleted successfully",
  "data": null
}
```

### Get Comic Avatar

```
GET /api/v1/comics/{comic}/avatar
```

Returns the avatar image for a comic.

#### Path Parameters

| Parameter | Type    | Required | Description                          |
|-----------|---------|----------|--------------------------------------|
| comic     | Integer | Yes      | The numeric ID of the comic to get avatar for|

#### Response Format

```json
{
  "mimeType": "image/png",
  "imageData": "base64encodedimagedata",
  "height": 200,
  "width": 200,
  "imageDate": null
}
```

### Get First Comic Strip

```
GET /api/v1/comics/{comic}/strips/first
```

Returns the first available comic strip for a comic.

#### Path Parameters

| Parameter | Type    | Required | Description                          |
|-----------|---------|----------|--------------------------------------|
| comic     | Integer | Yes      | The numeric ID of the comic                  |

#### Response Format

```json
{
  "mimeType": "image/gif",
  "imageData": "base64encodedimagedata",
  "height": 300,
  "width": 600,
  "imageDate": "1989-04-16"
}
```

### Get Next Comic Strip

```
GET /api/v1/comics/{comic}/next/{date}
```

Returns the next comic strip after the specified date.

#### Path Parameters

| Parameter | Type    | Required | Description                           |
|-----------|---------|----------|---------------------------------------|
| comic     | Integer | Yes      | The numeric ID of the comic                   |
| date      | String  | Yes      | The date in format "yyyy-MM-dd"       |

#### Response Format

```json
{
  "mimeType": "image/gif",
  "imageData": "base64encodedimagedata",
  "height": 300,
  "width": 600,
  "imageDate": "1989-04-17"
}
```

### Get Previous Comic Strip

```
GET /api/v1/comics/{comic}/previous/{date}
```

Returns the previous comic strip before the specified date.

#### Path Parameters

| Parameter | Type    | Required | Description                           |
|-----------|---------|----------|---------------------------------------|
| comic     | Integer | Yes      | The numeric ID of the comic                   |
| date      | String  | Yes      | The date in format "yyyy-MM-dd"       |

#### Response Format

```json
{
  "mimeType": "image/gif",
  "imageData": "base64encodedimagedata",
  "height": 300,
  "width": 600,
  "imageDate": "1989-04-15"
}
```

### Get Last Comic Strip

```
GET /api/v1/comics/{comic}/strips/last
```

Returns the most recent comic strip for a comic.

#### Path Parameters

| Parameter | Type    | Required | Description                           |
|-----------|---------|----------|---------------------------------------|
| comic     | Integer | Yes      | The numeric ID of the comic                   |

#### Response Format

```json
{
  "mimeType": "image/gif",
  "imageData": "base64encodedimagedata",
  "height": 300,
  "width": 600,
  "imageDate": "2023-05-01"
}
```

## Error Responses

All endpoints may return the following error responses:

### Comic Not Found (404)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 404,
  "message": "Comic not found with ID: 999",
  "data": null
}
```

### Comic Image Not Found (404)

```json
{
  "timestamp": "2023-05-01T10:15:30",
  "status": 404,
  "message": "Comic image not found for date: 1900-01-01",
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

1. **Comic Browser App**: Retrieve and display comics in a chronological order
2. **Comic Archive**: Build a complete collection of comic strips
3. **Reading Tracker**: Implement a system to track which comics a user has read
4. **Comic Search**: Create a search interface for finding specific comics