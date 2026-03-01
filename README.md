# ComicCacher

![Comic Viewer Banner](assets/ComicViewer.png)

> Serving up daily laughs since 2013

A personal project to read my favorite daily comic strips without ads, pop-ups, or paywalls. Just comics.

Originally written in C# and .NET 3.0 back in 2013, this has been rebuilt from the ground up with modern tech:
- **Backend**: Spring Boot 4 + Java 21 API that downloads and caches comics daily
- **Frontend**: Next.js 16 + React 19 web app (migrating from Angular — see `comic-hub/`)
- **Hosting**: Runs in my home K8s cluster

There's no public deployment — I built this for my own use and for fun. But if you'd like to run it yourself, go ahead!

## What does it do?

- Downloads daily comic strips from GoComics and ComicsKingdom
- Caches images locally with automatic cleanup
- Displays them in a clean, ad-free interface
- Works on mobile, tablet, and desktop
- Tracks metrics on cache performance and storage

## Project Structure

```
ComicCacher/
├── comic-common/    # Shared DTOs, config, service interfaces (Java)
├── comic-metrics/   # Cache & storage metrics collection (Java)
├── comic-engine/    # Download & storage engine, Spring Batch jobs (Java)
├── comic-api/       # REST + GraphQL API layer (Java/Spring Boot)
├── comic-hub/       # New web frontend (Next.js 16, React 19, TypeScript)
├── comic-web/       # Legacy web frontend (Angular — being replaced)
├── docs/            # API endpoint docs, UI refactor specs
└── utils/           # Debug & deployment scripts
```

## Quick Start

### Backend (comic-api)

```bash
./gradlew :comic-api:build
./gradlew :comic-api:bootRun

# View API docs at http://localhost:8080/swagger-ui/index.html
```

### Frontend (comic-hub)

```bash
cd comic-hub
cp .env.example .env.local
npm install
npm run dev

# Visit http://localhost:3000
```

## Deployment

Deployed to a home K8s cluster using Docker and Helm:

```bash
# Build backend
./gradlew :comic-api:build
./comic-api/build-docker.sh <TAG>

# Build frontend
cd comic-hub
./build-docker.sh

# Deploy with Helm
helm upgrade comics comics
```

## API & Docs

When running locally:
- Web App: http://localhost:3000
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- REST API: http://localhost:8080/api/v1/comics
- GraphQL: http://localhost:8080/graphql
- Metrics: http://localhost:8080/api/v1/metrics

Full endpoint documentation in the [docs/](docs/) directory.

## Development

For detailed architecture, build instructions, module structure, testing guidelines, and coding standards, see [CLAUDE.md](CLAUDE.md).
