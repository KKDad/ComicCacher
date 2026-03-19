# Users & Preferences API

## Queries

### me

Get the current authenticated user's profile.

```graphql
query {
  me: User
}
```

**Auth:** `@authenticated`

**Returns:** `User` (null if not authenticated)

```graphql
query {
  me {
    username
    email
    displayName
    created
    lastLogin
    roles
  }
}
```

---

### preferences

Get the current authenticated user's preferences.

```graphql
query {
  preferences: UserPreference
}
```

**Auth:** `@authenticated`

**Returns:** `UserPreference`

```graphql
query {
  preferences {
    username
    favoriteComics
    lastReadDates {
      comicId
      date
    }
    displaySettings
  }
}
```

---

## Mutations

### updateProfile

Update the current user's profile. Only provided fields are updated.

```graphql
mutation {
  updateProfile(input: UpdateProfileInput!): UpdateProfilePayload!
}
```

**Auth:** `@authenticated`

**UpdateProfileInput fields:**

| Field | Type | Description |
|---|---|---|
| `email` | `String` | New email address |
| `displayName` | `String` | New display name |

**Returns:** `UpdateProfilePayload!` -- `{ user: User, errors: [UserError!]! }`

```graphql
mutation {
  updateProfile(input: { displayName: "New Name" }) {
    user {
      username
      displayName
      email
    }
    errors {
      message
      field
    }
  }
}
```

---

### updatePassword

Update the current user's password.

```graphql
mutation {
  updatePassword(newPassword: String!): UpdatePasswordPayload!
}
```

**Auth:** `@authenticated`

| Parameter | Type | Description |
|---|---|---|
| `newPassword` | `String!` | New password |

**Returns:** `UpdatePasswordPayload!` -- `{ success: Boolean!, errors: [UserError!]! }`

```graphql
mutation {
  updatePassword(newPassword: "n3wS3cureP@ss") {
    success
    errors {
      message
      code
    }
  }
}
```

---

### deleteAccount

Delete a user account. This action is irreversible.

```graphql
mutation {
  deleteAccount(username: String!): DeleteAccountPayload!
}
```

**Auth:** `@hasRole(role: "ADMIN")`

| Parameter | Type | Description |
|---|---|---|
| `username` | `String!` | Username of the account to delete |

**Returns:** `DeleteAccountPayload!` -- `{ success: Boolean!, errors: [UserError!]! }`

```graphql
mutation {
  deleteAccount(username: "inactive_user") {
    success
    errors {
      message
      code
    }
  }
}
```

---

### addFavorite

Add a comic to the user's favorites.

```graphql
mutation {
  addFavorite(comicId: Int!): FavoritePayload!
}
```

**Auth:** `@authenticated`

| Parameter | Type | Description |
|---|---|---|
| `comicId` | `Int!` | Comic ID to add |

**Returns:** `FavoritePayload!` -- `{ preference: UserPreference, errors: [UserError!]! }`

```graphql
mutation {
  addFavorite(comicId: 42) {
    preference {
      favoriteComics
    }
    errors {
      message
    }
  }
}
```

---

### removeFavorite

Remove a comic from the user's favorites.

```graphql
mutation {
  removeFavorite(comicId: Int!): FavoritePayload!
}
```

**Auth:** `@authenticated`

| Parameter | Type | Description |
|---|---|---|
| `comicId` | `Int!` | Comic ID to remove |

**Returns:** `FavoritePayload!` -- `{ preference: UserPreference, errors: [UserError!]! }`

```graphql
mutation {
  removeFavorite(comicId: 42) {
    preference {
      favoriteComics
    }
    errors {
      message
    }
  }
}
```

---

### updateLastRead

Update the last read date for a comic.

```graphql
mutation {
  updateLastRead(comicId: Int!, date: Date!): UpdateLastReadPayload!
}
```

**Auth:** `@authenticated`

| Parameter | Type | Description |
|---|---|---|
| `comicId` | `Int!` | Comic ID |
| `date` | `Date!` | Last read date (YYYY-MM-DD) |

**Returns:** `UpdateLastReadPayload!` -- `{ preference: UserPreference, errors: [UserError!]! }`

```graphql
mutation {
  updateLastRead(comicId: 42, date: "2026-03-19") {
    preference {
      lastReadDates {
        comicId
        date
      }
    }
    errors {
      message
    }
  }
}
```

---

### updateDisplaySettings

Update display settings (theme, layout, etc.).

```graphql
mutation {
  updateDisplaySettings(settings: JSON!): UpdateDisplaySettingsPayload!
}
```

**Auth:** `@authenticated`

| Parameter | Type | Description |
|---|---|---|
| `settings` | `JSON!` | Arbitrary JSON object with display settings |

**Returns:** `UpdateDisplaySettingsPayload!` -- `{ preference: UserPreference, errors: [UserError!]! }`

```graphql
mutation {
  updateDisplaySettings(settings: { theme: "dark", layout: "grid", stripsPerPage: 10 }) {
    preference {
      displaySettings
    }
    errors {
      message
    }
  }
}
```

---

## Types

### User

| Field | Type | Description |
|---|---|---|
| `username` | `String!` | Unique username |
| `email` | `String` | Email address |
| `displayName` | `String` | Display name |
| `created` | `DateTime` | Account creation timestamp |
| `lastLogin` | `DateTime` | Last login timestamp |
| `roles` | `[String!]!` | Assigned roles: `USER`, `OPERATOR`, `ADMIN` |

### UserPreference

| Field | Type | Description |
|---|---|---|
| `username` | `String!` | Username this preference belongs to |
| `favoriteComics` | `[Int!]!` | List of favorite comic IDs |
| `lastReadDates` | `[LastReadEntry!]!` | Last read dates per comic |
| `displaySettings` | `JSON` | Display settings as key-value pairs |

### LastReadEntry

| Field | Type | Description |
|---|---|---|
| `comicId` | `Int!` | Comic ID |
| `date` | `Date!` | Last read date |
