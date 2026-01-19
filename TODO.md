# ComicCacher TODO

## Performance Improvements

### API Response Caching
- Add HTTP caching headers to backend API endpoints (Cache-Control, ETag)
- Target: Comic image endpoints (/api/v1/comics/{id}/avatar, /api/v1/comics/{id}/strip/*)
- Expected benefit: Reduce redundant network requests, improve load times for repeat visits
- Priority: Medium
- Estimated effort: 2-4 hours

### Review @Cacheable Usage in ComicManagementFacade
- Location: `comic-engine/src/main/java/org/stapledon/engine/management/ComicManagementFacade.java`
- Issue: `@Cacheable` on `getAllComics()` may be redundant since:
  - Comics are already stored in an in-memory `ConcurrentHashMap`
  - Sorting is O(n log n) but n is small (dozens of comics)
  - The cache adds complexity without meaningful performance benefit
- Action: Consider removing `@Cacheable` from `getAllComics()` to simplify code
- Note: Already removed `@Cacheable` from `getComic(id)` due to Optional caching issues
- Priority: Low
- Estimated effort: 30 minutes

### Standardize DateTime Types for GraphQL
- Issue: Some DTOs (e.g., `CombinedMetricsData`, `AccessMetricsData`) use `LocalDateTime` but GraphQL's `DateTime` scalar expects `OffsetDateTime` (RFC 3339)
- Current workaround: Using `@SchemaMapping` in resolvers to convert `LocalDateTime` → `OffsetDateTime`
- Action: Update all DTOs with timestamp fields to use `OffsetDateTime` for consistency
- Affected modules: `comic-metrics`, `comic-common`
- Priority: Low
- Estimated effort: 2-3 hours

## Testing

### BatchJobResolver Unit Tests
- Location: `comic-api/src/main/java/org/stapledon/api/resolver/BatchJobResolver.java`
- Issue: Integration tests are limited because batch jobs are disabled in test profile
- Action: Add comprehensive unit tests with mocked `BatchJobMonitoringService` and `DailyJobScheduler`
- Cover: All query methods (`recentBatchJobs`, `batchJobsByDateRange`, `batchJob`, `batchJobSummary`)
- Cover: Mutation (`triggerBatchJob`) with success and failure scenarios
- Priority: Medium
- Estimated effort: 1-2 hours
