# Comics API

## GraphQL Queries

### comics

Get comics with optional search, filtering, and cursor-based pagination.

```graphql
query {
  comics(
    search: String
    active: Boolean
    enabled: Boolean
    first: Int = 20
    after: String
  ): ComicConnection!
}
```

**Auth:** `@authenticated` (any authenticated user)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `search` | `String` | -- | Filter comics by name or author |
| `active` | `Boolean` | -- | Filter by actively publishing status |
| `enabled` | `Boolean` | -- | Filter by enabled status |
| `first` | `Int` | `20` | Number of comics to return (max 50) |
| `after` | `String` | -- | Cursor for pagination |

**Returns:** `ComicConnection!` (see [Relay pagination](overview.md#relay-cursor-pagination))

```graphql
query {
  comics(search: "Calvin", first: 5) {
    edges {
      node {
        id
        name
        author
        newest
        oldest
        active
        source
      }
      cursor
    }
    pageInfo {
      hasNextPage
      endCursor
    }
    totalCount
  }
}
```

---

### comic

Get a specific comic by ID.

```graphql
query {
  comic(id: Int!): Comic
}
```

**Auth:** `@authenticated`

| Parameter | Type | Description |
|---|---|---|
| `id` | `Int!` | Comic ID |

**Returns:** `Comic` (null if not found)

```graphql
query {
  comic(id: 42) {
    id
    name
    author
    description
    oldest
    newest
    enabled
    active
    avatarAvailable
    avatarUrl
    source
    sourceIdentifier
    publicationDays
  }
}
```

---

### strip

Get a comic strip by comic ID and date. More efficient than `comic.strip` when you only need the strip.

```graphql
query {
  strip(comicId: Int!, date: Date!): ComicStrip
}
```

**Auth:** `@authenticated`

| Parameter | Type | Description |
|---|---|---|
| `comicId` | `Int!` | Comic ID |
| `date` | `Date!` | Strip date (YYYY-MM-DD) |

**Returns:** `ComicStrip` (null if not found)

```graphql
query {
  strip(comicId: 42, date: "2026-03-19") {
    date
    available
    imageUrl
    previous {
      date
      available
    }
    next {
      date
      available
    }
  }
}
```

---

### search

Full-text search across comic names, authors, and descriptions.

```graphql
query {
  search(query: String!, limit: Int = 20): SearchResults!
}
```

**Auth:** `@authenticated`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | `String!` | -- | Search query string |
| `limit` | `Int` | `20` | Maximum number of results |

**Returns:** `SearchResults!`

```graphql
query {
  search(query: "garfield", limit: 5) {
    comics {
      id
      name
      author
    }
    totalCount
    query
  }
}
```

---

## GraphQL Mutations

### createComic

Create a new comic entry.

```graphql
mutation {
  createComic(input: CreateComicInput!): CreateComicPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

**CreateComicInput fields:**

| Field | Type | Default | Description |
|---|---|---|---|
| `name` | `String!` | -- | Display name |
| `author` | `String` | -- | Author/creator |
| `description` | `String` | -- | Description |
| `enabled` | `Boolean` | `true` | Whether enabled for display |
| `source` | `String` | -- | Source provider (e.g., "gocomics") |
| `sourceIdentifier` | `String` | -- | Source's identifier for this comic |
| `publicationDays` | `[DayOfWeek!]` | -- | Days it publishes (null = daily) |
| `active` | `Boolean` | `true` | Whether actively publishing |

**Returns:** `CreateComicPayload!` -- `{ comic: Comic, errors: [UserError!]! }`

```graphql
mutation {
  createComic(input: {
    name: "Dilbert"
    author: "Scott Adams"
    source: "gocomics"
    sourceIdentifier: "dilbert"
  }) {
    comic {
      id
      name
    }
    errors {
      message
      field
      code
    }
  }
}
```

---

### updateComic

Update an existing comic. Only provided fields are updated.

```graphql
mutation {
  updateComic(id: Int!, input: UpdateComicInput!): UpdateComicPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

**UpdateComicInput fields:**

| Field | Type | Description |
|---|---|---|
| `name` | `String` | Display name |
| `author` | `String` | Author/creator |
| `description` | `String` | Description |
| `enabled` | `Boolean` | Whether enabled for display |
| `source` | `String` | Source provider |
| `sourceIdentifier` | `String` | Source identifier |
| `publicationDays` | `[DayOfWeek!]` | Publication days |
| `active` | `Boolean` | Whether actively publishing |

**Returns:** `UpdateComicPayload!` -- `{ comic: Comic, errors: [UserError!]! }`

```graphql
mutation {
  updateComic(id: 42, input: { enabled: false }) {
    comic {
      id
      name
      enabled
    }
    errors {
      message
    }
  }
}
```

---

### deleteComic

Delete a comic and its cached strips. This action is irreversible.

```graphql
mutation {
  deleteComic(id: Int!): DeleteComicPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

| Parameter | Type | Description |
|---|---|---|
| `id` | `Int!` | Comic ID to delete |

**Returns:** `DeleteComicPayload!` -- `{ success: Boolean!, errors: [UserError!]! }`

```graphql
mutation {
  deleteComic(id: 42) {
    success
    errors {
      message
      code
    }
  }
}
```

---

## REST Endpoints

REST endpoints serve binary image data only. All metadata operations use GraphQL.

### GET /api/v1/comics/{id}/avatar

Retrieve the avatar image for a comic.

**Auth:** None (public)

| Parameter | Type | Description |
|---|---|---|
| `id` | `Integer` (path) | Comic ID |

**Response:**
- `200 OK` -- Binary image with appropriate `Content-Type` (e.g., `image/png`). Cached for 1 day (`Cache-Control: max-age=86400`).
- `404 Not Found` -- Comic or avatar not found.

```
GET /api/v1/comics/42/avatar
```

---

### GET /api/v1/comics/{id}/strip/{date}

Retrieve the comic strip image for a specific date.

**Auth:** None (public)

| Parameter | Type | Description |
|---|---|---|
| `id` | `Integer` (path) | Comic ID |
| `date` | `LocalDate` (path) | Strip date (YYYY-MM-DD) |

**Response:**
- `200 OK` -- Binary image with appropriate `Content-Type`. Cached for 7 days (`Cache-Control: max-age=604800`).
- `404 Not Found` -- Comic or strip not found for the given date.

```
GET /api/v1/comics/42/strip/2026-03-19
```

---

## Types

### Comic

| Field | Type | Description |
|---|---|---|
| `id` | `Int!` | Unique identifier |
| `name` | `String!` | Display name |
| `author` | `String` | Author/creator |
| `oldest` | `Date` | Date of oldest cached strip |
| `newest` | `Date` | Date of newest cached strip |
| `enabled` | `Boolean` | Whether enabled for display |
| `description` | `String` | Description |
| `avatarAvailable` | `Boolean` | Whether an avatar image exists |
| `avatarUrl` | `String` | URL path to avatar (e.g., `/api/v1/comics/123/avatar`) |
| `source` | `String` | Source provider (e.g., "gocomics", "comicskingdom") |
| `sourceIdentifier` | `String` | Identifier used by the source |
| `publicationDays` | `[DayOfWeek!]` | Days the comic publishes (null = daily) |
| `active` | `Boolean` | Whether actively publishing |
| `strip(date: Date)` | `ComicStrip` | Strip for a specific date (null = latest) |
| `strips(dates: [Date!]!)` | `[ComicStrip!]!` | Strips for multiple dates |
| `firstStrip` | `ComicStrip` | First (oldest) available strip |
| `lastStrip` | `ComicStrip` | Last (newest) available strip |

### ComicStrip

| Field | Type | Description |
|---|---|---|
| `date` | `Date!` | Date of the strip |
| `available` | `Boolean!` | Whether a strip exists for this date |
| `imageUrl` | `String` | URL to the strip image (null if not available) |
| `previous` | `ComicStrip` | Previous strip (null if at beginning) |
| `next` | `ComicStrip` | Next strip (null if at end) |

### SearchResults

| Field | Type | Description |
|---|---|---|
| `comics` | `[Comic!]!` | Matching comics |
| `totalCount` | `Int!` | Total number of results |
| `query` | `String!` | The search query that was executed |

### DayOfWeek Enum

`MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`
