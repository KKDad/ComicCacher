# Storage Details for ComicCacher

This document outlines the JSON files used for data storage in the ComicCacher application. The application primarily uses JSON files for configuration and persistence.

## Overview of Storage Files

| File Name | Purpose | Location | Read/Written By |
|-----------|---------|----------|----------------|
| ComicCacher.json | Bootstrap configuration for comics | /ComicAPI/src/main/resources/ | CacherConfigLoader |
| comics.json | Comic metadata and cache state | ./comics.json (configurable) | JsonConfigWriter |
| users.json | User accounts and authentication | ./users.json (configurable) | UserConfigWriter |
| preferences.json | User favorites and settings | ./preferences.json (configurable) | PreferenceConfigWriter |
| task-executions.json | Scheduled task tracking | Cache directory | TaskExecutionTrackerImpl |
| stats.db | Comic image cache statistics | Inside comic image directories | ImageCacheStatsUpdater |
| openapi.json | API documentation | /docs/ | Generated during build |

## Detailed Schema Information

### 1. ComicCacher.json

Bootstrap configuration defining which comics to download and their start dates.

**Schema:**
```json
{
  "dailyComics": [
    {
      "name": "String",
      "startDate": {
        "year": "Integer",
        "month": "Integer",
        "day": "Integer"
      }
    }
  ],
  "kingComics": [
    {
      "name": "String", 
      "website": "String",
      "startDate": {
        "year": "Integer",
        "month": "Integer",
        "day": "Integer"
      }
    }
  ]
}
```

**Purpose:** 
- `dailyComics`: Comics from GoComics
- `kingComics`: Comics from ComicsKingdom
- `startDate`: The earliest date to download for each comic

### 2. comics.json

Stores information about available comics and their metadata.

**Schema:**
```json
{
  "items": {
    "[hash_of_comic_name]": {
      "id": "Integer",
      "name": "String",
      "author": "String",
      "oldest": "LocalDate",
      "newest": "LocalDate",
      "enabled": "Boolean",
      "description": "String",
      "avatarAvailable": "Boolean"
    }
  }
}
```

**Purpose:** 
- `items`: Map of comic items keyed by name hash
- Each comic contains metadata including date range and enabled status
- Tracks which comics are available in the cache

**Location:** Configured via `comics.cache.config` property (defaults to ./comics.json)

### 3. users.json

Stores user account information and authentication data.

**Schema:**
```json
{
  "users": {
    "[username]": {
      "username": "String",
      "passwordHash": "String",
      "email": "String",
      "displayName": "String",
      "created": "LocalDateTime",
      "lastLogin": "LocalDateTime",
      "roles": ["String"],
      "userToken": "UUID"
    }
  }
}
```

**Purpose:**
- `users`: Map of user objects keyed by username
- Stores credentials, profile information, and security roles
- Tracks account creation and login dates

**Location:** Configured via `comics.cache.usersConfig` property (defaults to ./users.json)

**Validation:** Username and password hash are required fields

### 4. preferences.json

Stores user preferences, favorite comics, and reading history.

**Schema:**
```json
{
  "preferences": {
    "[username]": {
      "username": "String",
      "favoriteComics": ["Integer"],
      "lastReadDates": {
        "[comicId]": "LocalDate"
      },
      "displaySettings": {
        "[settingName]": "Object"
      }
    }
  }
}
```

**Purpose:**
- `preferences`: Map of user preferences keyed by username
- `favoriteComics`: List of comic IDs marked as favorites
- `lastReadDates`: Tracks which comics were read and when
- `displaySettings`: User-specific display configuration

**Location:** Configured via `comics.cache.preferencesConfig` property (defaults to ./preferences.json)

### 5. task-executions.json

Tracks when scheduled tasks were last executed.

**Schema:**
```json
{
  "[taskName]": "LocalDate"
}
```

**Purpose:**
- Simple map of task names to their last execution date
- Prevents tasks from running more than once per scheduled period
- Used by DailyRunner and StartupReconciler's scheduled daily reconciliation

**Location:** Stored in the cache directory (configured via `cache.location`)

### 6. stats.db

Stores cache statistics for comic images despite the .db extension.

**Schema:**
```json
{
  "oldestImage": "String",
  "newestImage": "String",
  "years": ["String"],
  "totalStorageBytes": "long",
  "perComicMetrics": {
    "[comicName]": {
      "comicName": "String",
      "storageBytes": "long",
      "imageCount": "int",
      "averageImageSize": "double",
      "mostRecentAccess": "String",
      "accessCount": "int",
      "hitRatio": "double",
      "storageByYear": {
        "[year]": "long"
      },
      "downloadTime": "long"
    }
  }
}
```

**Purpose:**
- Tracks storage metrics for the comic image cache
- Provides access statistics and hit ratios
- Breaks down storage by comic and year

**Location:** Inside each comic's image directory under the cache location

### 7. openapi.json

OpenAPI documentation for the REST API.

**Schema:** Standard OpenAPI 3.0 specification

**Purpose:**
- Documents API endpoints, parameters, and responses
- Generated during the build process
- Used for API documentation and client generation

**Location:** /docs/openapi.json