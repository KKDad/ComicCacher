# ComicCacher TODO

## Forgot Password Flow
- Wire up `requestPasswordReset` mutation in `comic-hub/src/app/(auth)/forgot-password/page.tsx`
- Currently the form submission is a no-op that immediately shows the success view
- Priority: Medium

## Performance Improvements

### API Response Caching
- Add HTTP caching headers to backend API endpoints (Cache-Control, ETag)
- Target: Comic image endpoints (/api/v1/comics/{id}/avatar, /api/v1/comics/{id}/strip/*)
- Expected benefit: Reduce redundant network requests, improve load times for repeat visits
- Priority: Medium
- Estimated effort: 2-4 hours


### Revisit OpenAPI/Swagger Generation
- With the move to GraphQL, only 2 REST endpoints remain (binary image streaming)
- Evaluate whether the openapi-gradle-plugin, `generate-openapi-docs.sh`, and `openapi.json` are still worth maintaining
- If not needed, remove the springdoc dependency, openApi task config, and related tasks from comic-api/build.gradle

### Remove Jackson BOM Overrides When Spring Boot Catches Up
- `ext['jackson-2-bom.version']` and `ext['jackson-bom.version']` in root build.gradle override Spring Boot 4.0.3's Jackson to fix CVEs
- Check with each Spring Boot upgrade whether the bundled Jackson versions include the fixes, and remove the overrides if so

### Reduce WebDriver Startup Overhead in GoComics IT Tests
- **Root cause:** `GoComicsIntegrationIT.getSubject()` creates a new `GoComics` instance per test method, each of which lazy-inits a new `ChromeDriver` process (~2-5s startup cost per test)
- **Where:** `GoComicsIntegrationIT` (lines 52-59) uses try-with-resources per test; `GoComics.initializeWebDriver()` (lines 67-95) does `WebDriverManager.chromedriver().setup()` + `new ChromeDriver()`
- **Fix is at the test level**, not in `GoComics` itself — it already has lazy init and `AutoCloseable`
- **Approach:** Share a single `GoComics` instance (or at least a shared `WebDriver`) across the test class via `@BeforeAll`/`@AfterAll`, resetting comic-specific state between tests instead of recreating the browser
