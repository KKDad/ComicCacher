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
- Add HTTP caching headers to backend API endpoints (Cache-Control, ETag)
- Target: Comic image endpoints (/api/v1/comics/{id}/avatar, /api/v1/comics/{id}/strip/*)
- Expected benefit: Reduce redundant network requests, improve load times for repeat visits
- Priority: Medium
- Estimated effort: 2-4 hours


### Enable Gradle Configuration Cache
- Consider enabling the Gradle configuration cache to speed up builds
- Reference: https://docs.gradle.org/9.4.0/userguide/configuration_cache_enabling.html
- Priority: Low

### Revisit OpenAPI/Swagger Generation
- With the move to GraphQL, only 2 REST endpoints remain (binary image streaming)
- Evaluate whether the openapi-gradle-plugin, `generate-openapi-docs.sh`, and `openapi.json` are still worth maintaining
- If not needed, remove the springdoc dependency, openApi task config, and related tasks from comic-api/build.gradle

### Upgrade to Java 25
- Upgrade from Java 21 to Java 25 when available
- Update `build.gradle` Java toolchain/sourceCompatibility settings
- Update CI/CD pipeline and Docker base images
- Clean up deprecated API usage first (see below)

### Clean Up Deprecated Java APIs
Audit complete. Remaining items (JJWT builder migration done):
- **Jsoup `.first()`/`.last()` → `.selectFirst()` / stream-based** — 8 instances across `GoComics`, `GoComicsDownloaderStrategy`, `ComicsKingdom`, `ComicsKingdomDownloaderStrategy` in comic-engine
- **Guava `@VisibleForTesting` → remove or replace** — 3 instances (`ComicBackfillService`, `DailyJobScheduler`, `SchedulerHealthCheck`)
- **Guava `Files.getNameWithoutExtension()` → plain Java** — 1 instance in `ImageUtils`
- Priority: Medium (do before Java 25 upgrade)

### Consolidate Root JSON Files into a Data Folder
- Move the loose JSON files in the project root into a single `data/` folder
- Update all code references to the new paths

### Reduce WebDriver Startup Overhead in GoComics IT Tests
- **Root cause:** `GoComicsIntegrationIT.getSubject()` creates a new `GoComics` instance per test method, each of which lazy-inits a new `ChromeDriver` process (~2-5s startup cost per test)
- **Where:** `GoComicsIntegrationIT` (lines 52-59) uses try-with-resources per test; `GoComics.initializeWebDriver()` (lines 67-95) does `WebDriverManager.chromedriver().setup()` + `new ChromeDriver()`
- **Fix is at the test level**, not in `GoComics` itself — it already has lazy init and `AutoCloseable`
- **Approach:** Share a single `GoComics` instance (or at least a shared `WebDriver`) across the test class via `@BeforeAll`/`@AfterAll`, resetting comic-specific state between tests instead of recreating the browser

## Feature Ideas (from competitive analysis)

### Reading Progress Tracking
- Per-user "mark as read" state with continue-where-you-left-off and unread count badges
- Multi-user auth already exists — add per-user read state to the preference/user model
- Every competitor with a UI has this (Kavita, Komga, OpenComic)
- Priority: Medium-High

### CBZ/PDF Export
- Export a date range of strips as CBZ or PDF for offline reading
- Natural extension of existing image storage — images are already on disk
- Universal feature across comic downloaders (dosage, comic-dl, mangal, comics-downloader)
- Priority: Medium

### OPDS Feed
- Serve comics via the OPDS protocol for external reader apps (Panels, Chunky, KOReader)
- Opens the collection to a large ecosystem of existing reader apps
- Kavita and Komga both support this
- Priority: Medium

### Respect robots.txt
- Check and honor `robots.txt` rules from GoComics and ComicsKingdom before scraping
- Good-citizen behavior that aligns with the copyright notice in the README
- dosage implements this — set a `User-Agent` and respect disallow rules
- Priority: Medium

### Random Strip Button
- Pick a random date within the available range for a given comic
- Fun daily discovery feature — trivial to implement
- Priority: Low (easy win)

### Favorites / Collections
- Named groups beyond "my comics" (e.g., "Sunday Funnies", "Political", "Classic")
- Kavita has collections and reading lists
- Priority: Low

### Download Failure Notifications
- Alert when a comic fails to download for N consecutive days
- Could be webhook, email, or in-app notification
- Priority: Low

### Configurable Scraping Rate Limits
- Expose delay configuration for respectful scraping
- Currently hardcoded — make configurable per source
- Priority: Low
