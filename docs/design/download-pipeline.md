# Download Pipeline

End-to-end flow from API request through web scraping, validation, deduplication, and storage.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant API as comic-api Controller
    participant MF as ComicManagementFacade
    participant DF as ComicDownloaderFacade
    participant Strategy as AbstractComicDownloaderStrategy
    participant Source as GoComics / ComicsKingdom
    participant IV as ImageValidationService
    participant SF as FileSystemComicStorageFacade
    participant DV as DuplicateImageValidationService
    participant HC as DuplicateHashCacheService
    participant IA as ImageAnalysisService
    participant IDX as ComicIndexService

    API->>MF: updateComicsForDate(date)
    MF->>MF: Load ComicConfig, filter by active + publication day
    loop For each eligible ComicItem
        MF->>DF: downloadComic(ComicDownloadRequest)
        DF->>DF: Resolve strategy from request.source
        DF->>Strategy: downloadComic(request)

        Note over Strategy: AbstractComicDownloaderStrategy.downloadComic()
        Strategy->>Strategy: downloadComicImage(request) [abstract]
        Strategy->>Source: HTTP GET (Jsoup / Selenium)
        Source-->>Strategy: HTML page
        Strategy->>Strategy: Extract image URL (og:image meta tag)
        Strategy->>Source: HTTP GET image URL
        Source-->>Strategy: byte[] imageData

        Strategy->>IV: validate(imageData)
        IV->>IV: Null/empty check
        IV->>IV: Size check (max 10 MB)
        IV->>IV: ImageIO.read() decode test
        IV->>IV: Dimension check (width > 0, height > 0)
        IV->>IV: determineFormat() via ImageIO readers
        IV-->>Strategy: ImageValidationResult

        alt Validation failed
            Strategy-->>DF: ComicDownloadResult.failure()
        else Validation passed
            Strategy-->>DF: ComicDownloadResult.success(imageData)
        end

        DF->>DF: recordSuccess() or recordFailure()
        DF-->>MF: ComicDownloadResult

        alt Download successful
            MF->>SF: saveComicStripWithResult(comic, date, imageData)

            SF->>IV: validateWithMinDimensions(imageData, 100, 50)
            IV-->>SF: ImageValidationResult

            alt Validation failed
                SF-->>MF: SaveResult.validationFailed()
            else Validation passed
                SF->>DV: validateNoDuplicate(comicId, name, date, imageData)
                DV->>DV: Check if duplicate detection enabled
                DV->>DV: Calculate hash via ImageHasherFactory
                DV->>HC: findByHash(comicId, name, year, hash)
                HC->>HC: loadHashesWithBackfill() if needed
                HC-->>DV: Optional<ImageHashRecord>

                alt Duplicate found (different date)
                    DV-->>SF: DuplicateValidationResult.duplicate()
                    SF-->>MF: SaveResult.duplicateSkipped()
                else Unique or same-date overwrite
                    DV-->>SF: DuplicateValidationResult.unique()
                    SF->>SF: Create directory structure
                    SF->>SF: Write image file (yyyy/yyyy-MM-dd.png)
                    SF->>HC: addImageToCache(comicId, name, date, imageData, path)
                    SF->>IDX: addDateToIndex(comicId, name, date)
                    SF->>IA: analyzeImage(comicId, name, imageData, path, validation)
                    IA->>IA: detectColorModeFromImage() via pixel sampling
                    IA-->>SF: ImageMetadata
                    SF->>SF: imageMetadataRepository.saveMetadata()
                    SF-->>MF: SaveResult.saved()
                end
            end
        end
    end
    MF-->>API: List<ComicDownloadResult>
```

## Strategy Dispatch

The `ComicDownloaderFacade` maintains a `ConcurrentHashMap<String, ComicDownloaderStrategy>` of registered strategies. Strategies self-register at startup via `registerDownloaderStrategy(source, strategy)`.

| Source | Strategy Class | Scraping Method | Image Extraction |
|--------|---------------|-----------------|------------------|
| `gocomics` | `GoComicsDownloaderStrategy` | Jsoup HTTP client | `og:image` meta tag from Open Graph metadata |
| `comicskingdom` | `ComicsKingdomDownloaderStrategy` | Jsoup HTTP client | `og:image` meta tags (selects 2nd for hi-res) |

Both strategies extend `AbstractComicDownloaderStrategy` which provides the template method pattern:

1. Call `downloadComicImage(request)` (abstract, implemented by each strategy)
2. Validate the result with `imageValidationService.validate(imageData)`
3. Return `ComicDownloadResult.success()` or `ComicDownloadResult.failure()`

Avatar downloads follow the same pattern through `downloadAvatar()` / `downloadAvatarImage()`.

## Legacy Downloaders

The `IDailyComic` / `DailyComic` hierarchy predates the strategy pattern. `GoComics` uses Selenium WebDriver for JavaScript-rendered pages. `ComicsKingdom` uses Jsoup. These are being replaced by the `*DownloaderStrategy` classes.

## Storage Pipeline

`FileSystemComicStorageFacade.saveComicStripWithResult()` executes a multi-step pipeline:

1. **Image validation** -- `validateWithMinDimensions(imageData, 100, 50)` ensures the image is decodable and meets minimum strip dimensions.
2. **Duplicate detection** -- `DuplicateImageValidationService.validateNoDuplicate()` computes a hash and checks against the year-scoped hash cache. Same-date re-downloads are allowed (overwrite).
3. **File write** -- Creates `{cache-root}/{ComicName}/{yyyy}/{yyyy-MM-dd}.png`.
4. **Hash cache update** -- `DuplicateHashCacheService.addImageToCache()` stores the hash for future dedup.
5. **Index update (CRITICAL)** -- `ComicIndexService.addDateToIndex()` adds the date to the persistent index. If this fails, the file is deleted to maintain consistency.
6. **Metadata analysis (non-critical)** -- `ImageAnalysisService.analyzeImage()` detects color mode and saves `ImageMetadata` via `ImageMetadataRepository`.

Steps 4 and 6 are non-critical: failures are logged but do not fail the save. Step 5 is critical: failure triggers a rollback of the file write.

## Error Recording

`ComicDownloaderFacade` classifies exceptions into `ComicRetrievalStatus` categories:

| Exception Type | Status |
|---------------|--------|
| `ConnectException`, `SocketTimeoutException`, `IOException` | `NETWORK_ERROR` |
| `HttpStatusException` | `PARSING_ERROR` |
| `AccessDeniedException` | `STORAGE_ERROR` |
| All others | `UNKNOWN_ERROR` |

Failed downloads are recorded via `RetrievalStatusService.recordRetrievalResult()` and tracked in `ErrorTrackingService` for per-comic error history. Successful downloads clear the error history for that comic.
