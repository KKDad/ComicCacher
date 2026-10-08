# ComicCacher TODO

## Flow the batch-job cards into the gap when one is expanded

- On `/batch-jobs`, expanding a card leaves empty space beside it, because the cards sit in a grid (`grid-cols-[repeat(auto-fill,…)] items-start`) whose rows are as tall as their tallest card
- Let the cards in the other column move up into that space, e.g. a masonry-style layout (CSS columns, or one flex column per grid column)
- Priority: Medium-High

## Retry a strip from the retrieval-status page

- The retrieval-status page shows which strips are missing and why, but fixing one means waiting for the next run or a backfill
- Add an ADMIN mutation `retryComicRetrieval(comicId, date)`, queued like `fetchComicAvatar`, that calls `ComicManagementFacade.downloadComicForDate` and the usual save path, and a retry button on missing cells and in the comic's drawer
- Priority: Medium. Deferred from the retrieval-status rework

## Keep attempt history in retrieval records

- `retrieval-status.json` keeps one record per comic and date, and a retry replaces it, so a strip that took three tries looks like one clean success. The retrieval-status grid can only mark a strip "recovered" while its record still says it failed
- Add an `attempts` count and the first failure (status, time) to `ComicRetrievalRecord`, kept when a later attempt replaces the record, and show them in the drawer
- Priority: Low. Deferred from the retrieval-status rework

## Replace hash-derived comic ids

- Older comics took their id from Java's `name.hashCode()` when they were bootstrapped, so ids are large and about half are negative (Drabble is −717937236, Garfield −1559823038). Comics added since get the highest id + 1 (`ComicManagementFacade.createComic`)
- What it has cost so far: the retrieval-status page's `comic=` URL parameter rejected negative ids, so those comics couldn't be selected (fixed in #455). Anything else that parses an id as a positive number has the same bug
- What else to fix:
  - highest id + 1 overflows past `Integer.MAX_VALUE` when the largest hash is close to it
  - the hashes are tied to names, which can be renamed
  - two names can collide (rare)
- Where ids live:
  - `comics.json`
  - users' favourites and last-read in the preferences
  - `retrieval-status.json` record ids (`{comicId}_{date}`)
  - the date index cache
  - metrics, which are joined by directory name since #452
  - every reader URL (`/comics/{id}/read`), so bookmarks and shared links carry the old ids
- **Decision:** a UUID as the internal id and a readable slug for URLs
  - **UUID:** the key in every store above. It's stable, can't collide, isn't tied to the name, and is never shown to readers
  - **Slug:** taken from the name (`drabble`, `calvin-and-hobbes`), unique and kept when the comic is renamed, with the old slug redirecting. Reader URLs become `/comics/drabble/read`
  - **Migration:** one migration gives every comic a UUID and a slug, and rewrites the stores above from the old int ids, with an old-id → comic map kept so old URLs, bookmarks and preference entries redirect. The GraphQL API takes the UUID (or the slug where a URL is involved) and keeps accepting the old int for a deprecation period
  - **Still to decide:** whether the strip directories (`{ComicDir}`, name-based today) move to the slug, and how long old ids keep working
- Priority: Medium. Each new id-handling surface can hit the negative-id case, and the migration grows with every store keyed by id

## Log server errors and show the error digest

- `error.tsx` and `global-error.tsx` only `console.error` in the browser. When a server render fails, the user sees a generic page and the `comics-ui` log has nothing to match it to
- Add `instrumentation.ts` with `onRequestError` to log server render and route handler errors with the path and route (Next's instrumentation guide), in the same one-line style as the API logs
- Show `error.digest` on the error pages ("Error reference: …") so a user report can be matched to the log line. Pairs with "Add timing metrics to diagnose slow page loads" and the comiccacher-logs frontend check
- Priority: Medium-High. Small change. The comiccacher-logs frontend check now reads the `comics-ui` log, but a server render error only shows there as Next's `⨯` line with no path or request id, so it can only be matched to the API log by time

## Show a batch job's log while it's running

- "View Logs" on a RUNNING execution says "No logs available for this execution". The per-execution file (`batch-logs/{jobName}/{jobName}-{date}-{hash}.log`) is written from the start banner on, but `JsonBatchExecutionTracker` only records `logFileName` in `afterJob`. Until the job ends, `batchJobLog` finds no file name and returns null
- Record `logFileName` in the STARTED entry `beforeJob` writes (the name is already chosen there for the `batchLogPath` MDC key), and keep it when `afterJob` updates the entry in place
- `LogViewer` fetches once when it opens. While the execution is `STARTED`/`STARTING`, poll `GetBatchJobLog` (3s, like the batch-jobs page's `refetchInterval`), stop once it finishes, follow the tail unless the user has scrolled up or is searching, and show a "Running…" marker in the header
- `batchJobLog` returns the whole file each time. Fine for today's sizes; if a long backfill log makes polling heavy, add an offset argument and return only the new bytes
- Check that lines from the per-source download pool and the manual-launch executor (both use `MdcTaskDecorator`) reach the file mid-run, not only the job thread's. Add tests: tracker writes `logFileName` on start, resolver returns a partial log, viewer polls only while running
- Priority: Medium-High. Today the only way to watch a long run (backfill, a manual retrieval) is `utils/logs.sh` on the host, and the data is already on disk

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
- Priority: Low. Lowered from Medium: only admins see these pages (nobody has OPERATOR) and they work. Fold it into the next change that touches them

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

### Investigate why `rewriteRun` rewrites files on a clean master

- On master at 0e8c3b8, `./gradlew rewriteRun` changed about 55 files nobody had touched, mostly moving imports (e.g. `java.time.Instant` from above the Lombok imports to below them in `JwtTokenDto`). CLAUDE.md lists it as the auto-fix for imports and formatting, so every run drags unrelated files into a change and they have to be reverted by hand
- Find out whether the OpenRewrite import-order recipe disagrees with checkstyle's import rule, or master was formatted another way. Then either align the recipe with checkstyle and run it once over the whole tree in its own PR, or change CLAUDE.md to stop recommending it
- Priority: Medium. Cheap to look into, and it bites every change that follows the documented workflow

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
