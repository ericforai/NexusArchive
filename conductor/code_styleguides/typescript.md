# TypeScript Code Style Guide

## 1. Typing
- Avoid `any`. Use `unknown` if the type is truly not known yet.
- Prefer `interface` for object definitions and `type` for unions/intersections.
- Enable `strict` mode in `tsconfig.json`.

## 2. Naming
- Use `PascalCase` for types, interfaces, and enums.
- Use `camelCase` for variables and functions.

## 3. Async/Await
- Prefer `async/await` over raw `.then()` chains.
- Always handle errors with `try/catch` or `.catch()`.

## 4. Null Checks
- Use optional chaining (`?.`) and nullish coalescing (`??`).