# ComicCacher TODO

## Forgot Password Flow

- Wire up `requestPasswordReset` mutation in `comic-hub/src/app/(auth)/forgot-password/page.tsx`
- Currently the form submission is a no-op that immediately shows the success view
- Priority: Medium

## Configure SMTP for Password Reset

- Mail is disabled by default (`spring.mail.host` is empty, `management.health.mail.enabled=false`)
- To enable in production, set the following environment variables:
  - `MAIL_HOST` — SMTP server hostname (e.g., `smtp.gmail.com`)
  - `MAIL_PORT` — SMTP port (default: 587)
  - `MAIL_USERNAME` — SMTP auth username
  - `MAIL_PASSWORD` — SMTP auth password
  - `MAIL_FROM` — sender address (default: `noreply@comiccacher.local`)
  - `MAIL_RESET_URL_BASE` — password reset page URL (default: `http://localhost:3000/reset-password`)
- Once SMTP is configured, re-enable the health indicator: `management.health.mail.enabled=true`
- Config file: `comic-api/src/main/resources/application.properties`
- Priority: Medium (blocked until Forgot Password Flow is wired up)

## Performance Improvements

### API Response Caching

- `Cache-Control: max-age` is already set on the image endpoints (`ComicController`: avatar 1 day, strip 7 days)
- Remaining: add `ETag` / `Last-Modified` so clients can revalidate cheaply once max-age expires
- Target: Comic image endpoints (/api/v1/comics/{id}/avatar, /api/v1/comics/{id}/strip/\*)
- Priority: Medium

### Enable Gradle Configuration Cache

- Consider enabling the Gradle configuration cache to speed up builds
- Reference: https://docs.gradle.org/9.4.0/userguide/configuration_cache_enabling.html
- Priority: Low

### Revisit OpenAPI/Swagger Generation

- With the move to GraphQL, only 2 REST endpoints remain (binary image streaming)
- Evaluate whether the openapi-gradle-plugin, `generate-openapi-docs.sh`, and `openapi.json` are still worth maintaining
- If not needed, remove the springdoc dependency, openApi task config, and related tasks from comic-api/build.gradle
- Priority: Medium

### Upgrade to Java 25

- Upgrade from Java 21 to Java 25 when available
- Update `build.gradle` Java toolchain/sourceCompatibility settings
- Update CI/CD pipeline and Docker base images
- Clean up deprecated API usage first (see below)
- Priority: Low

### Clean Up Deprecated Java APIs

- **Jsoup `.first()`/`.last()` → `.selectFirst()` / stream-based** — 7 instances across `GoComics`, `GoComicsDownloaderStrategy`, `ComicsKingdom`, `ComicsKingdomDownloaderStrategy` in comic-engine
- **Guava `@VisibleForTesting` → remove or replace** — 3 instances (`RetrievalStatusRepository`, `JsonRetrievalStatusRepository`, `JsonErrorTrackingRepository`)
- **Guava `Files.getNameWithoutExtension()` → plain Java** — 2 instances (`ImageUtils`, `FileSystemComicStorageFacade`)
- Priority: Medium (do before Java 25 upgrade)

### Consolidate Root JSON Files into a Data Folder

- Move the loose JSON files in the project root into a single `data/` folder
- Update all code references to the new paths
- Priority: Medium

### Replace the Selenium GoComics IT and Remove Selenium

- `GoComicsIntegrationIT` tests the wrong code: it drives the legacy Selenium `GoComics` class, which production has never used. Prod has downloaded through the Jsoup `GoComicsDownloaderStrategy` since 2025-05, and the prod image has no Chrome
- So the IT can pass while prod is broken (it would have missed the Brotli bug fixed in #270) and fail while prod is fine (`downloadAdamAtHomeFiveDaysAgo` fails on master today)
- History: Selenium was added to the already-unused legacy class in `b981c4d` ("Cleanup", 2025-10-21) and never wired into the strategy. Prod data shows Jsoup gets past bot detection (92–95% success in 2026-08/09; failures are 429 rate limits, which a browser would hit too)
- Steps:
  1. Rewrite `GoComicsIntegrationIT` to exercise `GoComicsDownloaderStrategy` against the live site, paced through `SourceThrottleService` and with a small number of fetches
  2. Delete the legacy `GoComics` class (and whatever in `DailyComic`/`IDailyComic` only it needs), and drop `selenium-java` / `webdrivermanager` from `comic-api/build.gradle` and `comic-engine/build.gradle`
  3. Delete `comic-common/.../infrastructure/web/DefaultTrustManager.java`, which nothing references
  4. Update the "Legacy downloaders" notes in `docs/design/architecture.md` and `docs/design/download-pipeline.md`
- Benefits: the IT validates the real prod path, and the jar/image lose dependencies nothing uses. This also replaces the old "reduce WebDriver startup overhead in the IT" item, since there'd be no WebDriver left
- Priority: Medium

## Feature Ideas

### CBZ/PDF Export

- Export a date range of strips as CBZ or PDF for offline reading
- Natural extension of existing image storage — images are already on disk
- Universal feature across comic downloaders (dosage, comic-dl, mangal, comics-downloader)
- Priority: Medium

### OPDS Feed

- Serve comics via the OPDS protocol for external reader apps (Panels, Chunky, KOReader)
- Opens the collection to a large ecosystem of existing reader apps
- Kavita and Komga both support this
- Priority: Low

### Respect robots.txt

- Check and honor `robots.txt` rules from GoComics and ComicsKingdom before scraping
- Good-citizen behavior that aligns with the copyright notice in the README
- dosage implements this — set a `User-Agent` and respect disallow rules
- Priority: Medium

### Download Failure Notifications

- Alert when a comic fails to download for N consecutive days
- Could be webhook, email, or in-app notification
- Priority: Low

### Configurable Scraping Rate Limits

- Per-source request delay (`downloader.sources.<source>.throttle.*`) and 429 retry/backoff (`downloader.sources.<source>.retry.*`) are already configurable in `application.properties`
- Remaining: expose them at runtime (e.g. in the Sources Configuration Screen) instead of requiring a redeploy
- Priority: Low

### Sources Configuration Screen

- Admin UI to add/remove comics and configure source-specific settings (e.g., scraping frequency, date range)
- Name of Source, Enabled/Disabled toggle, # of configured comics from source (if Applicable)
- For Example:
  - GoComics
    - Fetch list from https://www.gocomics.com/comics/a-to-z
    - Max days back to fetch: 14 (days)
    - I've got 8 of 400 comics configured
      - Add a button to force re-fetching the list of available comics from the source
      - Add a button to Run Comics-Backfil on an individual comic or source
  - ComicsKingdom
    - Fetch list from https://www.comicskingdom.com/guide
    - Max days back to fetch: 30 (days)
    - I've got 5 of 200 comics configured
- Priority: High

### Promote Comics from Dev to Prod

- Add a job that "promotes" strips the dev instance already downloaded into the prod instance's storage, so prod doesn't have to download them a second time
- Two sweep modes:
  - **Last 7 days**: the default, suited to a recurring run
  - **All-time**: a one-off full sweep across every date dev has
- Only copy strips prod is missing. Never overwrite existing prod files
- Bring the related metadata along (comic JSON, image hashes, analysis results) so duplicate detection and indexes stay consistent. Use atomic writes (see `docs/storage/overview.md`)
- Open questions: how files move (shared NFS mount, API pull, or rsync over the Docker context), which instance runs the job, and whether it can be scoped per comic
- Priority: Medium

# Additional Source Ideas

- **XKCD** — https://xkcd.com/archive/
  - Indexed
- **The Web Comic Factory** — http://www.thewebcomicfactory.com/
- **Kevin and Kell** — https://www.kevinandkell.com/archive/
- **Questionable Content** — https://www.questionablecontent.net/QCR/archive.php
- **Penny Arcade** — https://www.penny-arcade.com/comic
- **Sinfest** — https://www.sinfest.net
- Priority: Medium

## Fix strips silently not saved for Mother Goose & Grimm and Sherman's Lagoon

- Prod has recorded these as `SUCCESS` in `retrieval-status.json` every day through 2026-09-21 (20+ distinct images, distinct sizes), but the newest strip on disk is `2026-01-09.png` for both. No 2026 strips after that exist anywhere under `/comics`
- Other gocomics comics save fine (e.g. Pickles through 2026-09-21), and `image-hashes.json` in `MotherGoose&Grimm/2026/` was still being updated on 2026-09-21, so the download and hash steps run but the image file never lands
- Suspect: these are the only comics whose names have special characters (`&`, `'`), so the save path probably mishandles them. Unconfirmed; start in `FileSystemComicStorageFacade.saveComicStripWithResult()`
- Also: the download is recorded as a success even though nothing was written. Make a failed save show up as a failure
- Related: 429 failures are recorded as `COMIC_UNAVAILABLE`, which hides them among real "no strip today" cases. Give them their own status (e.g. `RATE_LIMITED`)
- After the fix, backfill 2026-01-10 onward for both comics
- Priority: High (silent data loss since January)

## Fix comic mutations dropping fields

- `updateComic` accepts `publicationDays` and `active` in `UpdateComicInput`, but the resolver never copies them, so the change is silently ignored (`comic-api/src/main/java/org/stapledon/api/resolver/ComicResolver.java`, `updateComic`)
- `createComic` also ignores `publicationDays` and `active` from `CreateComicInput`
- Neither input can set `firstStripNumber` / `lastStripNumber`, so indexed comics (Freefall) can't be created through the API
- Impact: prod config changes (e.g. FoxTrot → Sunday only, adding Freefall, 2026-09-24) had to be made by editing `comics.json` with the API stopped
- Add resolver tests that each input field reaches the saved `ComicItem`
- Priority: Medium

## Verify the gocomics 429 fix in prod

- Since 2026-09-22 six gocomics comics have failed every day with HTTP 429: Frank-And-Ernest, Luann, Mother Goose & Grimm, Pickles, ScaryGary, Sherman's Lagoon
- `fix/gocomics-429` added 429 retries with source-wide backoff (`downloader.sources.gocomics.retry.*`) and moved the User-Agent to Chrome 154 with matching client hints
- Prod data says this is rate limiting, not per-comic blocking: on 2026-09-22/23, 15 comics got 429 (Boondocks, Ziggy, WizardOfId and others), and most succeeded again on 09-24
- After deploy, check the `ComicDownloadJob` logs (prod runs it at 07:30, `BATCH_COMICDOWNLOAD_CRON`) for `Rate limited (HTTP 429)` warnings and see whether the retries succeed. If they don't, lower the request rate further (`downloader.sources.gocomics.throttle.*`) or spread the run out
- Optional: advertise and decode `zstd` like real Chrome (needs a pure-Java decoder such as `io.airlift:aircompressor` 2.x)
- Priority: High
