# ComicCacher TODO

## Teach the comiccacher-logs skill which jobs are paused or disabled

- Many dev jobs are paused on purpose because they're no longer being tested. The skill reports them as idle or overdue, which leads to wrong findings (on 2026-09-28 it flagged ComicBackfillJob as not having run since 09-25, but the job is paused on dev)
- The data is already fetched: `state/scheduler-state.json` has `paused`, `lastToggled` and `toggledBy` per job. Jobs switched off with `batch.<name>.enabled=false` log no `INITIALIZING SCHEDULER` line
- Record paused and disabled jobs in `summary.json`, skip the idle and overdue checks for them, list them in one observation, and badge them in the report's jobs table. With `both`, a job paused in one environment only is a difference to note, not drift to fix
- Raise an issue when a batch log shows `trigger=STARTUP_MAKEUP` for a paused job. The startup catch-up skips paused jobs now, so this would be a regression
- Priority: High

## Teach the comiccacher-logs skill about frontend health and unexpected log lines

- The skill (in `~/git/runbooks/skills/comiccacher-logs`) focuses on the API. It treats the `comics-ui` log as relevant only to web-UI questions, so a health report doesn't check the frontend
- Add a frontend health check to every report: container status and restarts, `/api/health`, and errors in the `comics-ui` log (failed server renders, GraphQL errors, refresh failures)
- Use the request timing lines: count `Slow request:` / `Slow GraphQL field` / `Slow storage read` WARNs in both logs, and join comics-ui and comics-api lines on `req=`
- Flag anything unexpected: log lines that match none of the known signatures, new WARN/ERROR messages, and error rates that jump compared with earlier runs, rather than reporting only the failures it already knows how to look for
- Priority: High

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

## Stop batch times depending on the JVM's timezone

- Spring Batch records job and step times as `LocalDateTime` in the JVM's default zone. `DateTimeUtils.toOffset` (used by `JsonBatchExecutionTracker` and `BatchJobResolver`) labels them with the `batch.timezone` offset, which is right only when the JVM also runs in `batch.timezone`
- Prod logs show `-04:00`, so the prod JVM runs on Toronto time today, but neither `comic-api/Dockerfile` nor `utils/prod/docker-compose.yml` sets `TZ`. On a UTC JVM, batch history times (UI and `batch-executions.json`) would be off by 4–5 hours
- Fix: read the `LocalDateTime` in `ZoneId.systemDefault()` and convert it to `batch.timezone`, or set `TZ` in the image and compose file. `hasJobRunToday` already compares in `batch.timezone`
- Priority: Medium

## Get ready for Next.js 17

- Next.js 17 was released 2026-06-15; comic-hub is on `next` 16.3.6 with React 19.3.0
- What matters for comic-hub:
  - **React 19 required:** already met, no change needed
  - **Turbopack only, Webpack defaults removed:** `next.config.ts` has no `webpack` hook, so the build should carry over. Check that Vitest and the `standalone` output still work in the Docker image
  - **New caching system:** we opt out of caching today (`cache: 'no-store'` in `getSession()`, `force-static` on `/api/health`). Check that both behave the same under the new model, and that authenticated pages and GraphQL responses are never cached across users
  - **Server Actions changes:** we don't use Server Actions (mutations go through `/api/graphql`), so nothing to migrate. Leave any move to Server Actions as a separate decision
  - **Partial hydration:** new and optional. Worth a look later for the reader page, not part of the upgrade
- Before upgrading: read the official upgrade guide and run the codemod (`npx @next/codemod upgrade`), bump `eslint-config-next` with `next`. CI now runs lint, codegen and `tsc`, so a broken bump fails there
- Verify with `npm run build`, `npm test`, `npm run lint` and a dev deploy before prod
- Priority: Medium

## Check the operator role on the server for the operations pages

- `/metrics`, `/retrieval-status` and `/batch-jobs` are hidden from USER accounts only by the nav (`isOperator` in `sidebar.tsx`, `nav-rail.tsx`, `header.tsx`). A USER who types the URL gets the page, and only the API's rejection of its queries stops them
- Next's authentication and data-security guides put authorization checks in server code, next to the data, not in what the UI shows
- Move the three pages into a route group (e.g. `(dashboard)/(operations)/layout.tsx`) whose server layout calls `getSession()` and `isOperator()`, and calls `notFound()` otherwise (or `forbidden()`, which needs the experimental `authInterrupts` flag)
- Confirm the API rejects each operations query and mutation for USER accounts too, and add a layout test for the USER case
- Priority: Medium-High

## Fetch page data on the server instead of after hydration

- Seven of the nine signed-in pages are `'use client'` and fetch through TanStack Query after the JavaScript loads: `comics`, `comics/[id]`, `metrics`, `retrieval-status`, `batch-jobs`, `read` and `comics/[id]/read` (the dashboard home is a server page that renders only `DashboardClient`). A first load goes HTML with skeleton → JS → `/api/graphql` → backend, one step after another, and `loading.tsx` only covers `getSession()`
- This goes against "server components by default" in `comic-hub/CLAUDE.md`
- Fix without losing the client cache: make each `page.tsx` a server component that prefetches its queries with `getAuthenticatedClient()` into a `QueryClient` and wraps the existing client component in `<HydrationBoundary state={dehydrate(queryClient)}>` (TanStack's Next.js App Router pattern). Start with the reader (`comics/[id]/read`: strip image is the LCP) and the dashboard home
- Read `params` and `searchParams` from the page props (they're Promises in Next 16) instead of `useParams()` / `useSearchParams()` in the page. Where a client component still needs `useSearchParams()`, wrap it in `<Suspense>` as the `use-search-params` docs recommend (`comics/page.tsx` and `read/page.tsx` don't today)
- `proxy.ts` (#397) already refreshes an expired access token before the render, so server fetches get a usable token
- Priority: Medium

## Share server lookups within a request

- Next's data-security guide wraps the current-user lookup in React `cache()` so every layout, page and `generateMetadata` in a render shares one call. `getSession()` (`lib/auth/session.ts`) and `comicTitle()` (`lib/comic-title.ts`) aren't wrapped
- `DashboardClient` runs `useGetMeQuery()` only for `displayName`, which the server layout already put in `UserContext`. Use `useUser()` instead and drop the extra request
- Once pages prefetch on the server (above), `comicTitle()` and the page both fetch `GetComic`; put them behind one `cache()`d `getComic(id)`
- Priority: Low

## Read the backend URL at runtime, not from a `NEXT_PUBLIC_` variable

- `GRAPHQL_ENDPOINT` in `lib/auth/constants.ts` is `process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT!`, but only server code uses it (route handlers, `getSession()`, the `/api/v1` rewrite in `next.config.ts`)
- `NEXT_PUBLIC_` values are fixed when `next build` runs and can be inlined into browser bundles, so the Dockerfile has to bake the backend host in as a build arg, and changing it means a rebuild. The `rewrites()` destination is fixed at build time as well
- Rename it to a server-only variable (e.g. `API_URL`) that's read at runtime, check that it's set when the server starts rather than using `!`, and replace the `/api/v1/*` rewrite with a route handler that streams the backend response, or document that it's fixed per image
- Update `.env.example`, the Dockerfile, `utils/dev-ui.sh` and the deploy scripts
- Priority: Medium

## Log server errors and show the error digest

- `error.tsx` and `global-error.tsx` only `console.error` in the browser. When a server render fails, the user sees a generic page and the `comics-ui` log has nothing to match it to
- Add `instrumentation.ts` with `onRequestError` to log server render and route handler errors with the path and route (Next's instrumentation guide), in the same one-line style as the API logs
- Show `error.digest` on the error pages ("Error reference: …") so a user report can be matched to the log line. Pairs with "Add timing metrics to diagnose slow page loads" and the comiccacher-logs frontend check
- Priority: Medium

## Tighten security headers and keep the app out of search engines

- `next.config.ts` sends `X-XSS-Protection: 1; mode=block`, which browsers no longer support and which current guidance says to drop (or set to `0`). There's no `Content-Security-Policy`
- Add a CSP following Next's Content Security Policy guide. The inline theme script needs a nonce (set in the existing `proxy.ts`, whose matcher already covers every page) or a hash
- Nothing tells crawlers to skip this private app: add `robots: { index: false, follow: false }` to the root `metadata`, or an `app/robots.ts` that disallows everything
- Priority: Low

## Advertise zstd to gocomics like real Chrome

- The gocomics 429s that started 2026-09-22 look resolved: the 2026-09-28 07:30 run on 2.5.0 (Chrome 154 User-Agent, 429 retries) got no 429s at all, so the retries weren't needed, and every gocomics comic but Shoe (no Open Graph image) downloaded
- 429s now have their own `RATE_LIMITED` retrieval status, so a return shows up on the retrieval-status page
- Optional hardening: send and decode `zstd` in `Accept-Encoding` as Chrome does (needs a pure-Java decoder such as `io.airlift:aircompressor` 2.x)
- Priority: Very-Low

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

### Move the Cache-Root JSON Files into a Data Folder

- The cache root (`/comics`) has about nine loose JSON files: `comics.json`, `users.json`, `retrieval-status.json` and so on (see `docs/storage/overview.md`)
- Move them into a `data/` folder, update the code paths, and migrate the existing prod and dev storage
- Priority: Low

### Add Live-Site Downloader ITs

- The legacy Selenium `GoComics` and Jsoup `ComicsKingdom` classes and their live-site ITs are gone; nothing now checks the production strategies against the real sites
- Add ITs that run `GoComicsDownloaderStrategy` and `ComicsKingdomDownloaderStrategy` against the live sites, paced through `SourceThrottleService` with a handful of fetches, and keep them out of the default CI run so a site change doesn't block merges
- Priority: Very-Low

## Feature Ideas

### Sources Configuration Screen

- Admin UI to add/remove comics and configure source-specific settings (e.g., scraping frequency, date range)
- Name of Source, Enabled/Disabled toggle, # of configured comics from source (if Applicable)
- Should also cover the per-source throttle and retry settings, which today live in `application.properties` and need a redeploy to change
- For Example:
  - GoComics
    - Fetch list from https://www.gocomics.com/comics/a-to-z
    - Max days back to fetch: 14 (days) (With optional auto-detect)
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
- Priority: Low

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
- Priority: High
