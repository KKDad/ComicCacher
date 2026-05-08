# Comic Common Coding Standards

Shared types and contracts. Sits at the bottom of the dependency graph — every other backend module depends on this one. Defers to [@~/comic-api/CLAUDE.md](../comic-api/CLAUDE.md) for cross-cutting Java standards.

## What Lives Here

| Package | Purpose |
|---------|---------|
| `common.dto` | DTOs shared across modules (`ComicItem`, `ComicConfig`, `ImageDto`, etc.) |
| `common.service` | Service interfaces (no implementations) |
| `common.repository` | Repository interfaces |
| `common.config` | Shared config types and base configuration |
| `common.infrastructure` | Cross-cutting infrastructure types |
| `common.model` | Domain model types |
| `common.util` | Utilities — notably `GsonUtils` |

## What Does NOT Live Here

- **No Spring controllers, resolvers, or `@Component` beans.** This module is type/interface only.
- **No Spring Batch jobs.** Those are in `comic-engine`.
- **No business logic.** Implementations belong in the consuming module.
- **No HTTP clients, no Selenium, no Jsoup.** Scraping lives in `comic-engine`.

## Stability

This module is the foundation everything else builds on. Treat changes here as breaking-by-default:
- **Adding a field to a DTO:** safe if optional and Gson-tolerant.
- **Renaming a field or changing its type:** breaks every consumer + every JSON file already on disk. Don't do it. Add a new field, deprecate the old one, migrate over multiple releases.
- **Adding a new DTO class:** safe.
- **Removing a class or interface:** breaks consumers. Deprecate first, remove later.

## Serialization

- DTOs intended for JSON-on-disk MUST round-trip through `GsonUtils`. Test the round-trip.
- Use `@SerializedName` if the JSON key differs from the field name.
- For Records (rare in this codebase — see Lombok preference below): register `RecordAdapterFactory`.
- Use the `@Qualifier("gsonWithLocalDate")` Gson bean for date-time serialization. Prefer `OffsetDateTimeAdapter` for new code; `LocalDateTimeAdapter` is for backward-compat reads only.

## DTO Standard

Use Lombok (`@Builder`, `@Getter`, `@Setter`, `@AllArgsConstructor`, `@NoArgsConstructor`) for all DTOs. Java records are not the preferred pattern in this codebase.

## Time Handling

- Storage / wire instants: `OffsetDateTime` or `Instant`. Never bare `LocalDateTime`.
- Date-only values (comic dates, filter ranges): `LocalDate`.
