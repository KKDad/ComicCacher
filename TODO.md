# ComicCacher TODO

Ordered by priority, most urgent first within each tier.

## High

### Fix strips silently not saved (Mother Goose & Grimm, Sherman's Lagoon)

- **Problem:** since 2026-01-09 no strip files have been written for either comic, yet `retrieval-status.json` records `SUCCESS` daily (through 2026-09-21). Downloads and `image-hashes.json` updates happen; the image file never lands
- **Cause (confirmed on prod, fixed):** #190 (2026-01-09) made `ComicIndexService` put the date index for names outside `[a-zA-Z0-9 _-]` in `comic_{id}/`. On prod those directories (`comic_1177918400`, `comic_62159896`) are `root:root 755`, but the container runs as `comicapi` (uid 1001). So each day the strip was written, its hash recorded, the index write failed, and the save's rollback deleted the strip. The downloader had already recorded `SUCCESS`
- **Fix:** the index now uses `ComicIdentifier.getDirectoryName()`, the same directory as the strips. Failed saves are recorded as `STORAGE_ERROR`
- **After deploying:**
  1. On the host, delete `comic_1177918400/` and `comic_62159896/` under `/var/lib/docker/volumes/comics_comicdata/_data`
  2. Backfill 2026-01-10 onward for both comics

### Fix startup catch-up jobs blocking readiness

- **Problem:** `StartupJobRunner.onApplicationReady()` runs missed daily jobs synchronously on `main`. Readiness (`ACCEPTING_TRAFFIC`) isn't published until they finish, so `/actuator/health` returns 503 `OUT_OF_SERVICE`. On dev (2.4.7 deploy, 2026-09-24) a gocomics backfill kept it unhealthy for many minutes
- **Risk:** `prod-run.sh` rolls back if the container isn't healthy within 180s. Any prod restart after a daily job's scheduled time, on a day it hasn't run yet, will trigger a catch-up and roll back the deploy. This can block shipping the 429 fix below
- **Fix:** launch catch-up jobs asynchronously (e.g. on the batch `TaskExecutor`), keeping their order if they depend on each other. Add a test that `onApplicationReady` returns without waiting
- **Also check:** whether `hasJobRunToday` decides "today" in UTC or `batch.timezone`

### Verify the gocomics 429 fix in prod

- **Background:** since 2026-09-22, six gocomics comics have failed daily with HTTP 429. It's source-wide rate limiting, not per-comic blocking. #318 added 429 retry with source-wide backoff (`downloader.sources.gocomics.retry.*`) and a Chrome 154 User-Agent
- **After deploying:** check the 07:30 `ComicDownloadJob` logs for `Rate limited (HTTP 429)` warnings and whether the retries succeed. If not, lower `downloader.sources.gocomics.throttle.*` or spread the run out
- **Follow-up:** 429s are recorded as `COMIC_UNAVAILABLE`, which hides them among genuine "no strip today" results. Give them their own status (e.g. `RATE_LIMITED`)
- **Optional:** advertise and decode `zstd` like real Chrome (needs a pure-Java decoder such as `io.airlift:aircompressor` 2.x)

## Medium

### Fix comic mutations dropping fields

- `createComic` and `updateComic` in `ComicResolver` ignore `publicationDays` and `active` from their inputs
- Neither input accepts `firstStripNumber` / `lastStripNumber`, so indexed comics (Freefall) can't be created through the API
- As a result, prod config changes have to be made by stopping the API and editing `comics.json`
- Add resolver tests showing every input field reaches the saved `ComicItem`

### Alert when a comic stops getting new strips

- Alert when a comic has no new strip file on disk for N days, whatever its retrieval status says. The Mother Goose & Grimm bug went unnoticed for 8 months because the status said `SUCCESS`
- Channel: webhook, email, or in-app

### Replace the Selenium GoComics IT and remove Selenium

- `GoComicsIntegrationIT` tests the legacy Selenium `GoComics` class, which prod has never used. Prod uses the Jsoup `GoComicsDownloaderStrategy`, and the prod image has no Chrome. So the IT can pass while prod is broken, and fail while prod is fine
- **Steps:**
  1. Rewrite the IT against `GoComicsDownloaderStrategy`, paced through `SourceThrottleService` with only a few fetches
  2. Delete `GoComics` (plus anything in `DailyComic` / `IDailyComic` only it uses) and drop `selenium-java` / `webdrivermanager` from the root, `comic-api`, and `comic-engine` `build.gradle`
  3. Delete the unused `comic-common/.../infrastructure/web/DefaultTrustManager.java`
  4. Update the "Legacy downloaders" notes in `docs/design/architecture.md` and `docs/design/download-pipeline.md`

### Sources configuration screen

- Admin UI listing each source (GoComics, ComicsKingdom, Freefall) with:
  - Enabled/disabled toggle
  - Comics configured vs. available (e.g. "8 of 400")
  - Max days back to fetch
  - Runtime throttle and retry settings (already configurable in `application.properties` under `downloader.sources.<source>.throttle.*` / `retry.*`, but changes need a redeploy)
  - Buttons: refresh the list of available comics from the source; run backfill for one comic or the whole source
- Comic lists: GoComics https://www.gocomics.com/comics/a-to-z, ComicsKingdom https://www.comicskingdom.com/guide

### Promote comics from dev to prod

- A job that copies strips dev already downloaded into prod storage so prod doesn't download them again
- Modes: last 7 days (default, recurring) and all-time (one-off)
- Only copy what prod is missing; never overwrite. Bring the metadata along (sidecar JSON, image hashes, date indexes) using atomic writes (`docs/storage/overview.md`)
- **Open questions:** transport (shared NFS mount, API pull, or rsync over SSH), which instance runs it, and whether it can be scoped per comic

### Respect robots.txt

- Fetch and honor `robots.txt` disallow rules for GoComics and ComicsKingdom before scraping (as dosage does)

### Clean up deprecated Java APIs

- Jsoup `.first()` / `.last()` → `.selectFirst()` or streams: 7 uses in `GoComics`, `GoComicsDownloaderStrategy`, `ComicsKingdom`, `ComicsKingdomDownloaderStrategy` (some go away with the Selenium removal)
- Guava `@VisibleForTesting`: 3 uses (`RetrievalStatusRepository`, `JsonRetrievalStatusRepository`, `JsonErrorTrackingRepository`)
- Guava `Files.getNameWithoutExtension()` → plain Java: 2 uses (`ImageUtils`, `FileSystemComicStorageFacade`)

### Decide whether to keep OpenAPI/Swagger

- Only 2 REST endpoints remain (image streaming); everything else is GraphQL
- If it isn't worth keeping, remove springdoc, the openApi task config in `comic-api/build.gradle`, `generate-openapi-docs.sh`, and `openapi.json`

## Low

### Finish forgot password

- `comic-hub/src/app/(auth)/forgot-password/page.tsx` doesn't call `requestPasswordReset`; it just shows the success view
- It also needs SMTP, which is off by default. Set `MAIL_HOST`, `MAIL_PORT` (587), `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, and `MAIL_RESET_URL_BASE`, then set `management.health.mail.enabled=true` (`comic-api/src/main/resources/application.properties`)

### Fix deploy script quirks

- **Wrong rollback baseline label:** `current_ref()` in `utils/prod-run.sh` takes the text after the last `:` of `.Config.Image`, which for a digest-pinned image is the digest hex. Rollback still works, but the plan output and audit log are wrong. Fix: `ref="${config_image%%@*}"; tag="${ref##*:}"`
- **Piped confirmation swallowed:** in `prod-build-and-run.sh`, earlier `ssh`/`scp` calls consume stdin, so `echo y | …` never reaches `Continue?`, and the script exits silently. Fix: `ssh -n` on the staging call, and print a message when `read` gets no input

### Add ETag / Last-Modified to image endpoints

- `/api/v1/comics/{id}/avatar` and `/strip/*` already send `Cache-Control: max-age` (1 day / 7 days). Add `ETag` or `Last-Modified` so clients can revalidate cheaply once it expires

### Move cache-root JSON files into a `data/` folder

- The cache root holds ~9 loose JSON files (`comics.json`, `users.json`, `retrieval-status.json`, etc.; see `docs/storage/overview.md`). Move them into `data/`, update the code paths, and migrate existing prod/dev storage

### Upgrade to Java 25

- Do the deprecated-API cleanup first
- Update the Gradle toolchain, CI, and Docker base images

### Enable the Gradle configuration cache

- https://docs.gradle.org/current/userguide/configuration_cache_enabling.html

## Ideas

- **CBZ/PDF export:** export a date range of strips for offline reading (images are already on disk)
- **OPDS feed:** serve comics to reader apps like Panels, Chunky, and KOReader (Kavita and Komga do this)
- **More sources:**
  - [XKCD](https://xkcd.com/archive/) (indexed)
  - [The Web Comic Factory](http://www.thewebcomicfactory.com/)
  - [Kevin and Kell](https://www.kevinandkell.com/archive/)
  - [Questionable Content](https://www.questionablecontent.net/QCR/archive.php)
  - [Penny Arcade](https://www.penny-arcade.com/comic)
  - [Sinfest](https://www.sinfest.net)
