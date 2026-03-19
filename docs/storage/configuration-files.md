# Configuration Files

Four JSON files define the application's persistent configuration. Three are user-configurable via `CacheProperties` (prefix `comics.cache`); one is a read-only classpath resource.

## File Inventory

| File | Purpose | Property | Default | Responsible Class |
|:---|:---|:---|:---|:---|
| `ComicCacher.json` | Bootstrap comic list | N/A (classpath) | `src/main/resources/ComicCacher.json` | `ApplicationConfigurationFacade` |
| `comics.json` | Comic registry/metadata | `comics.cache.config` | `comics.json` | `JsonComicRepository` via `ConfigurationFacade` |
| `users.json` | User accounts | `comics.cache.usersConfig` | `users.json` | `JsonUserRepository` via `ConfigurationFacade` |
| `preferences.json` | User preferences | `comics.cache.preferencesConfig` | `preferences.json` | `JsonPreferenceRepository` via `ConfigurationFacade` |

All three configurable files are resolved relative to `comics.cache.location` by `ApplicationConfigurationFacade.getConfigFile()`.

---

## 1. ComicCacher.json (Bootstrap)

Read-only classpath resource loaded via `getClass().getClassLoader().getResourceAsStream("ComicCacher.json")`. Defines which comics to download and their starting dates. Not writable at runtime.

**DTO:** `Bootstrap` (`comic-common`)

```json
{
  "dailyComics": [
    {
      "stripName": "String",
      "startDate": "LocalDate (yyyy-MM-dd)",
      "source": "String (e.g., gocomics)",
      "sourceIdentifier": "String (e.g., calvinandhobbes)",
      "publicationDays": ["MONDAY", "TUESDAY", ...],
      "active": true
    }
  ],
  "kingComics": [
    {
      "stripName": "String",
      "startDate": "LocalDate (yyyy-MM-dd)",
      "source": "String (e.g., comicskingdom)",
      "sourceIdentifier": "String (e.g., beetle-bailey)",
      "publicationDays": null,
      "active": true
    }
  ]
}
```

Each entry implements `IComicsBootstrap`. `publicationDays` defaults to `null` (daily). `active` defaults to `true`.

---

## 2. comics.json (Comic Registry)

The authoritative registry of all known comics and their metadata. Keyed by comic ID (integer). Loaded once and cached in memory by `ApplicationConfigurationFacade`.

**DTO:** `ComicConfig` wrapping `Map<Integer, ComicItem>` (`comic-common`)

```json
{
  "items": {
    "42": {
      "id": 42,
      "name": "Calvin and Hobbes",
      "author": "Bill Watterson",
      "oldest": "1985-11-18",
      "newest": "2025-03-15",
      "enabled": true,
      "description": "String (nullable)",
      "avatarAvailable": true,
      "source": "gocomics",
      "sourceIdentifier": "calvinandhobbes",
      "publicationDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"],
      "active": false
    }
  }
}
```

### Field Reference

| Field | Type | Default | Description |
|:---|:---|:---|:---|
| `id` | `int` | -- | Unique comic identifier |
| `name` | `String` | -- | Display name |
| `author` | `String` | `null` | Author/artist name |
| `oldest` | `LocalDate` | `null` | Oldest cached strip date |
| `newest` | `LocalDate` | `null` | Newest cached strip date |
| `enabled` | `boolean` | `true` | Whether downloading is enabled |
| `description` | `String` | `null` | Comic description |
| `avatarAvailable` | `boolean` | `false` | Whether avatar.png exists |
| `source` | `String` | `null` | Source provider identifier |
| `sourceIdentifier` | `String` | `null` | Provider-specific comic slug |
| `publicationDays` | `List<DayOfWeek>` | `null` | Days comic publishes (null = daily) |
| `active` | `boolean` | `true` | Whether comic is actively publishing |

> **Note:** The `comics` list field on `ComicConfig` is marked `@JsonIgnore` and never serialized. Only the `items` map is persisted.

---

## 3. users.json (User Accounts)

Stores user accounts with hashed passwords. Keyed by username string.

**DTO:** `UserConfig` wrapping `Map<String, User>` (`comic-api`)

```json
{
  "users": {
    "admin": {
      "username": "admin",
      "passwordHash": "$2a$10$...",
      "email": "admin@example.com",
      "displayName": "Admin User",
      "created": "2025-01-15T10:30:00",
      "lastLogin": "2025-03-18T08:45:00",
      "roles": ["ADMIN", "OPERATOR", "USER"],
      "userToken": "550e8400-e29b-41d4-a716-446655440000"
    }
  }
}
```

### Field Reference

| Field | Type | Default | Description |
|:---|:---|:---|:---|
| `username` | `String` | -- | Unique login name (map key) |
| `passwordHash` | `String` | -- | BCrypt-hashed password |
| `email` | `String` | `null` | Email address |
| `displayName` | `String` | `null` | Display name |
| `created` | `LocalDateTime` | `now()` | Account creation timestamp |
| `lastLogin` | `LocalDateTime` | `null` | Last successful login |
| `roles` | `List<String>` | `[]` | Role assignments (ADMIN, OPERATOR, USER) |
| `userToken` | `UUID` | random | Stable user token |

---

## 4. preferences.json (User Preferences)

Stores per-user favorites and reading history. Keyed by username string.

**DTO:** `PreferenceConfig` wrapping `Map<String, UserPreference>` (`comic-api`)

```json
{
  "preferences": {
    "admin": {
      "username": "admin",
      "favoriteComics": [42, 17, 93],
      "lastReadDates": {
        "42": "2025-03-18",
        "17": "2025-03-15"
      },
      "displaySettings": {
        "theme": "dark",
        "stripWidth": 800
      }
    }
  }
}
```

### Field Reference

| Field | Type | Default | Description |
|:---|:---|:---|:---|
| `username` | `String` | -- | Username (map key) |
| `favoriteComics` | `List<Integer>` | `[]` | List of favorited comic IDs |
| `lastReadDates` | `Map<Integer, LocalDate>` | `{}` | Last read date per comic ID |
| `displaySettings` | `Map<String, Object>` | `{}` | Arbitrary display preferences |

---

## Key Source Files

| File | Module |
|:---|:---|
| `ApplicationConfigurationFacade.java` | `comic-api` |
| `JsonComicRepository.java` | `comic-api` |
| `JsonUserRepository.java` | `comic-api` |
| `JsonPreferenceRepository.java` | `comic-api` |
| `CacheProperties.java` | `comic-common` |
| `ComicConfig.java` / `ComicItem.java` | `comic-common` |
| `UserConfig.java` / `User.java` | `comic-api` |
| `PreferenceConfig.java` / `UserPreference.java` | `comic-api` |
| `Bootstrap.java` / `IComicsBootstrap.java` | `comic-common` |
