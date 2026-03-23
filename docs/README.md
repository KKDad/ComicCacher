# ComicCacher Documentation

## API Reference

GraphQL-first API with JWT authentication, three roles (USER, OPERATOR, ADMIN), and two supplementary REST endpoints.

| Document | Description |
|----------|-------------|
| [@~/docs/api/overview.md](api/overview.md) | Architecture, auth model, scalars, pagination, error handling |
| [@~/docs/api/comics.md](api/comics.md) | Comic queries, mutations, and REST image endpoints |
| [@~/docs/api/auth.md](api/auth.md) | Registration, login, JWT lifecycle |
| [@~/docs/api/users.md](api/users.md) | Profile management and display preferences |
| [@~/docs/api/batch-jobs.md](api/batch-jobs.md) | Job execution, scheduling, and log queries |
| [@~/docs/api/retrieval-status.md](api/retrieval-status.md) | Download tracking and history |
| [@~/docs/api/metrics.md](api/metrics.md) | Storage and access metrics |
| [@~/docs/api/health.md](api/health.md) | Health check and error codes |

## Design

Architecture decisions, data flows, and internal patterns.

| Document | Description |
|----------|-------------|
| [@~/docs/design/architecture.md](design/architecture.md) | Module graph, key classes, facade pattern |
| [@~/docs/design/download-pipeline.md](design/download-pipeline.md) | End-to-end download, validate, dedup, store flow |
| [@~/docs/design/downloader-strategies.md](design/downloader-strategies.md) | Strategy hierarchy, daily vs. indexed comics, adding new sources |
| [@~/docs/design/batch-jobs.md](design/batch-jobs.md) | Scheduler framework, job configs, execution tracking |
| [@~/docs/design/image-validation.md](design/image-validation.md) | 3-layer validation, dedup, and analysis pipeline |
| [@~/docs/design/adding-batch-jobs.md](design/adding-batch-jobs.md) | Developer guide for new job creation |

## Storage

File-based persistence layer on NFS.

| Document | Description |
|----------|-------------|
| [@~/docs/storage/overview.md](storage/overview.md) | Directory layout, naming conventions, atomic writes |
| [@~/docs/storage/configuration-files.md](storage/configuration-files.md) | comics.json, users.json, preferences.json, bootstrap |
| [@~/docs/storage/operational-state.md](storage/operational-state.md) | Batch executions, retrieval status, scheduler state, metrics |
| [@~/docs/storage/comic-data.md](storage/comic-data.md) | Strip images, date indexes, hash caches, metadata sidecars |
