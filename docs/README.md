# ComicCacher Documentation

## API Reference

GraphQL-first API with JWT authentication, three roles (USER, OPERATOR, ADMIN), and two supplementary REST endpoints.

| Document | Description |
|----------|-------------|
| [Overview](api/overview.md) | Architecture, auth model, scalars, pagination, error handling |
| [Comics](api/comics.md) | Comic queries, mutations, and REST image endpoints |
| [Auth](api/auth.md) | Registration, login, JWT lifecycle |
| [Users](api/users.md) | Profile management and display preferences |
| [Batch Jobs](api/batch-jobs.md) | Job execution, scheduling, and log queries |
| [Retrieval Status](api/retrieval-status.md) | Download tracking and history |
| [Metrics](api/metrics.md) | Storage and access metrics |
| [Health](api/health.md) | Health check and error codes |

## Design

Architecture decisions, data flows, and internal patterns.

| Document | Description |
|----------|-------------|
| [Architecture](design/architecture.md) | Module graph, key classes, facade pattern |
| [Download Pipeline](design/download-pipeline.md) | End-to-end download, validate, dedup, store flow |
| [Batch Jobs](design/batch-jobs.md) | Scheduler framework, job configs, execution tracking |
| [Image Validation](design/image-validation.md) | 3-layer validation, dedup, and analysis pipeline |
| [Adding Batch Jobs](design/adding-batch-jobs.md) | Developer guide for new job creation |

## Storage

File-based persistence layer on NFS.

| Document | Description |
|----------|-------------|
| [Overview](storage/overview.md) | Directory layout, naming conventions, atomic writes |
| [Configuration Files](storage/configuration-files.md) | comics.json, users.json, preferences.json, bootstrap |
| [Operational State](storage/operational-state.md) | Batch executions, retrieval status, scheduler state, metrics |
| [Comic Data](storage/comic-data.md) | Strip images, date indexes, hash caches, metadata sidecars |
