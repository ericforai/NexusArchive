# Java Code Style Guide

## 1. Naming Conventions
- **Classes**: `PascalCase` (e.g., `UserService`)
- **Methods**: `camelCase` (e.g., `getUserById`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_COUNT`)
- **Packages**: `lowercase` (e.g., `com.nexusarchive.service`)

## 2. Formatting
- Use 4 spaces for indentation.
- Limit lines to 120 characters.
- Use K&R style for braces (opening brace on the same line).

## 3. Best Practices
- **Lombok**: Use `@Data`, `@Builder`, `@AllArgsConstructor` to reduce boilerplate.
- **Streams**: Prefer Java Streams for collection processing.
- **Optional**: Use `Optional` to handle potential null values gracefully.
- **Exceptions**: Use custom runtime exceptions for business logic errors.

## 4. Documentation
- Use Javadoc for public methods and classes.
- Add comments explaining *why*, not *what*.
