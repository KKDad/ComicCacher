# Comic Sources and Their Catalogs

The **Sources** page in Comics Hub lists each place comics come from (GoComics, Comics Kingdom, Freefall). For each one it shows:
- how many of the source's comics are configured;
- the source's settings, read-only;
- a catalog of every comic the source offers.

Each catalog row has two switches. **Downloading** is the comic's `active` flag: the daily download and backfill fetch its strips. **Visible** is its `enabled` flag: readers can see it. Switching either on for a comic that isn't configured yet adds it to `comics.json` with defaults.

## ComicSource

Every source is one Spring bean implementing `ComicSource` (`comic-engine`, `org.stapledon.engine.source`):

| Method | Purpose |
|--------|---------|
| `id()`, `displayName()` | `"gocomics"`, `"GoComics"`. The id is what comics use in their `source` field |
| `downloader()` | The source's `ComicDownloaderStrategy` |
| `identifierFor(comic)` | The slug the downloader uses: `sourceIdentifier`, or the fallback it derives from the name. Matches configured comics to catalog entries |
| `comicPageUrl(identifier)` | The comic's page at the source, for people |
| `imageHosts()` | The only hosts thumbnails may be downloaded from |
| `catalog()` | Optional. Reads every comic the source offers |
| `startDetector()` | Optional. Reads where a comic starts at the source |

`SourceRegistry` collects every `ComicSource`, registers each downloader with `ComicDownloaderFacade`, and is the one list of sources. The batch jobs' `source` parameter options, comic validation and the Sources page all ask it. Adding a source means writing its downloader and one `ComicSource`; see [Adding a New Source](downloader-strategies.md#adding-a-new-source).

| Source | Catalog | Start |
|--------|---------|-------|
| GoComics | The A–Z page (`/comics/a-to-z`), one page of about 400 comics. Each anchor carries a JSON-LD `ImageObject` with the title, author and badge image | `"firstDate"` in the Next.js data of the comic's page |
| Comics Kingdom | The public WordPress API behind the site (`wp.comicskingdom.com/wp-json/wp/v2/ck_feature`), 100 per page, with each feature's byline, featured image and oldest strip | `ck_oldest_comic.date`, which comes with the catalog |
| Freefall | One fixed entry, no request | Strip 1 |

Every catalog and start request goes through `SourceThrottleService.withRetries`, under the source's own throttle and 429 back-off. GoComics pages are fetched with `BrowserFetcher`, which sends the desktop Chrome headers Cloudflare expects. A catalog page that parses to nothing is treated as a failure (the layout probably changed), and the stored catalog is kept.

## Catalog storage

`SourceCatalogRepository` keeps every catalog in `source-catalog.json` in the cache root (see [Operational State](../storage/operational-state.md)). A refresh merges the new list into the stored one:

- An entry seen for the first time gets `firstSeen`.
- An entry seen again gets `lastSeen`, its name, author and image updated, and `removedAt` cleared.
- An entry the source no longer lists gets `removedAt`. Entries are never deleted, so the page can show **No longer listed**, and a configured comic that disappears from its source shows up under **Not in the catalog**.

A configured comic is matched to its catalog entry by `ComicSource.identifierFor`, so older comics with no `sourceIdentifier` still match through the name fallback.

## SourceCatalogJob

`SourceCatalogJobConfig` defines the job. It fires daily (`batch.source-catalog.cron`, 05:00), but a scheduled run is skipped unless some catalog is older than `batch.source-catalog.max-age-days` (7). The skip check reads only `source-catalog.json`. A run:

1. Refreshes each due catalog (or only the `source` parameter's; `force=true` ignores the age).
2. Detects the start of up to `batch.source-catalog.start-detect-per-run` (5) configured comics per source that have none.
3. Deletes catalog thumbnails older than `comics.catalog.thumbnail-max-age-days` (30), and those of comics their source no longer lists.

A failed refresh fails the step after the other sources have run, so the batch history shows it. The page's **Refresh catalog** button runs this job for one source with `force=true`, so manual refreshes appear in the batch history and logs too. One refresh per source runs at a time.

## Thumbnails

Comics that aren't configured have no avatar. Their catalog thumbnails are downloaded **on demand**:

1. The page asks for the rows on screen (50 at a time) with the `requestCatalogThumbnails` mutation. That needs a signed-in operator, and asks for at most 100 per call.
2. `CatalogThumbnailService` queues each download on the single-threaded `catalogTaskExecutor`. A request that is already queued or cached is skipped. A failure isn't retried for `comics.catalog.thumbnail-failure-memo-hours` (24).
3. The download is paced under `downloader.sources.<source>-assets` when that is configured (the image CDNs need less care than the pages), otherwise under the source. It accepts only https URLs on the source's `imageHosts()`, redirects included, and at most 2 MB. The image must pass `ImageValidationService`.
4. The file is written to `{cache}/tmp/catalog-thumbnails/{source}/{identifier}.{ext}`. `tmp/` is excluded from storage metrics (`CacheLayout`) and is always safe to delete. `comics.catalog.thumbnail-dir` moves it.
5. `GET /api/v1/sources/{source}/thumbnails/{identifier}` serves cached files to anyone, the way comic images are served. It never starts a download, so anonymous requests can't make the server fetch anything.

Until a thumbnail is cached, the row shows the comic's initials. When a catalog comic is added, its avatar is queued on the same executor: the cached thumbnail is copied when there is one, otherwise the strategy's avatar download runs.

## Start dates

A comic's start is where its archive begins at the source:

- **`sourceStartDate`** for daily comics. Backfill never scans before it. Without it, backfill doesn't go further back than the oldest stored strip, as before.
- **`firstStripNumber`** for numbered comics.

`startSource` records where the value came from: `DETECTED` (the source said so, or a stored strip proved it) or `MANUAL` (an admin set it).

- **Detection** runs:
  - when a comic is added from its catalog, unless the catalog already gave the start (Comics Kingdom);
  - on the **Ask the source** button (`detectComicStart`);
  - for a few comics per source in each `SourceCatalogJob` run.

  It is recorded on the catalog entry, and saved on the comic unless an admin set the start. The Start date dialog then shows **The source says …**, with a button to use it.
- **Correction:** every comic write goes through `ComicManagementFacade.persist`. A stored strip older than `sourceStartDate`, or with a lower number than `firstStripNumber`, proves the value wrong, so it is moved back. This applies even to a value an admin set. The correction logs `Start date for … corrected` at WARN, plus an `AUDIT` line.

## Visibility

`enabled=false` (Visible off) hides a comic from everyone but admins:
- the `comics`, `comic`, `search` and `randomStrip` queries leave it out;
- favorites and last-read entries skip it;
- its strip images return 404.

Strip images are fetched without credentials, so the REST endpoint can't tell an admin from a reader. Avatars stay public. Admins pass `includeHidden: true` to `comics` to list hidden comics. Downloading is separate: a hidden comic can keep downloading, for example to fill its archive before it is shown.

## Key Source Files

| File | Purpose |
|------|---------|
| `comic-engine/.../engine/source/ComicSource.java` | The source contract |
| `comic-engine/.../engine/source/SourceRegistry.java` | Collects sources, registers downloaders |
| `comic-engine/.../engine/source/GoComicsSource.java`, `ComicsKingdomSource.java`, `FreefallSource.java` | The three sources |
| `comic-engine/.../engine/source/SourceCatalogService.java` | Refresh, join, add from catalog, start detection |
| `comic-engine/.../engine/source/SourceCatalogRepository.java` | `source-catalog.json` |
| `comic-engine/.../engine/source/CatalogThumbnailService.java` | On-demand thumbnails |
| `comic-engine/.../engine/source/ComicValidator.java` | Rules shared by `createComic`, `updateComic` and the catalog |
| `comic-engine/.../engine/batch/config/SourceCatalogJobConfig.java` | The job |
| `comic-api/.../api/resolver/SourceResolver.java` | GraphQL; see [Sources API](../api/sources.md) |
| `comic-hub/src/app/(dashboard)/sources/` | The pages, gated to operators in the server layout |
