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
