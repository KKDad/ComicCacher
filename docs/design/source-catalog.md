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
| `detailsFetcher()` | Optional. Reads a comic's description and tags from its own page, for a catalog that doesn't carry them |

`SourceRegistry` collects every `ComicSource`, registers each downloader with `ComicDownloaderFacade`, and is the one list of sources. The batch jobs' `source` parameter options, comic validation and the Sources page all ask it. Adding a source means writing its downloader and one `ComicSource`; see [Adding a New Source](downloader-strategies.md#adding-a-new-source).

| Source | Catalog | Start | Details |
|--------|---------|-------|---------|
| GoComics | The A–Z page (`/comics/a-to-z`), one page of about 400 comics. Each anchor carries a JSON-LD `ImageObject` with the title, author and badge image | `"firstDate"` in the Next.js data of the comic's page | The comic's about page (`/{slug}/about`): the `ComicSeries` JSON-LD description, and `"comic":{"categories":[…]}` from the Next.js data (broad, e.g. "Newspaper Comic Strips") |
| Comics Kingdom | The public WordPress API behind the site (`wp.comicskingdom.com/wp-json/wp/v2/ck_feature`), 100 per page, with each feature's byline, featured image and oldest strip | `ck_oldest_comic.date`, which comes with the catalog | `excerpt` and the `ck_genre-*` entries of `class_list`, which come with the catalog |
| Freefall | One fixed entry, no request | Strip 1 | None |

Every catalog and start request goes through `SourceThrottleService.withRetries`, under the source's own throttle and 429 back-off. Details and thumbnails, which the job reads many of, fail fast instead: the first 429 backs the source off once and stops that source's work for the run (see [SourceCatalogJob](#sourcecatalogjob)). GoComics pages are fetched with `BrowserFetcher`, which sends the desktop Chrome headers Cloudflare expects. A catalog page that parses to nothing is treated as a failure (the layout probably changed), and the stored catalog is kept.

## Catalog storage

`SourceCatalogRepository` keeps every catalog in `source-catalog.json` in the cache root (see [Operational State](../storage/operational-state.md)). A refresh merges the new list into the stored one:

- An entry seen for the first time gets `firstSeen`.
- An entry seen again gets `lastSeen`, its name, author and image updated (and its description and tags, when the catalog carries them), and `removedAt` cleared.
- An entry the source no longer lists gets `removedAt`. Entries are never deleted, so the page can show **No longer listed**, and a configured comic that disappears from its source shows up under **Not in the catalog**.

A configured comic is matched to its catalog entry by `ComicSource.identifierFor`, so older comics with no `sourceIdentifier` still match through the name fallback.

## SourceCatalogJob

`SourceCatalogJobConfig` defines the job. It fires daily (`batch.source-catalog.cron`, 05:00), but a scheduled run is skipped unless some catalog is older than `batch.source-catalog.max-age-days` (7), or some details or thumbnails are due. The skip check reads only `source-catalog.json`, never the disk or a source. A run:

1. Refreshes each due catalog (or only the `source` parameter's; `force=true` ignores the age).
2. Detects the start of up to `batch.source-catalog.start-detect-per-run` (5) configured comics per source that have none.
3. Reads due [details](#details) for up to `batch.source-catalog.details-per-run` (100) comics per source.
4. Deletes stale [thumbnails](#thumbnails), then downloads up to `batch.source-catalog.thumbnails-per-run` (100) due ones per source.

Steps 2–4 stop a source at its first HTTP 429: the source has been backed off (honouring `Retry-After`), and the rest wait for the next day's run. Every request is paced by the source's throttle, the same one the daily download uses: at GoComics' 8–20 s, 100 about pages take about 25 minutes, so a full run ends before the 06:00 download. After the first week or so, most days have little to do.

A failed refresh fails the step after the other sources have run, so the batch history shows it. The page's **Refresh catalog** button runs this job for one source with `force=true`, so manual refreshes appear in the batch history and logs too. One refresh per source runs at a time.

## Details

The Sources page shows each comic's tags (genres or categories) as chips, which filter the list, and its description behind an info icon. Search matches descriptions too.

- **Comics Kingdom** carries them in its catalog, so each refresh updates them at no cost.
- **GoComics** needs one request per comic, to its about page. `SourceCatalogService.fetchDueDetails` reads them in `SourceCatalogJob`, configured comics first. Each answer is kept for a random 30–90 days (`detailsExpireAt`), so refetches spread out instead of coming due together. A failed read is retried the next day; a 429 leaves the comic due.

A comic added from its catalog gets the catalog's description.

## Thumbnails

Comics that aren't configured have no avatar. Their catalog thumbnails are downloaded in the background by `SourceCatalogJob`, and on demand for rows on screen that have none yet:

1. **Background:** `CatalogThumbnailService.prefetchDue` downloads thumbnails that were never saved, or whose last download failed more than a week ago (`thumbnailSavedAt`, `thumbnailFailedAt` in `source-catalog.json`), so the job never scans the disk to find work.
2. **On demand:** the page asks for the rows on screen (50 at a time) with the `requestCatalogThumbnails` mutation. That needs a signed-in operator, and asks for at most 100 per call. Each download is queued on the single-threaded `catalogTaskExecutor`. A request that is already queued or cached is skipped. A failure isn't retried for `comics.catalog.thumbnail-failure-memo-hours` (24).
3. The download is paced under `downloader.sources.<source>-assets` when that is configured (the image CDNs need less care than the pages), otherwise under the source. It accepts only https URLs on the source's `imageHosts()`, redirects included, and at most 8 MB. The image must pass `ImageValidationService`, and one wider than 400 px is shrunk to 400 px and saved as PNG.
4. The file is written to `{cache}/tmp/catalog-thumbnails/{source}/{identifier}.{ext}`. `tmp/` is excluded from storage metrics (`CacheLayout`) and is always safe to delete. `comics.catalog.thumbnail-dir` moves it.
5. A thumbnail is kept for `comics.catalog.thumbnail-max-age-days` (365) plus 0–90 days, fixed per comic so expiries spread out. The purge also deletes thumbnails of comics their source no longer lists. A deleted thumbnail becomes due again.
6. `GET /api/v1/sources/{source}/thumbnails/{identifier}` serves cached files to anyone, the way comic images are served. It never starts a download, so anonymous requests can't make the server fetch anything.

Until a thumbnail is cached, the row shows the comic's initials.

A configured comic's **avatar** is separate: `{cache}/{ComicDir}/avatar.png`, kept until replaced. When a catalog comic is added, its avatar is queued on the same executor: the cached thumbnail is copied when there is one, otherwise the strategy's avatar download runs. **Fetch avatar** on a row does the same, and `AvatarBackfillJob` (07:15) downloads any that are missing.

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
| `comic-engine/.../engine/source/SourceCatalogService.java` | Refresh, join, add from catalog, start detection, details |
| `comic-engine/.../engine/source/CatalogThumbnailService.java` | Catalog thumbnails: download, prefetch, purge |
| `comic-engine/.../engine/source/SourceCatalogRepository.java` | `source-catalog.json` |
| `comic-engine/.../engine/source/ComicValidator.java` | Rules shared by `createComic`, `updateComic` and the catalog |
| `comic-engine/.../engine/batch/config/SourceCatalogJobConfig.java` | The job |
| `comic-api/.../api/resolver/SourceResolver.java` | GraphQL; see [Sources API](../api/sources.md) |
| `comic-hub/src/app/(dashboard)/sources/` | The pages, gated to operators in the server layout |
