# ComicAPI Coding Standards

## Import Standards
- **NEVER use star/wildcard imports** (e.g., `import com.package.*`)
- **Set class count to use import on demand**: 50
- **Set names count to use import on demand**: 150
- **Import Order**:
  1. Static imports (never use star imports for static imports)
  2. com.*
  3. org.*
  4. java.*
  5. javax.*
  6. All other imports

## Code Formatting
- Annotation parameters: Wrap if long, align parameters
- No first column comments
- No control statements in one line
- Keep up to 3 blank lines in declarations, code, and before closing braces
- No blank lines before imports
- 2 blank lines around classes
- Method call parameters: Always wrap
- Method parameters: Wrap if long
- Resource lists: Always wrap
- Extends/implements lists: Wrap if long
- Throws lists: Wrap if long 
- Method call chains: Wrap if long, wrap first call
- Binary operations: Wrap if long, put operator on next line
- Ternary operations: Always wrap
- Array initializers: Wrap if long
- Assignments: Wrap if long
- Parameter annotations: Wrap if long
- Variable annotations: Always wrap
- Enum constants: Always wrap
- Enable smart tabs for indentation

## Java Collections Style
- Prefer factory methods for collections:
  - `Map.of()` instead of `new HashMap<>()`
  - `List.of()` instead of `new ArrayList<>()`
  - `Set.of()` instead of `new HashSet<>()`
- For small, fixed-size collections (up to ~10 elements), use: `Map.of(k1, v1, k2, v2)`, `List.of(e1, e2, e3)`, etc.
- For larger or modifiable collections: `new HashMap<>(Map.of(...))`, `new ArrayList<>(List.of(...))`
- For concurrent collections: `ConcurrentHashMap.newKeySet()`, `Collections.synchronizedList(List.of(...))`
- Prefer `computeIfAbsent()` over `containsKey()` + `put()` pattern

## Ordering Standards
- Maintain alphabetical ordering for:
  - Enum entries
  - Import statements (within each group)
  - Class members/constants
  - Method parameters (new overloads)
  - Properties in configuration files
- Exceptions for semantic grouping or inherent logical order

## Enum Standards
- Use singular form for enum class names
- Use ALL_CAPS for enum values
- Use adjective-noun order for enum values
- Arrange enum entries alphabetically
- Place enum values on separate lines
- Maintain consistent naming patterns
- Add documentation for non-obvious enum values
- Place methods after all enum values
- Implement `toString()` for serialized or displayed enums

## Lombok Usage
- Use `@ToString` with `onlyExplicitlyIncluded=true`
- Mark fields with `@ToString.Include` individually
- Include identifier fields or crucial logging fields
- No sensitive data in toString output
- Consistent order for Lombok annotations
- Prefer `@Builder` for complex objects
- Use `@EqualsAndHashCode` carefully

## Javadoc Standards
- Document all public classes, interfaces, and methods
- DO NOT document variables/fields
- DO NOT use `@return` tags 
- DO NOT use `@param` tags
- Use descriptive summaries explaining purpose
- Document exceptions with `@throws` only when needed
- Include examples for complex APIs
- Use `{@inheritDoc}` for overridden methods
- Keep Javadoc concise
- Use regular comments for implementation details

## Code Quality Requirements
- Apply all code style preferences consistently
- Follow existing patterns when modifying code
- Use appropriate exception handling
- Ensure files end with a single newline
- Check README.md for updates/typos before commits
- Focus README updates on user-facing information
- Perform comprehensive code quality checks:
  - Code style and formatting
  - Import ordering
  - Documentation quality
  - Error handling
  - Test quality standards