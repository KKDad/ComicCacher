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
