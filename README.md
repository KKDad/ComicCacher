# ComicCacher

![Comic Viewer Banner](assets/ComicViewer.png)

[![Backend CI](https://github.com/KKDad/ComicCacher/actions/workflows/gradle.yml/badge.svg)](https://github.com/KKDad/ComicCacher/actions/workflows/gradle.yml)
[![Frontend CI](https://github.com/KKDad/ComicCacher/actions/workflows/angular.yml/badge.svg)](https://github.com/KKDad/ComicCacher/actions/workflows/angular.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F)
![Next.js](https://img.shields.io/badge/Next.js-16-black)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> Your daily comics. No ads. No paywalls. No nonsense.

**ComicCacher** is a self-hosted comic strip reader that automatically downloads, caches, and serves your favorite daily strips from GoComics and ComicsKingdom in a clean, distraction-free interface. It does exactly one thing well: **lets you read your comics in peace.**

---

## Why?

Every comic strip site is the same story: autoplay ads, pop-ups, newsletter modals, cookie banners, and tracking scripts — all to read a four-panel strip. ComicCacher sidesteps all of it. It scrapes the strips at 6 AM, stores them locally, and serves them through a fast, minimal UI. That's it.

## Highlights

- **Fully automated** — Comics download daily on schedule. Missed a day? Startup makeup runs catch it.
- **Self-healing** — Automatically backfills gaps, syncs avatars, repairs metadata, and cleans up stale data — all observable from the admin UI.
- **Smart dedup** — Duplicate strips are automatically detected and skipped so you never see the same image twice.
- **Instant navigation** — Aggressive caching and predictive prefetch mean strips load before you click.
- **GraphQL-first API** — With REST fallback, Swagger UI, JWT auth, and three roles (USER, OPERATOR, ADMIN).
- **Responsive UI** — Works on phone, tablet, and desktop. Dark mode included.
- **Fully self-contained** — No database required. Two Docker containers and a volume mount. That's the entire deployment.

## Supported Sources

| Source | Comics | Scraping Method |
|--------|--------|-----------------|
| **[GoComics](https://www.gocomics.com)** | 300+ strips | Selenium WebDriver |
| **[ComicsKingdom](https://comicskingdom.com)** | 100+ strips | Jsoup HTML parsing |

## How It Works

1. **Schedule** — Batch job fires at 6 AM and scrapes today's strips from GoComics and ComicsKingdom
2. **Validate** — 3-layer pipeline checks image integrity, dimensions, and deduplicates via perceptual + cryptographic hashing
3. **Store** — Strips saved to filesystem with atomic writes (NFS-safe, no database required)
4. **Serve** — GraphQL API delivers strips to the Next.js frontend with Caffeine cache for instant responses
5. **Read** — Clean, ad-free reading experience on any device

## Tech Stack

| Layer | Tech |
|-------|------|
| **Backend** | Java 21, Spring Boot 4, Spring Batch, Caffeine Cache |
| **API** | GraphQL + REST, Springdoc OpenAPI, JWT (JJWT) |
| **Scraping** | Jsoup (ComicsKingdom), Selenium (GoComics) |
| **Image Pipeline** | TwelveMonkeys ImageIO, perceptual hashing, 3-layer validation |
| **Frontend** | Next.js 16, React 19, TypeScript 5, Tailwind CSS 4, TanStack Query v5 |
| **Infra** | Docker |

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

## Prerequisites

- **Java 21** and **Gradle** (for backend)
- **Node.js 22 LTS** and **npm** (for frontend)
- **Docker** (for containerized deployment)

## Quick Start

### From Source

#### Backend

```bash
./gradlew :comic-api:build
./gradlew :comic-api:bootRun
```

API docs at [localhost:8080/swagger-ui](http://localhost:8080/swagger-ui/index.html) | GraphQL at [localhost:8080/graphql](http://localhost:8080/graphql)

#### Frontend

```bash
cd comic-hub
cp .env.example .env.local
npm install && npm run dev
```

Open [localhost:3000](http://localhost:3000).

## Deployment

Runs as two Docker containers on a home server. Build the images with:

```bash
# Backend
./gradlew :comic-api:build
./comic-api/build-docker.sh <TAG>

# Frontend
cd comic-hub && ./build-docker.sh
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

This project started in **2013** as a quick C#/.NET 3.0 script to scrape a few comics. Over the years it's been rewritten, rearchitected, and modernized — first to Java/Spring, then to Spring Boot 4 with a GraphQL API, and most recently to a Next.js 16 frontend. 500+ commits later, it's been serving up daily laughs for over a decade.

There's no public deployment. This is a personal project, built for fun and for learning. But the code is here if you want to run your own — PRs and ideas are welcome.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md).

## Notice

This software is intended for **personal, private use only**. It is not designed to publicly broadcast, redistribute, or archive comic strips. Downloaded comics must not be shared publicly without explicit permission from the copyright holder.

**Please support the creators.** The artists and writers behind these strips deserve your support. Visit their official sites, buy their books, or donate directly — they make the comics you enjoy every day.

If you are a comic publisher or copyright holder and would like your content excluded from ComicCacher, please [open an issue](../../issues/new) and we will promptly remove support for your strips.

## Development

For module internals, testing guidelines, and coding standards, see the [docs/](docs/README.md) directory.
