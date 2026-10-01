# ComicCacher TODO

## Run manually triggered batch jobs in the background

- A manual "Run now" holds the GraphQL request open for the whole job. `AbstractJobScheduler` calls `JobOperator.start`, which runs the job on the calling thread, so the job runs on the Tomcat thread serving the mutation. On dev on 2026-10-01, SourceCatalogJob run 466 (`source=ALL`) ran on `tomcat-handler-230` and its request `c417bc13` had no completion line minutes later. The 12:21 run did the same for 52 s
- Start-date detection takes 10 to 20 s per GoComics comic, so a full catalog run takes minutes. Long enough for the browser or a proxy to time out while the job keeps going, and each run ties up a Tomcat thread
- Launch manual triggers on an async executor (with `MdcTaskDecorator`, so the job's log lines keep `req=` and `user=`) and return the execution id at once. The UI already polls `recentBatchJobs` every 3 s, so it shows progress without waiting on the mutation
- Keep the per-job lock that stops two runs overlapping, and check that scheduled runs behave the same
- Priority: Very High

## Log the GraphQL operation name

- Every API request line says `op=anonymous` and `POST /graphql (anonymous)`. `GraphQlLoggingInterceptor` reads only `request.getOperationName()`, and comic-hub's `graphql-client.ts` sends `{query, variables}` without `operationName`. The operation names added in #415 never appear, so a slow or failing request can't be tied to a query without its request id
- Send `operationName` from `graphql-client.ts` (codegen's `TypedDocumentString` knows it), and have the interceptor fall back to the name of the document's single operation, so other clients get it too
- Add a test that a named query logs its name
- Priority: Very High

## Log slow GraphQL fields once per request

- `TimingInstrumentation` logs a `Slow GraphQL field` WARN for every field over the threshold. On dev on 2026-10-01 at 14:10:36, one 828 ms page load (`req=593f31dc`) logged 46 of them, one per `Comic.lastStrip`: 46 of the day's 54 warnings
- The fields run in parallel, so they all cross the threshold together and the lines repeat the same fact. The request line already names the slowest field (`slowest=Comic.lastStrip:806ms`)
- Log one WARN per request with the slowest field, how many fields went over and the threshold, and keep per-field detail at DEBUG
- Priority: Very High

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

## Teach the comiccacher-logs skill about jobs that are still running

- The skill treats a job with no exit code as failed. On 2026-10-01 it raised a critical "SourceCatalogJob failed 1 time" for a manual run (execution 466) that was `STARTED` and still running when the logs were fetched
- It also can't find that run's log. `batch-executions.json` has `log_file: null` until the job ends, so the issue pointed at `batch-logs/SourceCatalogJob/None`, although `SourceCatalogJob-20261001-d555ad9a.log` was already on disk and growing
- Record running jobs in `summary.json` (status `STARTED` with no end time) and leave them out of the failure count. Find their log by job name, date and start time when `log_file` is empty. Show them in the report's jobs table as running, with elapsed time and the last log line
- Raise an issue only when a run has gone on much longer than its usual duration, or when a `STARTED` run survives a container restart. Those runs are stuck or orphaned, not running
- Priority: High

## Check the operator role on the server for the operations pages

- `/metrics`, `/retrieval-status` and `/batch-jobs` are hidden from USER accounts only by the nav (`isOperator` in `sidebar.tsx`, `nav-rail.tsx`, `header.tsx`). A USER who types the URL gets the page, and only the API's rejection of its queries stops them
- Next's authentication and data-security guides put authorization checks in server code, next to the data, not in what the UI shows
- Move the three pages into a route group (e.g. `(dashboard)/(operations)/layout.tsx`) whose server layout calls `getSession()` and `isOperator()`, and calls `notFound()` otherwise (or `forbidden()`, which needs the experimental `authInterrupts` flag). `/sources` already does this in `sources/layout.tsx`; follow that pattern
- Confirm the API rejects each operations query and mutation for USER accounts too, and add a layout test for the USER case
- Priority: Medium-High. Unchanged: authorization belongs on the server, and confirming the API side is cheap

## Log server errors and show the error digest

- `error.tsx` and `global-error.tsx` only `console.error` in the browser. When a server render fails, the user sees a generic page and the `comics-ui` log has nothing to match it to
- Add `instrumentation.ts` with `onRequestError` to log server render and route handler errors with the path and route (Next's instrumentation guide), in the same one-line style as the API logs
- Show `error.digest` on the error pages ("Error reference: …") so a user report can be matched to the log line. Pairs with "Add timing metrics to diagnose slow page loads" and the comiccacher-logs frontend check
- Priority: Medium-High. Raised from Medium: small change, and the comiccacher-logs frontend check (High) has nothing to find without it

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
- Priority: Medium. Unchanged: do it before the server-fetch work below, so that work targets the new caching model once

## Fetch page data on the server instead of after hydration

- Seven of the nine signed-in pages are `'use client'` and fetch through TanStack Query after the JavaScript loads: `comics`, `comics/[id]`, `metrics`, `retrieval-status`, `batch-jobs`, `read` and `comics/[id]/read` (the dashboard home is a server page that renders only `DashboardClient`). A first load goes HTML with skeleton → JS → `/api/graphql` → backend, one step after another, and `loading.tsx` only covers `getSession()`
- This goes against "server components by default" in `comic-hub/CLAUDE.md`
- Fix without losing the client cache: make each `page.tsx` a server component that prefetches its queries with `getAuthenticatedClient()` into a `QueryClient` and wraps the existing client component in `<HydrationBoundary state={dehydrate(queryClient)}>` (TanStack's Next.js App Router pattern). Start with the reader (`comics/[id]/read`: strip image is the LCP) and the dashboard home
- Read `params` and `searchParams` from the page props (they're Promises in Next 16) instead of `useParams()` / `useSearchParams()` in the page. Where a client component still needs `useSearchParams()`, wrap it in `<Suspense>` as the `use-search-params` docs recommend (`comics/page.tsx` and `read/page.tsx` don't today)
- `proxy.ts` (#397) already refreshes an expired access token before the render, so server fetches get a usable token
- Priority: Medium. Unchanged, but after the Next.js 17 upgrade. Scope the first pass to the reader and the dashboard home

## Read the backend URL at runtime, not from a `NEXT_PUBLIC_` variable

- `GRAPHQL_ENDPOINT` in `lib/auth/constants.ts` is `process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT!`, but only server code uses it (route handlers, `getSession()`, the `/api/v1` rewrite in `next.config.ts`)
- `NEXT_PUBLIC_` values are fixed when `next build` runs and can be inlined into browser bundles, so the Dockerfile has to bake the backend host in as a build arg, and changing it means a rebuild. The `rewrites()` destination is fixed at build time as well
- Rename it to a server-only variable (e.g. `API_URL`) that's read at runtime, check that it's set when the server starts rather than using `!`, and replace the `/api/v1/*` rewrite with a route handler that streams the backend response, or document that it's fixed per image
- Update `.env.example`, the Dockerfile, `utils/dev-ui.sh` and the deploy scripts
- Priority: Low. Lowered from Medium: `build.sh` already builds per environment, so the baked-in URL costs nothing today

## Review the admin pages after the UI revamp

- The 2.5.0 UI revamp focused on the public pages (reader, auth, comics list). The admin pages (batch jobs, metrics, retrieval status, comic management) weren't reviewed
- Check them against the revamped design: layout, spacing, typography, dark mode, mobile width, empty and loading states
- Priority: Low. Lowered from Medium: only operators see these pages and they work. Fold it into the next change that touches them

## Share server lookups within a request

- Next's data-security guide wraps the current-user lookup in React `cache()` so every layout, page and `generateMetadata` in a render shares one call. `getSession()` (`lib/auth/session.ts`) and `comicTitle()` (`lib/comic-title.ts`) aren't wrapped
- `DashboardClient` runs `useGetMeQuery()` only for `displayName`, which the server layout already put in `UserContext`. Use `useUser()` instead and drop the extra request
- Once pages prefetch on the server (above), `comicTitle()` and the page both fetch `GetComic`; put them behind one `cache()`d `getComic(id)`
- Priority: Low

## Tighten security headers and keep the app out of search engines

- `next.config.ts` sends `X-XSS-Protection: 1; mode=block`, which browsers no longer support and which current guidance says to drop (or set to `0`). There's no `Content-Security-Policy`
- Add a CSP following Next's Content Security Policy guide. The inline theme script needs a nonce (set in the existing `proxy.ts`, whose matcher already covers every page) or a hash
- Nothing tells crawlers to skip this private app: add `robots: { index: false, follow: false }` to the root `metadata`, or an `app/robots.ts` that disallows everything
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

## Advertise zstd to gocomics like real Chrome

- The gocomics 429s that started 2026-09-22 look resolved: the 2026-09-28 07:30 run on 2.5.0 (Chrome 154 User-Agent, 429 retries) got no 429s at all, so the retries weren't needed, and every gocomics comic but Shoe (no Open Graph image) downloaded
- 429s now have their own `RATE_LIMITED` retrieval status, so a return shows up on the retrieval-status page
- Optional hardening: send and decode `zstd` in `Accept-Encoding` as Chrome does (needs a pure-Java decoder such as `io.airlift:aircompressor` 2.x)
- Priority: Very-Low

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

### Respect robots.txt

- Check and honor `robots.txt` rules from GoComics and ComicsKingdom before scraping
- Good-citizen behavior that aligns with the copyright notice in the README
- dosage implements this — set a `User-Agent` and respect disallow rules
- Priority: Medium. Unchanged: decide first what to do if a source disallows a path we need

### Edit Source Settings on the Sources Page

- The Sources page shows each source's throttle, 429 retry and backfill settings read-only; they live in `application.properties` and need a redeploy to change
- Keep overrides in a `sources.json` in the cache root on top of the properties, and have `SourceThrottleService` and `BackfillConfigurationService` read the effective values on every call, so a change applies at once. Show which values are overridden
- Also a per-source on/off switch (backfill already has `batch.comic-backfill.sources.<id>.enabled`)
- Priority: Low

### Promote Comics from Dev to Prod

- Add a job that "promotes" strips the dev instance already downloaded into the prod instance's storage, so prod doesn't have to download them a second time
- Two sweep modes:
  - **Last 7 days**: the default, suited to a recurring run
  - **All-time**: a one-off full sweep across every date dev has
- Only copy strips prod is missing. Never overwrite existing prod files
- Bring the related metadata along (sidecar JSON, image hashes, date indexes) so duplicate detection and indexes stay consistent. Use atomic writes (see `docs/storage/overview.md`)
- Open questions: how files move (shared NFS mount, API pull, or scp over ssh), which instance runs the job, and whether it can be scoped per comic
- Priority: Low. Lowered from Medium: the gocomics 429s are resolved, so saving the second download matters less, and writing into prod storage from dev needs its open questions settled first

### Download Failure Notifications

- Alert when a comic hasn't had a new strip on disk for N days
- Base it on the files, not the retrieval status: the Mother Goose & Grimm bug went unnoticed for eight months because the status said `SUCCESS`
- Could be webhook, email, or in-app notification
- Priority: Low

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
