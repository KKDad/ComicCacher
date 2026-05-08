# Comic Metrics Coding Standards

Cache and storage metrics. Tracks comic access counts, error rates per comic, and color/grayscale analysis sampling. Persists event-driven to JSON in the comics cache directory. Defers to [@~/comic-api/CLAUDE.md](../comic-api/CLAUDE.md) for cross-cutting Java standards.

## Module Layout

| Package | Purpose |
|---------|---------|
| `metrics.collector` | Collectors that observe events from comic-api / comic-engine and update in-memory counters |
| `metrics.service` | Aggregation and query services |
| `metrics.repository` | JSON-backed persistence for metrics state |
| `metrics.dto` | Metric DTOs (use Lombok per project standard) |
| `metrics.config` | Properties classes and Spring config |

## Persistence Model

- Metrics live in JSON files under `${comics.cache.location}` (e.g., `last_errors.json`, access metrics).
- Persistence is **event-driven**, not scheduled. The `comics.metrics.persist-threshold` property (default 50) controls how many access events buffer before a flush.
- Per-comic error history is bounded: `comics.metrics.error-tracking.max-errors-per-comic` (default 5).
- Color-detection sampling is configurable: `comics.metrics.color-detection.sample-percentage` (default 5.0) — percentage of pixels sampled to classify B&W vs color.

## Standards

- DTOs use Lombok (`@Builder`, `@Getter`, `@Setter`, etc.). Records are not the preferred pattern in this codebase.
- Counters and aggregates are in-memory primitives or `LongAdder`/`AtomicLong` — never raw `int`/`long` for shared state.
- Read paths must be safe under concurrent updates from collectors.
- All on-disk metric files use Gson via `GsonUtils` and the project's standard adapters.

## Metrics Pipeline (intentional design)

Metrics surface only through GraphQL queries and JSON files on disk. **This is the intentional design** — ComicCacher is a single-tenant home-server app and does not run a Micrometer / Prometheus / OpenTelemetry pipeline. Don't add one without a concrete operator need; the JSON-on-disk approach is sufficient and matches the rest of the storage model.
