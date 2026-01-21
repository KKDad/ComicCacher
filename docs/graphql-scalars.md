# GraphQL Custom Scalars

ComicCacher's GraphQL API uses custom scalar types to handle specific data formats.

## Available Scalars

### Date

Represents a calendar date without time.

**Format:** ISO-8601 date (`yyyy-MM-dd`)

**Examples:**
```json
"2024-01-15"
"2023-12-25"
"2025-03-01"
```

**Usage in Schema:**
```graphql
type Comic {
  oldest: Date
  newest: Date
}

type ComicStrip {
  date: Date!
}
```

---

### DateTime

Represents a timestamp with timezone information.

**Format:** ISO-8601 datetime with offset (`yyyy-MM-dd'T'HH:mm:ssZ`)

**Examples:**
```json
"2024-01-15T10:30:00Z"
"2024-01-15T15:45:30-05:00"
"2024-01-15T20:00:00+00:00"
```

**Usage in Schema:**
```graphql
type RetrievalRecord {
  retrievalTime: DateTime!
}
```

---

### JSON

Represents arbitrary JSON data. Used for flexible/dynamic fields.

**Format:** Any valid JSON value (object, array, string, number, boolean, null)

**Examples:**
```json
{"key": "value", "nested": {"count": 42}}
["item1", "item2", "item3"]
"simple string"
123
true
null
```

**Usage in Schema:**
```graphql
type HealthStatus {
  details: JSON
}
```

---

## Input Handling

When providing scalar values as GraphQL variables, ensure proper formatting:

```json
{
  "date": "2024-01-15",
  "timestamp": "2024-01-15T10:30:00Z",
  "metadata": {"source": "api", "version": 2}
}
```

## Validation

- **Date**: Must be a valid calendar date. Invalid dates like `"2024-02-30"` will be rejected.
- **DateTime**: Must include timezone information. Naive timestamps are not accepted.
- **JSON**: Must be syntactically valid JSON. Malformed JSON will cause a parsing error.

## Implementation

These scalars are implemented using `graphql-java-extended-scalars`. The configuration is in `GraphQLConfig.java`:

```java
@Bean
public RuntimeWiringConfigurer runtimeWiringConfigurer() {
    return wiringBuilder -> wiringBuilder
        .scalar(ExtendedScalars.Date)
        .scalar(ExtendedScalars.DateTime)
        .scalar(ExtendedScalars.Json);
}
```
