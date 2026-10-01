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
    includeHidden: Boolean = false
    first: Int = 20
    after: String
  ): ComicConnection!
}
```

**Auth:** `@authenticated` (any authenticated user)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `search` | `String` | -- | Filter comics by name or author |
| `active` | `Boolean` | -- | Filter by whether new strips are downloaded |
| `enabled` | `Boolean` | -- | Filter by enabled (visible) status |
| `includeHidden` | `Boolean` | `false` | Include hidden (`enabled: false`) comics. Honoured for admins only; everyone else never sees hidden comics, here or in `comic`, `search` and `randomStrip` |
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

### randomStrip

Get a random strip. With `comicId`, a random strip from that comic; without it, a random comic and date.

```graphql
query {
  randomStrip(comicId: Int): ComicStrip
}
```

**Auth:** `@authenticated`

| Parameter | Type | Description |
|---|---|---|
| `comicId` | `Int` | Comic to pick from (null = any comic) |

**Returns:** `ComicStrip` (null if there is nothing to pick)

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
| `enabled` | `Boolean` | `true` | Whether readers see it |
| `source` | `String` | -- | Source provider (e.g., "gocomics") |
| `sourceIdentifier` | `String` | -- | Source's identifier for this comic |
| `publicationDays` | `[DayOfWeek!]` | -- | Days it publishes (null = daily) |
| `active` | `Boolean` | `true` | Whether new strips are downloaded |
| `firstStripNumber` | `Int` | -- | First strip number; required for numbered sources (Freefall) |
| `lastStripNumber` | `Int` | -- | Highest strip number downloaded |
| `sourceStartDate` | `Date` | -- | First strip date the source has |

**Returns:** `CreateComicPayload!` -- `{ comic: Comic, errors: [UserError!]! }`

The comic gets the next free id. A start given here (`sourceStartDate` or `firstStripNumber`) is recorded as `startSource: MANUAL`.

**Validation** (errors come back in `errors` with code `VALIDATION_ERROR` and `field` like `input.sourceIdentifier`; nothing is saved):
- `name` is required, unique (ignoring case), doesn't share another comic's storage folder, and isn't reserved for the cache's own folders (`tmp`, `batch-logs`, ...).
- `source` must be a registered source.
- `sourceIdentifier` must be lower-case letters, digits, `-` or `_`, and no other comic may already be that source comic.
- `firstStripNumber` is at least 1 and not after `lastStripNumber`; `sourceStartDate` isn't in the future.

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
| `enabled` | `Boolean` | Whether readers see it |
| `source` | `String` | Source provider |
| `sourceIdentifier` | `String` | Source identifier |
| `publicationDays` | `[DayOfWeek!]` | Publication days |
| `active` | `Boolean` | Whether new strips are downloaded |
| `firstStripNumber` | `Int` | First strip number |
| `lastStripNumber` | `Int` | Highest strip number downloaded |
| `sourceStartDate` | `Date` | First strip date the source has |

**Returns:** `UpdateComicPayload!` -- `{ comic: Comic, errors: [UserError!]! }`

The same validation as `createComic`, applied only to the fields that change, so a value saved before the rules existed never blocks an unrelated change. A new start is recorded as `startSource: MANUAL`. A stored strip older than the start corrects it on save (see [start dates](../design/source-catalog.md#start-dates)).

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
| `enabled` | `Boolean` | Whether readers see it. Hidden comics are left out of every query except for admins, and their strip images return 404 |
| `description` | `String` | Description |
| `avatarAvailable` | `Boolean` | Whether an avatar image exists |
| `avatarUrl` | `String` | URL path to avatar (e.g., `/api/v1/comics/123/avatar`) |
| `source` | `String` | Source provider (e.g., "gocomics", "comicskingdom") |
| `sourceIdentifier` | `String` | Identifier used by the source |
| `publicationDays` | `[DayOfWeek!]` | Days the comic publishes (null = daily) |
| `active` | `Boolean` | Whether new strips are downloaded |
| `firstStripNumber` / `lastStripNumber` | `Int` | Strip number range, for numbered comics |
| `sourceStartDate` | `Date` | First strip date the source has |
| `startSource` | `StartSource` | `DETECTED` or `MANUAL` |
| `avatarPending`, `startPending`, `reportedStartDate`, `reportedStartStripNumber` | | For the Sources page; see [Sources API](sources.md) |
| `strip(date: Date)` | `ComicStrip` | Strip for a specific date (null = latest) |
| `strips(dates: [Date!]!)` | `[ComicStrip!]!` | Strips for multiple dates (first 30 only) |
| `firstStrip` | `ComicStrip` | First (oldest) available strip |
| `lastStrip` | `ComicStrip` | Last (newest) available strip |
| `stripWindow(center: Date!, before: Int!, after: Int!)` | `[ComicStrip!]!` | Up to `before` older strips, the centre date and up to `after` newer strips, in chronological order. `before` and `after` are capped at 20. A centre date with no strip (e.g. a far-future date) comes back as an unavailable entry rather than being moved to the nearest strip; use `newest` to find the latest date |

### ComicStrip

| Field | Type | Description |
|---|---|---|
| `date` | `Date!` | Date of the strip |
| `available` | `Boolean!` | Whether a strip exists for this date |
| `imageUrl` | `String` | URL to the strip image (null if not available) |
| `width` | `Int` | Image width in pixels (null if not available) |
| `height` | `Int` | Image height in pixels (null if not available) |
| `transcript` | `String` | Transcript text from the comic page (null if not available) |
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
