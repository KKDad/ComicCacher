# Comic Data Files

Each comic has its own directory under the cache root. Within that directory, strip images are organized by year alongside index files, hash caches, and metadata sidecars.

## Directory Structure

```
{CacheRoot}/{ComicDirName}/
  avatar.png                              # Comic avatar (see overview.md)
  available-dates.json                    # Date index for fast navigation
  {Year}/                                 # e.g., 2025/
    2025-01-15.png                        # Strip image
    2025-01-15.json                       # Metadata sidecar
    image-hashes.json                     # Duplicate detection hashes
```

---

## Strip Images

Strip images are stored as PNG files named by date in ISO-8601 format.

- **Path pattern:** `{CacheRoot}/{ComicDirName}/{yyyy}/{yyyy-MM-dd}.png`
- **Naming:** `DateTimeFormatter.ofPattern("yyyy-MM-dd")` for the filename, `DateTimeFormatter.ofPattern("yyyy")` for the year directory
- **Validation before save:** Images must pass `ImageValidationService` checks -- minimum dimensions of 100x50 pixels, valid decodable format, max 10MB file size
- **Duplicate check:** Before saving, `DuplicateValidationService` checks the image hash against existing hashes for the same comic/year. Duplicates are skipped.

### Save Sequence (FileSystemComicStorageFacade.saveComicStripWithResult)

1. Validate image (format, dimensions, size)
2. Check for duplicates via `DuplicateValidationService`
3. Create `{ComicDirName}/{year}/` directory if needed
4. Write image bytes to `{yyyy-MM-dd}.png`
5. **CRITICAL:** Update `available-dates.json` via `ComicIndexService.addDateToIndex()` -- if this fails, the image file is deleted to maintain consistency
6. Add hash to `image-hashes.json` via `DuplicateHashCacheService` (non-critical)
7. Analyze and save metadata sidecar `{yyyy-MM-dd}.json` via `ImageMetadataRepository` (non-critical)

---

## available-dates.json (Date Index)

A sorted list of all dates that have a strip image on disk for a given comic. Eliminates expensive day-by-day directory scans on NFS/RAID storage.

- **Path:** `{CacheRoot}/{ComicDirName}/available-dates.json`
- **Constant:** `ComicIndexService.INDEX_FILENAME = "available-dates.json"`
- **Persistence:** Written via `NfsFileOperations.atomicWrite()`

**DTO:** `ComicDateIndex` (`comic-common`)

```json
{
  "comicId": 42,
  "comicName": "Calvin and Hobbes",
  "availableDates": [
    "1985-11-18",
    "1985-11-19",
    "1985-11-20",
    "2025-03-17",
    "2025-03-18"
  ],
  "lastUpdated": "2025-03-18"
}
```

### Field Reference

| Field | Type | Description |
|:---|:---|:---|
| `comicId` | `int` | Comic identifier |
| `comicName` | `String` | Comic display name |
| `availableDates` | `List<LocalDate>` | Sorted ascending list of dates with images on disk |
| `lastUpdated` | `LocalDate` | Date the index was last modified |

### Binary Search Navigation

The `availableDates` list is always maintained in sorted order. `ComicIndexService` uses `Collections.binarySearch()` for O(log n) lookups:

- **`getNextDate(comicId, comicName, fromDate)`** -- finds the first date strictly after `fromDate`
- **`getPreviousDate(comicId, comicName, fromDate)`** -- finds the last date strictly before `fromDate`
- **`getNewestDate(comicId, comicName)`** -- returns the last element
- **`getOldestDate(comicId, comicName)`** -- returns the first element
- **`addDateToIndex(comicId, comicName, date)`** -- inserts at the correct sorted position (no-op if already present)
- **`removeDateFromIndex(comicId, comicName, date)`** -- removes a date (used during purge)

### Lazy Load and In-Memory Cache

Indexes are loaded lazily on first access per comic and cached in a `ConcurrentHashMap<Integer, ComicDateIndex>`. Thread safety is provided by per-comic `ReentrantReadWriteLock` instances.

Load sequence in `getOrLoadIndex()`:

1. Try read lock -- return if cached
2. Upgrade to write lock, double-check cache
3. Load from disk (`available-dates.json`)
4. If empty, check `verifiedEmptyComics` set to avoid repeated rebuilds
5. If not verified empty, trigger filesystem rebuild
6. If still empty after rebuild, mark as verified empty

### Rebuild from Filesystem

`rebuildIndex(comicId, comicName)` scans the filesystem to regenerate the index:

1. Resolve comic directory: `{CacheRoot}/{sanitizedComicName}/`
2. Iterate year directories (skip `@`-prefixed Synology metadata dirs)
3. For each `*.png` file, parse the date from the filename (`yyyy-MM-dd`)
4. Collect into a sorted list
5. Write to disk, then update the in-memory cache

Optional `validateMetadata` flag reads each sidecar JSON to verify the `comicId` matches the expected value.

The `invalidateCache(comicId)` method evicts the in-memory entry, forcing a reload on next access.

---

## image-hashes.json (Duplicate Detection)

Stores perceptual or cryptographic hashes per image for duplicate detection within a comic/year. Managed by `DuplicateImageHashRepository` with an in-memory cache layer, and `DuplicateHashCacheService` for backfill and algorithm migration.

- **Path:** `{CacheRoot}/{ComicDirName}/{Year}/image-hashes.json`
- **Constant:** `DuplicateImageHashRepository.HASH_FILE_NAME = "image-hashes.json"`
- **Persistence:** Written via `NfsFileOperations.atomicWrite()`

**DTO:** `Map<String, ImageHashRecord>` keyed by hash value (`comic-common`)

```json
{
  "a1b2c3d4e5f6": {
    "hash": "a1b2c3d4e5f6",
    "date": "2025-03-18",
    "filePath": "/cache/CalvinandHobbes/2025/2025-03-18.png",
    "algorithm": "DIFFERENCE_HASH"
  },
  "f6e5d4c3b2a1": {
    "hash": "f6e5d4c3b2a1",
    "date": "2025-03-17",
    "filePath": "/cache/CalvinandHobbes/2025/2025-03-17.png",
    "algorithm": "DIFFERENCE_HASH"
  }
}
```

### Field Reference (ImageHashRecord)

| Field | Type | Description |
|:---|:---|:---|
| `hash` | `String` | Hash value (also the map key) |
| `date` | `LocalDate` | Date of the comic strip |
| `filePath` | `String` | Absolute path to the image file |
| `algorithm` | `HashAlgorithm` | Algorithm used: `DIFFERENCE_HASH`, `AVERAGE_HASH`, `MD5`, `SHA256` |

### Hash Algorithm Configuration

Configured via `comics.cache.hashAlgorithm` in `CacheProperties`:

| Algorithm | Type | Description |
|:---|:---|:---|
| `DIFFERENCE_HASH` (default) | Perceptual | Detects visually similar images |
| `AVERAGE_HASH` | Perceptual | Simpler, faster, less accurate |
| `MD5` | Cryptographic | Byte-exact matching, fast |
| `SHA256` | Cryptographic | Byte-exact matching, secure |

Duplicate detection can be disabled entirely via `comics.cache.duplicateDetectionEnabled` (default: `true`).

### Backfill and Algorithm Migration (DuplicateHashCacheService)

`loadHashesWithBackfill()` handles two automatic scenarios:

1. **Empty cache with existing images:** If `image-hashes.json` is empty or missing but the year directory contains `*.png` files, all images are hashed and the cache is populated.
2. **Algorithm change:** If the `algorithm` field in existing records differs from the configured `comics.cache.hashAlgorithm`, the entire year's cache is rebuilt with the new algorithm.

In both cases, `replaceHashes()` overwrites the file atomically.

### In-Memory Cache

`DuplicateImageHashRepository` maintains a `ConcurrentHashMap` keyed by `"{comicId}:{year}"`. Loaded on first access per comic/year. `clearCache()` evicts all entries.

---

## Metadata Sidecars

Each strip image has a companion JSON sidecar containing image analysis metadata. Managed by `ImageMetadataRepository`.

- **Path:** `{CacheRoot}/{ComicDirName}/{Year}/{yyyy-MM-dd}.json` (same name as the image, with `.json` extension)
- **Persistence:** Written via `NfsFileOperations.atomicWrite()`
- **Naming logic:** The image file extension (`.png`, `.jpg`, `.jpeg`, `.gif`, `.tif`, `.tiff`, `.bmp`, `.webp`) is replaced with `.json`

**DTO:** `ImageMetadata` (`comic-common`)

```json
{
  "comicId": 42,
  "comicName": "Calvin and Hobbes",
  "filePath": "/cache/CalvinandHobbes/2025/2025-03-18.png",
  "format": "PNG",
  "width": 900,
  "height": 293,
  "sizeInBytes": 145230,
  "colorMode": "COLOR",
  "samplePercentage": 10.0,
  "captureTimestamp": "2025-03-18T06:30:45-04:00",
  "sourceUrl": "https://assets.amuniversal.com/abc123"
}
```

### Field Reference

| Field | Type | Description |
|:---|:---|:---|
| `comicId` | `int` | Comic identifier |
| `comicName` | `String` | Comic display name |
| `filePath` | `String` | Absolute path to the image file |
| `format` | `ImageFormat` | `PNG`, `JPEG`, `GIF`, `BMP`, `WEBP`, `TIFF`, `UNKNOWN` |
| `width` | `int` | Image width in pixels |
| `height` | `int` | Image height in pixels |
| `sizeInBytes` | `long` | File size in bytes |
| `colorMode` | `ColorMode` | `GRAYSCALE`, `COLOR`, `UNKNOWN` |
| `samplePercentage` | `double` | Percentage of pixels sampled for color detection (0.0-100.0) |
| `captureTimestamp` | `OffsetDateTime` | When metadata was captured |
| `sourceUrl` | `String` (nullable) | Original download URL |

### Validation Rules

Metadata is only saved if it passes validation in `ImageMetadataRepository.isValidMetadata()`:

- `format` must not be `null` or `UNKNOWN`
- `width` and `height` must be > 0
- `sizeInBytes` must be > 0
- `colorMode` of `UNKNOWN` is acceptable (partial metadata is better than none)

Metadata capture is non-critical -- if it fails, the strip image save still succeeds.

---

## Key Source Files

| File | Module |
|:---|:---|
| `FileSystemComicStorageFacade.java` | `comic-engine` |
| `ComicIndexService.java` | `comic-engine` |
| `ComicDateIndex.java` | `comic-common` |
| `DuplicateImageHashRepository.java` | `comic-engine` |
| `DuplicateHashCacheService.java` | `comic-engine` |
| `ImageHashRecord.java` | `comic-common` |
| `ImageMetadataRepository.java` | `comic-engine` |
| `ImageMetadata.java` | `comic-common` |
| `NfsFileOperations.java` | `comic-common` |
