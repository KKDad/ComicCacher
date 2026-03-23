# CLAUDE.md

## Project Overview

ComicCacher is a web comic downloader and viewer application built as a multi-module Gradle project.

## Architecture

```mermaid
graph TD
    subgraph Backend ["Backend (Java 21 / Spring Boot 4)"]
        COMMON["comic-common<br/>DTOs, interfaces, config, utilities"]
        METRICS["comic-metrics<br/>Cache & storage metrics"]
        ENGINE["comic-engine<br/>Downloaders, facades, Spring Batch"]
        API["comic-api<br/>REST + GraphQL API layer"]
    end

    subgraph Frontend
        HUB["comic-hub<br/>Next.js 16 / React 19"]
        WEB["comic-web<br/>Legacy Angular (being replaced)"]
    end

    API --> ENGINE
    API --> METRICS
    API --> COMMON
    ENGINE --> METRICS
    ENGINE --> COMMON
    METRICS --> COMMON
    HUB -->|GraphQL + REST| API
    WEB -->|GraphQL + REST| API
```

## Build Commands

**Backend:**
| Command | Purpose |
|---------|---------|
| `./gradlew clean build` | Full build |
| `./gradlew :comic-api:bootRun` | Run API server |
| `./gradlew :comic-api:test` | Unit tests |
| `./gradlew :comic-api:integrationTest` | Integration tests |
| `./gradlew clean checkstyleMain` | Checkstyle (project-level only) |
| `./gradlew rewriteRun` | Auto-fix imports/spacing/formatting |
| **`./gradlew clean testAll`** | **Final verification before any task** |

**Frontend (comic-hub):**
| Command | Purpose |
|---------|---------|
| `cd comic-hub && npm run dev` | Dev server (http://localhost:3000) |
| `cd comic-hub && npm run build` | Production build |
| `cd comic-hub && npm test` | Run tests |
| `cd comic-hub && npm run codegen` | GraphQL codegen |

## Git Workflow

- **Never commit or push directly to master.**
- **Never add `Co-Authored-By` lines to commit messages.**
- Branch naming: `feature/description` or `fix/description`
- Push and create PR via `gh pr create`

## Module Standards

Each module has its own coding standards. **Module-level standards override this file.**

| Module | Standards | Key Details |
|--------|-----------|-------------|
| **comic-api** | [@~/comic-api/CLAUDE.md](comic-api/CLAUDE.md) | GraphQL-first, Gson (not Jackson), NFS filesystem as DB, JWT auth (USER/OPERATOR/ADMIN) |
| **comic-hub** | [@~/comic-hub/CLAUDE.md](comic-hub/CLAUDE.md) | Z-index tokens, httpOnly cookie auth, `proxy.ts` (not `middleware.ts`) |
| **comic-engine** | See [@~/docs/design/architecture.md](docs/design/architecture.md) | Downloaders, facades, Spring Batch jobs |
| **comic-common** | See [@~/docs/design/architecture.md](docs/design/architecture.md) | Shared DTOs, service interfaces, utilities |

## Time Handling Rules

| Context | Rule |
|---------|------|
| **Storage** | UTC always. Use `OffsetDateTime` or `Instant`, never bare `LocalDateTime` |
| **Date-only values** | `LocalDate` is fine (comic dates, filter ranges) |
| **Spring Batch boundary** | Convert `LocalDateTime` at resolver boundary using `batch.timezone` property |
| **GraphQL wire format** | `DateTime` scalar = ISO-8601 with offset (e.g., `2026-03-18T10:00:00-04:00`) |
| **Frontend** | `new Date(isoString)` for parsing, `toLocaleString()` for display. Never assume a timezone |
| **Gson** | `OffsetDateTimeAdapter` for new code. `LocalDateTimeAdapter` for backward compat only |

## Data Flow: Comic Download Pipeline

```mermaid
graph LR
    DL["Downloader<br/>(GoComics / ComicsKingdom)"] --> VAL["ImageValidationService<br/>size, decode, dimensions"]
    VAL --> DEDUP["DuplicateImageValidationService<br/>perceptual + crypto hashes"]
    DEDUP --> ANALYZE["ImageAnalysisService<br/>color/grayscale detection"]
    ANALYZE --> STORE["NFS Storage<br/>strips/{date}.{ext}"]
```

See [@~/docs/design/download-pipeline.md](docs/design/download-pipeline.md) and [@~/docs/design/image-validation.md](docs/design/image-validation.md).

## Documentation Index

Full docs live in [@~/docs/README.md](docs/README.md):

| Area | Key Documents |
|------|--------------|
| **API** | [@~/docs/api/overview.md](docs/api/overview.md) (auth, pagination, errors), [@~/docs/api/comics.md](docs/api/comics.md), [@~/docs/api/batch-jobs.md](docs/api/batch-jobs.md) |
| **Design** | [@~/docs/design/architecture.md](docs/design/architecture.md), [@~/docs/design/batch-jobs.md](docs/design/batch-jobs.md), [@~/docs/design/downloader-strategies.md](docs/design/downloader-strategies.md) |
| **Storage** | [@~/docs/storage/overview.md](docs/storage/overview.md) (NFS layout, atomic writes), [@~/docs/storage/comic-data.md](docs/storage/comic-data.md) |
| **UI Refactor** | [@~/docs/2026-ui-refactor/](docs/2026-ui-refactor/) (visual style, component specs, screen layouts) |

## Debug Utilities

- **`utils/fetch-prod-logs.sh [lines]`** — Fetch Docker logs from production (default 500 lines)
- **`utils/tunnel-to-prod-api.sh`** — SSH tunnel: `localhost:8888` to production API
