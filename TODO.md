# ComicCacher TODO

## Refresh expired sessions on page load

- The access token lasts 15 minutes (`jwt.expiration=900000`). Once it expires, any full page load sends the user to `/login`, even with a valid 24-hour refresh token
- `getSession()` in `comic-hub/src/lib/auth/session.ts` only sends the access cookie to `me` and returns null when it's rejected. The `(dashboard)` and `(reader)` layouts then `redirect('/login')`. Only `/api/graphql` refreshes on 401, so client requests recover but server renders don't
- Found while testing on dev 2026-09-25: reloading the reader after 15 minutes landed on the login page
- Fix: when `me` is rejected and a refresh cookie exists, refresh in `getSession()` (reuse the `/api/graphql` refresh logic) and set the new cookies. Server components can't set cookies, so this likely belongs in a new `proxy.ts` (Next middleware; the no-op one was removed in 2.5.0) or a route handler. Honour the remember-me cookie as the refresh path does, and add tests for expired-access / valid-refresh
- Priority: High

## Fix startup catch-up jobs blocking readiness

- `StartupJobRunner` runs any daily job that missed its time today on the main thread, inside the `ApplicationReadyEvent` listener
- Readiness isn't reported until those jobs finish, so `/actuator/health` returns 503 `OUT_OF_SERVICE` the whole time. On dev (2.4.7 deploy) a gocomics backfill kept it unhealthy for many minutes
- This matters for prod: `prod-run.sh` rolls back if the container isn't healthy within 180s, so a restart on a day a job hasn't run yet will roll the deploy back
- ComicBackfillJob no longer has a catch-up run (it runs every 2 h), so the slow gocomics case is gone; the other daily jobs still run inline
- Run the catch-up jobs in the background (e.g. on the batch `TaskExecutor`), keeping their order, and add a test that the listener returns straight away
- Also check whether `hasJobRunToday` uses UTC or `batch.timezone` for "today"
- Priority: High

## Verify the gocomics 429 fix in prod

- Since 2026-09-22 about six gocomics comics have failed every day with HTTP 429. It's rate limiting across the whole source, not per-comic blocking
- 2.4.7 added 429 retries with backoff (`downloader.sources.gocomics.retry.*`) and moved the User-Agent to Chrome 154
- Check the next 07:30 `ComicDownloadJob` run for `Rate limited (HTTP 429)` warnings and whether the retries succeed. If they don't, lower `downloader.sources.gocomics.throttle.*` or spread the run out
- 429s are recorded as `COMIC_UNAVAILABLE`, which hides them among real "no strip today" days. Give them their own status (e.g. `RATE_LIMITED`)
- Optional: advertise and decode `zstd` like real Chrome (needs a pure-Java decoder such as `io.airlift:aircompressor` 2.x)
- Priority: High

## Backfill Mother Goose & Grimm and Sherman's Lagoon

- The save bug is fixed (2.4.8), and the 139 strips per comic that dev had were copied to prod
- Still missing: 2026-01-10 to about 2026-02-10, plus any other days dev didn't have
- Run a backfill from 2026-01-10 for both comics once 2.4.8 is on prod. Delete any `comic_*` folders the old version recreated in the cache root first
- Priority: High

## Dependency upgrades held back from 2.5.0

Dependabot opened these on 2026-09-27; each passed CI (or failed it) without showing the real problem, because CI neither regenerates GraphQL code nor runs ESLint.
- **`@graphql-codegen/typescript-operations` 6 (#387):** the regenerated `src/generated/graphql.ts` no longer compiles. v6 also emits the input and enum types the `typescript` plugin generates (duplicate identifiers), and no longer types custom scalars as `any` (`Date` fields became `{}`). Migrate `codegen.yml`: drop or reconfigure the `typescript` plugin, and map `scalars` (`Date`/`DateTime` to `string`, `JSON` to `any`), then fix the call sites that relied on `any`
- **TypeScript 7 (#391):** typescript-eslint doesn't support TS 7 yet, so `npm run lint` crashes. Wait for typescript-eslint support (tracked in typescript-eslint#10940) or run TS 6 side by side for linting
- **`@types/node` 26 (#389):** fails the build and would describe Node 26 while we run Node 24 LTS. Take it with the move to Node 26 once it becomes LTS (late October 2026), and ignore `@types/node` majors in `dependabot.yml` until then
- Also: add `npm run codegen` (with a check that `src/generated` is unchanged) and `npm run lint` to the comic-hub CI job, so the next bump like these fails in CI
- Priority: High

## Add timing metrics to diagnose slow page loads

- Slow page loads are hard to diagnose because the logs don't show where the time goes
- The API already logs the total time for each request (`RequestLoggingFilter`, `-> 200 in 123ms`), but nothing breaks that time down, and comic-hub logs no timings at all
- Add timings throughout the application:
  - **comic-hub:** server render time per page, and the time for each GraphQL call made during the render (tagged with `X-Request-Id` so it can be matched to the API line)
  - **comic-api:** time per GraphQL resolver / data fetcher, and slow storage reads (NFS JSON files, strip images)
  - Log a WARN line when a request or render crosses a threshold, so slow loads stand out without turning on DEBUG
- Priority: Medium-High

## Review the admin pages after the UI revamp

- The 2.5.0 UI revamp focused on the public pages (reader, auth, comics list). The admin pages (batch jobs, metrics, retrieval status, comic management) weren't reviewed
- Check them against the revamped design: layout, spacing, typography, dark mode, mobile width, empty and loading states
- Priority: Medium

## Fix comic mutations dropping fields

- `updateComic` and `createComic` in `ComicResolver` ignore `publicationDays` and `active` from their inputs, so changes to them are silently lost
- Neither input can set `firstStripNumber` / `lastStripNumber`, so indexed comics (Freefall) can't be created through the API
- For now, prod config changes mean stopping the API and editing `comics.json` by hand
- Add resolver tests that each input field reaches the saved `ComicItem`
- Priority: Medium

## Fix prod deploy script quirks

- `current_ref()` in `utils/prod-run.sh` takes the tag from after the last `:` of the image name. For a digest-pinned image that's the digest, so the plan output and audit log show the wrong version. Rollback still works. Fix: strip `@digest` first
- In `prod-build-and-run.sh` the staging `ssh`/`scp` calls eat stdin, so `echo y | …` never reaches the `Continue?` prompt and the script exits without saying why. Use `ssh -n` there, and print a message when `read` gets no input
- Priority: Low

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
- The forgot-password and reset-password pages are wired up; without SMTP the reset email is never sent
- Priority: Low

## Performance Improvements

### API Response Caching

- `Cache-Control: max-age` is already set on the image endpoints (`ComicController`: avatar 1 day, strip 7 days)
- Remaining: add `ETag` / `Last-Modified` so clients can revalidate cheaply once max-age expires
- Target: Comic image endpoints (/api/v1/comics/{id}/avatar, /api/v1/comics/{id}/strip/\*)
- Priority: Low

### Enable Gradle Configuration Cache

- Consider enabling the Gradle configuration cache to speed up builds
- Reference: https://docs.gradle.org/current/userguide/configuration_cache_enabling.html
- Priority: Low

### Clean Up Deprecated Java APIs

- **Jsoup `.first()`/`.last()` → `.selectFirst()` / stream-based** — in `GoComicsDownloaderStrategy` and `ComicsKingdomDownloaderStrategy` in comic-engine
- **Guava `@VisibleForTesting` → remove or replace** — 3 instances (`RetrievalStatusRepository`, `JsonRetrievalStatusRepository`, `JsonErrorTrackingRepository`)
- **Guava `Files.getNameWithoutExtension()` → plain Java** — 2 instances (`ImageUtils`, `FileSystemComicStorageFacade`)
- Priority: Medium

### Move the Cache-Root JSON Files into a Data Folder

- The cache root (`/comics`) has about nine loose JSON files: `comics.json`, `users.json`, `retrieval-status.json` and so on (see `docs/storage/overview.md`)
- Move them into a `data/` folder, update the code paths, and migrate the existing prod and dev storage
- Priority: Low

### Add Live-Site Downloader ITs

- The legacy Selenium `GoComics` and Jsoup `ComicsKingdom` classes and their live-site ITs are gone; nothing now checks the production strategies against the real sites
- Add ITs that run `GoComicsDownloaderStrategy` and `ComicsKingdomDownloaderStrategy` against the live sites, paced through `SourceThrottleService` with a handful of fetches, and keep them out of the default CI run so a site change doesn't block merges
- Priority: Medium

## Feature Ideas

### Sources Configuration Screen

- Admin UI to add/remove comics and configure source-specific settings (e.g., scraping frequency, date range)
- Name of Source, Enabled/Disabled toggle, # of configured comics from source (if Applicable)
- Should also cover the per-source throttle and retry settings, which today live in `application.properties` and need a redeploy to change
- For Example:
  - GoComics
    - Fetch list from https://www.gocomics.com/comics/a-to-z
    - Max days back to fetch: 14 (days)
    - I've got 8 of 400 comics configured
      - Add a button to force re-fetching the list of available comics from the source
      - Add a button to Run Comics-Backfill on an individual comic or source
  - ComicsKingdom
    - Fetch list from https://www.comicskingdom.com/guide
    - Max days back to fetch: 30 (days)
    - I've got 5 of 200 comics configured
- Priority: Medium

### Fetch All Comics from a Source and Toggle Them On/Off

- Today the comic configuration (`comics.json`) is hand-coded: adding a comic means knowing its source URL and editing the file
- Fetch the full list of comics each source offers (e.g. GoComics A–Z, ComicsKingdom guide), store it, and let an admin turn any comic on or off with a single toggle
- Turning a comic on should create its configuration with sensible defaults; turning it off should stop downloads without deleting its stored strips
- Overlaps with the Sources Configuration Screen above; this is the smaller first step
- Priority: Low

### Download Failure Notifications

- Alert when a comic hasn't had a new strip on disk for N days
- Base it on the files, not the retrieval status: the Mother Goose & Grimm bug went unnoticed for eight months because the status said `SUCCESS`
- Could be webhook, email, or in-app notification
- Priority: Medium

### Promote Comics from Dev to Prod

- Add a job that "promotes" strips the dev instance already downloaded into the prod instance's storage, so prod doesn't have to download them a second time
- Two sweep modes:
  - **Last 7 days**: the default, suited to a recurring run
  - **All-time**: a one-off full sweep across every date dev has
- Only copy strips prod is missing. Never overwrite existing prod files
- Bring the related metadata along (sidecar JSON, image hashes, date indexes) so duplicate detection and indexes stay consistent. Use atomic writes (see `docs/storage/overview.md`)
- Open questions: how files move (shared NFS mount, API pull, or scp over ssh), which instance runs the job, and whether it can be scoped per comic
- Priority: Medium

### Respect robots.txt

- Check and honor `robots.txt` rules from GoComics and ComicsKingdom before scraping
- Good-citizen behavior that aligns with the copyright notice in the README
- dosage implements this — set a `User-Agent` and respect disallow rules
- Priority: Medium

### CBZ/PDF Export

- Export a date range of strips as CBZ or PDF for offline reading
- Natural extension of existing image storage — images are already on disk
- Priority: Low

### OPDS Feed

- Serve comics via the OPDS protocol for external reader apps (Panels, Chunky, KOReader)
- Kavita and Komga both support this
- Priority: Low

# Additional Source Ideas

- **XKCD** — https://xkcd.com/archive/
  - Indexed
- **The Web Comic Factory** — http://www.thewebcomicfactory.com/
- **Kevin and Kell** — https://www.kevinandkell.com/archive/
- **Questionable Content** — https://www.questionablecontent.net/QCR/archive.php
- **Penny Arcade** — https://www.penny-arcade.com/comic
- **Sinfest** — https://www.sinfest.net
- Priority: Low
