# ComicCacher

![Comic Viewer Banner](assets/ComicViewer.png)

> Serving up daily laughs since 2013

A personal project to read my favorite daily comic strips without ads, pop-ups, or paywalls. Just comics.

Originally written in C# and .NET 3.0 back in 2013, this has been rebuilt from the ground up with modern tech:
- **Backend**: Spring Boot 4 + Java 21 API that downloads and caches comics daily
- **Frontend**: Angular 21 + Material Design web app with glassmorphism UI (zoneless, Vitest)
- **Hosting**: Runs in my home K8s cluster

There's no public deployment - I built this for my own use and for fun. But if you'd like to run it yourself, go ahead!

## What does it do?

- 📰 Downloads daily comic strips from GoComics and ComicsKingdom
- 💾 Caches images locally with automatic 7-day cleanup
- 🎨 Displays them in a clean, ad-free interface
- 📱 Works great on mobile, tablet, and desktop
- ⚡ Virtual scrolling for smooth browsing
- 📊 Tracks metrics on cache performance and storage

## Quick Start

### Backend (comic-api)

```bash
# Build and run
./gradlew :comic-api:build
./gradlew :comic-api:bootRun

# Run tests
./gradlew :comic-api:test
./gradlew :comic-api:integrationTest

# View API docs at http://localhost:8080/swagger-ui/index.html
```

### Frontend (comic-web)

```bash
cd comic-web
npm install
npm start

# Visit http://localhost:4200
```

## Deployment

I deploy this to my home K8s cluster using Docker and Helm:

```bash
# Build backend
./gradlew :comic-api:build
./comic-api/build-docker.sh 2.1.35

# Build frontend
cd comic-web
./build-docker.sh

# Deploy with Helm
helm upgrade comics comics
```

## API & Docs

When running locally:
- Web App: http://localhost:4200
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- REST API: http://localhost:8080/api/v1/comics
- Metrics: http://localhost:8080/api/v1/metrics

Full endpoint documentation in the [docs/](docs/) directory.

## Development

For detailed architecture, build instructions, module structure, testing guidelines, and coding standards, see [Antigravity.md](Antigravity.md).