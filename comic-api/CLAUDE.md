# ComicAPI Coding Standards

## 1. Documentation & Storage
- **Source of Truth:** All comic metadata resides in JSON files on the NFS filesystem. Treat the filesystem as a "Read-Through Cache." Refer to `STORAGE_DETAILS.md` for schema.
- **No Database:** Do not implement JPA, Hibernate, or any relational database logic.
- **NFS I/O Safety:** Use `java.nio.file.Path` over `java.io.File`.
- **Atomic Writes:** When updating JSON metadata, use a "write-to-temp-then-move" pattern to prevent corruption on NFS during network hiccups.
- **Hybrid API:**
  - **GraphQL** for all metadata queries and mutations.
  - **REST** exclusively for binary data streaming (e.g., serving `.jpg` or `.webp` pages).

## 2. Import Standards
- **Never use star/wildcard imports** (e.g., `import com.package.*` or `import static ...*`).
- **Import Collapse:** Set "Class count to use import on demand" and "Names count" to **999**.
- **Import Order (alphabetical within groups):**
  1. Static imports (no wildcards)
  2. `com.*`
  3. `org.*`
  4. `java.*`
  5. `javax.*` / `jakarta.*`
  6. All other imports

## 3. Formatting & Layout
- **Line Length:** Hard wrap at **190 characters**.
- **Chained Method Calls:** Always wrap on the dot (`.`).
- **Binary Operations:** Wrap if long; operator (`&&`, `||`, `+`) must be first character on the new line.
- **Control Statements:** Braces `{}` mandatory for all `if`, `else`, `for`, `while`. No single-line statements.
- **Spacing:** 2 blank lines around class definitions. No blank lines before first import. Up to 3 blank lines inside methods.
- **Indentation:** Smart Tabs.

## 4. Enum Standards
- **Naming:** Singular form (e.g., `ComicType`). Values in `ALL_CAPS` with `ADJECTIVE_NOUN` order.
- **Layout:** Alphabetical entries, each on its own line. Methods and constructors after the final value.
- **Logic:** Implement `toString()` for values displayed in GQL schema or UI.

## 5. Javadoc & Comments
- Document all **public** classes, interfaces, and methods.
- **Do not** document variables or fields. **Do not** use `@return` or `@param` tags.
- Use `{@inheritDoc}` for overridden methods. Use `//` for internal notes.

## 6. Java & Collections Style
- Prefer `record` types for GQL DTOs and JSON metadata mapping.
- Use `Map.of()`, `List.of()`, `Set.of()` for small/fixed collections.
- Use `new ArrayList<>(List.of(...))` if the collection must be modifiable.
- Prefer `.computeIfAbsent()` over `containsKey()` + `put()`.
- Leverage Virtual Threads for I/O-bound NFS reads.

## 7. GraphQL Implementation
- **Schema-First:** Update `.graphqls` schema before modifying Resolvers.
- **Cursor-based Pagination:** Relay-style (`edges`/`node`) for all comic lists. No offset-based pagination.
- **N+1 Prevention:** Use `DataLoader` for all nested metadata lookups from JSON files.
- **Scalars:** Custom scalars for `Date` and `FilePath`.
- **Mutations:** Return a "Payload" object containing the updated object and a list of user-friendly errors.
- **Binary Data:** GQL handles metadata only. Binary streams stay on REST using `FileSystemResource`.
- **Authorization:** Three roles — `USER` (default), `OPERATOR` (batch/metrics read-only), `ADMIN` (full access). Schema directives: `@public`, `@authenticated`, `@hasRole(role: "ROLE")`.

## 8. JSON Serialization (Gson)
- **Gson only.** Do not use Jackson annotations (`@JsonProperty`, `@JsonFormat`).
- Use `GsonBuilder().registerTypeAdapterFactory(new RecordAdapterFactory())` for Records.
- Use `@SerializedName` if JSON key differs from Java field name.
- Use `@Qualifier("gsonWithLocalDate")` bean for date-time serialization.

## 9. Lombok Usage
- `@ToString(onlyExplicitlyIncluded = true)` with `@ToString.Include` on identifier/logging fields.
- Never include sensitive NFS paths or user data in `toString` output.
- Prefer `@Builder` for complex engine-level objects that are not Records.

## 10. Ordering Standards
Alphabetical ordering required for: enum entries, import statements, class members (Constants > Fields > Constructors > Methods), properties in `.yml`/`.properties` files.

## 11. Quality Enforcement
- Run `./gradlew clean checkstyleMain` before every commit.
- Use `./gradlew rewriteRun` for trivial auto-fixes (imports, spacing, formatting).
- Zero new warnings in any PR. Clean up legacy warnings when modifying a file.

## 12. Further Reading
See [docs/](../docs/README.md) for full API reference, design decisions, and storage specifications.
