# ComicCacher

![Comic Viewer Banner](assets/ComicViewer.png)

> Your daily comics. No ads. No paywalls. No nonsense.

**ComicCacher** is a self-hosted comic strip reader that automatically downloads, caches, and serves your favorite daily strips from GoComics and ComicsKingdom in a clean, distraction-free interface.

Born as a C#/.NET hobby project in 2013, it's been rebuilt from scratch across 500+ commits into a modern, production-grade stack — and it still does exactly what it was always meant to do: **let you read your comics in peace.**

---

## Why?

Every comic strip site is the same story: autoplay ads, pop-ups, newsletter modals, cookie banners, and tracking scripts — all to read a four-panel strip. ComicCacher sidesteps all of it. It scrapes the strips at 6 AM, stores them locally, and serves them through a fast, minimal UI. That's it.

## Highlights

- **Fully automated** — Comics download daily on schedule. Missed a day? Startup makeup runs catch it.
- **Smart caching** — Perceptual + cryptographic hash dedup, image validation, and predictive prefetch so navigation is instant.
- **6 batch jobs** — Daily download, backfill gaps, avatar sync, metadata repair, metrics archive, and record purge — all observable from the admin UI.
- **GraphQL-first API** — With REST fallback, Swagger UI, JWT auth, and three roles (USER, OPERATOR, ADMIN).
- **Responsive UI** — Works on phone, tablet, and desktop. Dark mode included.
- **Runs anywhere** — Docker, Kubernetes, bare metal. If it runs Java 21 and Node 22, it runs ComicCacher.
- **Fully self-contained** — No database required. All state lives on the filesystem (NFS-safe with atomic writes).

## Tech Stack

| Layer | Tech |
|-------|------|
| **Backend** | Java 21, Spring Boot 4, Spring Batch, Caffeine Cache |
| **API** | GraphQL + REST, Springdoc OpenAPI, JWT (JJWT) |
| **Scraping** | Jsoup (ComicsKingdom), Selenium (GoComics) |
| **Image Pipeline** | TwelveMonkeys ImageIO, perceptual hashing, 3-layer validation |
| **Frontend** | Next.js 16, React 19, TypeScript 5, Tailwind CSS 4, TanStack Query v5 |
| **Infra** | Docker, Helm, Kubernetes |

## Architecture

```
comic-common ─── shared DTOs, config, service interfaces
     ↑
     ├── comic-metrics ─── cache & storage metrics
     ├── comic-engine ─── download engine, batch jobs, image validation
     │        ↑
     └── comic-api ─── REST + GraphQL API (orchestrates everything)
              ↓
         comic-hub ─── Next.js frontend
```

Full architecture docs, Mermaid diagrams, and module reference in [`docs/`](docs/README.md).

## Quick Start

### Backend

```bash
./gradlew :comic-api:build
./gradlew :comic-api:bootRun
```

API docs at [localhost:8080/swagger-ui](http://localhost:8080/swagger-ui/index.html) | GraphQL at [localhost:8080/graphql](http://localhost:8080/graphql)

### Frontend

```bash
cd comic-hub
cp .env.example .env.local
npm install && npm run dev
```

Open [localhost:3000](http://localhost:3000).

## Deployment

Runs on a home Kubernetes cluster. Build and deploy with:

```bash
# Backend
./gradlew :comic-api:build
./comic-api/build-docker.sh <TAG>

# Frontend
cd comic-hub && ./build-docker.sh

# Deploy
helm upgrade comics comics
```

## Project Structure

```
ComicCacher/
├── comic-common/    # Shared DTOs, config, service interfaces
├── comic-metrics/   # Cache & storage metrics collection
├── comic-engine/    # Download engine, batch jobs, image validation
├── comic-api/       # REST + GraphQL API layer
├── comic-hub/       # Web frontend (Next.js 16 / React 19)
├── comic-web/       # Legacy Angular frontend (being replaced)
├── docs/            # API reference, design docs, storage specs
└── utils/           # Debug & deployment scripts
```

## The Story

This project started in **2013** as a quick C#/.NET 3.0 script to scrape a few comics. Over the years it's been rewritten, rearchitected, and modernized — first to Java/Spring, then to Spring Boot 4 with a GraphQL API, and most recently to a Next.js 16 frontend. It runs on a home K8s cluster and has been serving up daily laughs for over a decade.

There's no public deployment. This is a personal project, built for fun and for learning. But the code is here if you want to run your own — PRs and ideas are welcome.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md).

## Development

For detailed build instructions, module internals, testing guidelines, and coding standards, see [CLAUDE.md](CLAUDE.md).
