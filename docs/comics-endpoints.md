# Comics API Endpoints

This document describes the API endpoints for comic operations in ComicCacher.

> [!IMPORTANT]
> **GraphQL Migration Complete**: All comic metadata operations now use GraphQL.
> Only the avatar image endpoint remains as REST (binary data over HTTP).

## GraphQL Endpoint

**Endpoint:** `POST /graphql`

All comic metadata queries and mutations use the GraphQL endpoint. See the [GraphQL Schema](../comic-api/src/main/resources/graphql/comics-schema.graphql) for full details.

### Comic Queries

#### List Comics (with Pagination)

```graphql
query ListComics($first: Int, $after: String, $searchTerm: String) {
  comics(first: $first, after: $after, searchTerm: $searchTerm) {
    edges {
      cursor
      node {
        id
        name
        description
        oldest
        newest
        avatarUrl
      }
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

#### Get Single Comic

```graphql
query GetComic($id: ID!) {
  comic(id: $id) {
    id
    name
    description
    source
    sourceIdentifier
    oldest
    newest
    avatarUrl
    strip(date: "2024-01-15") {
      imageUrl
      date
      previous
      next
    }
    firstStrip {
      imageUrl
      date
      next
    }
    lastStrip {
      imageUrl
      date
      previous
    }
  }
}
```

### Comic Mutations

#### Create Comic

```graphql
mutation CreateComic($input: CreateComicInput!) {
  createComic(input: $input) {
    id
    name
  }
}
```

#### Update Comic

```graphql
mutation UpdateComic($id: ID!, $input: UpdateComicInput!) {
  updateComic(id: $id, input: $input) {
    id
    name
    description
  }
}
```

#### Delete Comic

```graphql
mutation DeleteComic($id: ID!) {
  deleteComic(id: $id)
}
```

---

## REST Endpoints (Binary Data Only)

### Get Comic Avatar

Retrieves the avatar image for a comic.

**Endpoint:** `GET /api/v1/comics/{comicId}/avatar`

**Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `comicId` | Path | Comic ID (integer) |

**Response:**
- **200 OK**: Returns the avatar image as binary data with appropriate `Content-Type` header (e.g., `image/png`)
- **404 Not Found**: Avatar not available for this comic

**Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/comics/42/avatar" \
  -H "Authorization: Bearer <token>" \
  --output avatar.png
```

### Get Comic Strip

Retrieves the comic strip image for a specific date.

**Endpoint:** `GET /api/v1/comics/{comicId}/strip/{date}`

**Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `comicId` | Path | Comic ID (integer) |
| `date`    | Path | Strip date in ISO-8601 format (`yyyy-MM-dd`) |

**Response:**
- **200 OK**: Returns the strip image as binary data with appropriate `Content-Type` header (e.g., `image/jpeg`) and 7-day cache control
- **404 Not Found**: Strip not available for this comic on the given date

**Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/comics/42/strip/2024-01-15" \
  -H "Authorization: Bearer <token>" \
  --output strip.jpg
```

---

## Pagination

ComicCacher uses **Relay-style cursor pagination** for GraphQL:

| Parameter | Type | Description |
|-----------|------|-------------|
| `first` | Int | Number of items to fetch (max 100) |
| `after` | String | Cursor to fetch items after |

**Example Response:**
```json
{
  "data": {
    "comics": {
      "edges": [
        {
          "cursor": "Y3Vyc29yOjQ1",
          "node": {
            "id": "45",
            "name": "Garfield"
          }
        }
      ],
      "pageInfo": {
        "hasNextPage": true,
        "hasPreviousPage": false,
        "startCursor": "Y3Vyc29yOjQ1",
        "endCursor": "Y3Vyc29yOjU0"
      },
      "totalCount": 127
    }
  }
}
```

---

## Custom Scalars

The GraphQL API uses these custom scalar types:

| Scalar | Format | Example |
|--------|--------|---------|
| `Date` | ISO-8601 date | `"2024-01-15"` |
| `DateTime` | ISO-8601 datetime | `"2024-01-15T10:30:00Z"` |
| `JSON` | Arbitrary JSON | `{"key": "value"}` |

---

## Error Handling

GraphQL errors are returned in the standard format:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Comic not found",
      "path": ["comic"],
      "extensions": {
        "classification": "NOT_FOUND"
      }
    }
  ]
}
```