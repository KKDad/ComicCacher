# ComicAPI Coding Standards (Antigravity 2026)

## 1. Documentation & Storage
- **Source of Truth:** All comic metadata resides in JSON files on the NFS filesystem. Treat the filesystem as a "Read-Through Cache." Refer to `STORAGE_DETAILS.md` for schema.
- **No Database:** Do not implement JPA, Hibernate, or any relational database logic.
- **NFS I/O Safety:** Mandate the use of `java.nio.file.Path` over `java.io.File`.
- **Atomic Writes:** When updating JSON metadata, use a "write-to-temp-then-move" pattern to prevent corruption on NFS during network hiccups.
- **Hybrid API:** - Use **GraphQL** for all metadata queries and mutations.
  - Use **REST** exclusively for binary data streaming (e.g., serving `.jpg` or `.webp` pages).

## 2. Import Standards
- **NEVER use star/wildcard imports** (e.g., `import com.package.*` or `import static ...*`).
- **Import Collapse:** Set "Class count to use import on demand" and "Names count" to **999** to ensure imports are always explicit.
- **Import Order (Alphabetical within groups):**
  1. Static imports (No wildcards)
  2. `com.*`
  3. `org.*`
  4. `java.*`
  5. `javax.*` / `jakarta.*`
  6. All other imports

## 3. Formatting & Layout
- **Line Length:** Hard wrap at **190 characters**.
- **Chained Method Calls:** Always wrap on the dot (`.`).
Example:
```java
  return comicRepository.findAll()
      .stream()
      .filter(c -> c.getYear() > 2020)
      .toList();
```
- **Binary Operations:** Wrap if long; the operator (e.g., `&&`, `||`, `+`) must be the first character on the new line.
- **Control Statements:** Braces `{}` are **mandatory** for all `if`, `else`, `for`, and `while` blocks. No single-line statements.
- **Spacing:**
  - **2 blank lines** around class definitions.
  - **No blank lines** before the first import statement.
  - Up to **3 blank lines** allowed inside methods to separate logical groups.
- **Indentation:** Use **Smart Tabs**.

## 4. Enum Standards
- **Naming:** Use **singular form** for enum class names (e.g., `ComicType`).
- **Values:** Use `ALL_CAPS` with `ADJECTIVE_NOUN` order.
- **Layout:**
  - Arrange entries **alphabetically**.
  - Place each value on its own line.
  - Place methods and constructors **after** the final enum value.
- **Logic:** Implement `toString()` for values displayed in the GQL schema or UI.

## 5. Javadoc & Comments
- **Scope:** Document all **public** classes, interfaces, and methods.
- **Restrictions:**
  - **DO NOT** document variables or fields.
  - **DO NOT** use `@return` or `@param` tags (provide description in the summary instead).
- **Inheritance:** Use `{@inheritDoc}` for overridden methods.
- **Formatting:** No first-column comments; use `//` for internal implementation notes.

## 6. Java 25 & Collections Style
- **Records:** Prefer `record` types for GQL DTOs and JSON metadata mapping.
- **Collection Factories:**
  - Use `Map.of()`, `List.of()`, and `Set.of()` for small/fixed collections.
  - Use `new ArrayList<>(List.of(...))` if the collection must be modifiable.
  - Prefer `.computeIfAbsent()` over `containsKey()` + `put()`.
- **NFS I/O:** Leverage **Virtual Threads** (Spring Boot 4 default patterns) to handle I/O-bound NFS reads to prevent thread exhaustion.

## 7. GraphQL (GQL) Implementation
- **Schema-First Development:** The `.graphqls` schema file must be updated before generating or modifying Resolvers.
- **Cursor-based Pagination:** Mandate Relay-style cursor pagination (`edges`/`node`) for all comic lists. Offset-based pagination is prohibited for performance reasons on NFS.
- **N+1 Prevention:** Mandate the use of `DataLoader` for all nested metadata lookups from JSON files to batch NFS I/O.
- **Scalars:** Use custom scalars for `Date` and `FilePath` to ensure validation before hitting the NFS.
- **Mutations:** All mutations must return a "Payload" object containing the updated object and a list of user-friendly errors.
- **Binary Data (REST Exception):** GQL handles metadata only. Binary streams (comic page images) must remain on REST using `FileSystemResource` to leverage Spring's optimized byte-range support.

## 8. JSON Serialization (Gson)
- **Gson Exclusive:** **DO NOT** use Jackson annotations (`@JsonProperty`, `@JsonFormat`).
- **Gson Record Support:** Use `GsonBuilder().registerTypeAdapterFactory(new RecordAdapterFactory())` when using Records for metadata.
- **Mapping:** Use `@SerializedName` if the JSON key differs from the Java field name.
- **Date Handling:** Use the `@Qualifier("gsonWithLocalDate")` bean for all date-time serialization.

## 9. Lombok Usage
- **toString:** Use `@ToString(onlyExplicitlyIncluded = true)`. Mark specific identifier or logging fields with `@ToString.Include`.
- **Privacy:** Never include sensitive NFS paths or user data in `toString` output.
- **Builders:** Prefer `@Builder` for complex engine-level objects that are not Records.

## 10. Ordering Standards
- **Alphabetical Ordering** is required for:
  - Enum entries.
  - Import statements.
  - Class members (Constants > Fields > Constructors > Methods).
  - Properties in `.yml` or `.properties` configuration files.

  ## 11. Quality Enforcement & Checkstyle
- **Automated Validation:** Run `./gradlew clean checkstyleMain` before every commit.
- **Trivial Fixes:** Use `./gradlew rewriteRun` to automatically resolve trivial warnings (imports, spacing, simple formatting).
- **Complex Warnings:** Any non-trivial Checkstyle warnings that cannot be auto-fixed must be discussed with the team. 
- **Zero-Regression Policy:** There should be **zero new warnings** introduced in any Pull Request. Existing legacy warnings should be cleaned up whenever a file is modified.
