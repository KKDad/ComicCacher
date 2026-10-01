# Sources API

The queries and mutations behind the Sources page. Operators can read; only admins change anything. See [Comic Sources and Their Catalogs](../design/source-catalog.md) for how it works.

## GraphQL Queries

### sources / source

```graphql
query {
  sources {                     # OPERATOR
    id displayName kind hasCatalog catalogUrl canDetectStart
    catalogCount configuredCount activeCount
    lastRefreshed lastRefreshAttempt lastRefreshError refreshing
    settings { throttleMinDelayMs throttleMaxDelayMs retryMaxAttempts backfillMaxDaysBack }
  }
  source(id: "gocomics") {      # OPERATOR; null for an unknown id
    catalog(search: "peanuts", filter: NOT_CONFIGURED, first: 200) {
      totalCount
      pageInfo { hasNextPage endCursor }
      edges { node { identifier name author pageUrl thumbnailUrl thumbnailPending startDate startStripNumber removedAt comic { id active enabled } } }
    }
    orphans { id name sourceIdentifier }
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `kind` | `SourceKind` | `DAILY` (strips by date) or `INDEXED` (numbered) |
| `catalogCount` | `Int!` | Comics the source currently lists |
| `configuredCount` / `activeCount` | `Int!` | Configured comics from the source / of those, downloading |
| `refreshing` | `Boolean!` | A catalog refresh is running now |
| `settings` | `SourceSettings!` | Effective throttle, 429 retry and backfill settings, read-only (`application.properties`) |
| `catalog` | `SourceCatalogConnection!` | Entries by name. `filter`: `ALL`, `CONFIGURED`, `NOT_CONFIGURED`, `REMOVED`. `first` max 500 |
| `orphans` | `[Comic!]!` | Configured comics from the source that its catalog doesn't list |

`SourceCatalogEntry.thumbnailUrl` is null until the thumbnail has been downloaded (see `requestCatalogThumbnails`). `comic` is the configured comic, or null.

The `Comic` type also has fields for the page:

| Field | Type | Description |
|-------|------|-------------|
| `avatarPending` / `startPending` | `Boolean!` | An avatar download / start detection is queued or running |
| `reportedStartDate` / `reportedStartStripNumber` | `Date` / `Int` | Where the source says the comic starts, which can differ from a value an admin set |

## GraphQL Mutations

| Mutation | Role | Description |
|----------|------|-------------|
| `refreshSourceCatalog(source)` | ADMIN | Runs `SourceCatalogJob` for the source with `force=true`. Returns `TriggerBatchJobPayload`; an error when the source has no catalog or is already refreshing |
| `addComicFromCatalog(input: { source, identifier, active = true, enabled = true })` | ADMIN | Configures a catalog comic with defaults: its catalog name and author, `sourceIdentifier` = the identifier, daily, and the catalog's start. Queues its avatar and, when no start is known, start detection. Errors use `VALIDATION_ERROR` (unknown entry, already configured, name taken) |
| `fetchComicAvatar(id)` | ADMIN | Queues an avatar download. `QueueComicTaskPayload { queued, comic, errors }` |
| `detectComicStart(id)` | ADMIN | Queues reading the comic's start from its source |
| `requestCatalogThumbnails(source, identifiers)` | OPERATOR | Queues thumbnail downloads, at most 100 per call. Returns `{ queued }`, the number newly queued |

The Downloading and Visible switches use `updateComic(id, { active })` and `updateComic(id, { enabled })`. Per-comic and per-source backfill use `triggerJob("ComicBackfillJob", { comic: "<id>" })` and `triggerJob("ComicBackfillJob", { source: "<id>" })`.

## REST Endpoint

### GET /api/v1/sources/{source}/thumbnails/{identifier}

A cached catalog thumbnail, with `Cache-Control: max-age=604800`. No authentication. Returns 404 (`no-store`) when the thumbnail hasn't been downloaded; this endpoint never starts a download.
