# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

**NexusArchive** - Electronic Accounting Archive Management System (电子会计档案管理系统)

| Layer | Technology | Location |
|-------|------------|----------|
| Backend | Spring Boot 3.1.6, Java 17, MyBatis-Plus 3.5.7 | `nexusarchive-java/` |
| Frontend | React 19, TypeScript 5.8, Vite 6, Ant Design 6 | `src/` |
| Database | PostgreSQL (primary), DM8 (optional) | Flyway migrations |
| Cache | Redis | Spring Data Redis |
| Auth | JWT (jjwt 0.12.3), Spring Security | `security/` package |

## Repository Layout

```
nexusarchive/
├── nexusarchive-java/              # Backend (Maven project)
│   ├── pom.xml                     # Maven dependencies
│   ├── src/main/java/com/nexusarchive/
│   │   ├── controller/             # REST endpoints
│   │   ├── service/                # Business logic
│   │   │   └── impl/               # Service implementations
│   │   ├── mapper/                 # MyBatis-Plus mappers
│   │   ├── entity/                 # JPA/MyBatis entities
│   │   ├── dto/                    # Request/Response DTOs
│   │   ├── config/                 # Spring configuration
│   │   ├── security/               # Auth & JWT
│   │   ├── common/                 # Shared utilities
│   │   └── integration/            # External system integrations
│   └── src/main/resources/
│       ├── application.yml         # Main config
│       ├── application-dev.yml     # Dev profile
│       └── db/migration/           # Flyway SQL scripts
├── src/                            # Frontend (React/Vite)
│   ├── components/                 # React components
│   ├── api/                        # API client (axios)
│   ├── features/                   # Feature modules
│   ├── hooks/                      # Custom React hooks
│   ├── store/                      # Zustand state
│   ├── routes/                     # React Router config
│   ├── utils/                      # Utility functions
│   └── __tests__/                  # Vitest tests
├── package.json                    # Frontend dependencies
├── vite.config.ts                  # Vite configuration
├── tsconfig.json                   # TypeScript config
├── docker-compose.infra.yml        # Infrastructure (DB + Redis)
└── docker-compose.app.yml          # Application services (server)
```

## Development Commands

### Backend (run from `nexusarchive-java/`)

```bash
# Build
mvn clean compile                   # Compile
mvn clean package -DskipTests       # Package JAR (skip tests)
mvn clean package                   # Package JAR (with tests)

# Run
mvn spring-boot:run                 # Start dev server
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # With dev profile

# Test
mvn test                            # Run all tests
mvn test -Dtest=ClassName           # Run specific test class
mvn test -Dtest=ClassName#methodName # Run specific test method

# Dependencies
mvn dependency:tree                 # Show dependency tree
mvn versions:display-dependency-updates  # Check for updates
```

### Frontend (run from project root)

```bash
# Install
npm install                         # Install dependencies

# Development
npm run dev                         # Start Vite dev server (port 5173)
npm run build                       # Production build to dist/
npm run preview                     # Preview production build

# Test
npm run test                        # Run Vitest in watch mode
npm run test:run                    # Run tests once
npm run test:coverage               # Run with coverage report
```

### Docker

```bash
# Development (DB + Redis only)
docker-compose -f docker-compose.infra.yml up -d

# Production (DB + Redis + Backend + Frontend)
docker-compose -f docker-compose.infra.yml \
               -f docker-compose.app.yml \
               --env-file .env.server up -d
```

### NPM Scripts (project root)

```bash
npm run dev        # Start development environment
npm run dev:stop   # Stop development environment
npm run db:dump    # Export database to db/seed-data.sql
npm run db:load    # Import database from db/seed-data.sql
npm run db:reset   # Reset database (drop volume and reinit)
npm run deploy     # Deploy to server
```

## Code Conventions

### Backend (Java)

- **Package structure**: `com.nexusarchive.<layer>` (controller, service, mapper, entity, dto)
- **Naming**: PascalCase for classes, camelCase for methods/variables
- **Entities**: Use Lombok (`@Data`, `@Builder`), MyBatis-Plus annotations
- **Services**: Interface in `service/`, implementation in `service/impl/`
- **DTOs**: Separate request/response DTOs, use validation annotations
- **Exceptions**: Custom exceptions in `common/exception/`, global handler in config

### Frontend (TypeScript/React)

- **Components**: Functional components with hooks, PascalCase filenames
- **State**: Zustand for global state, React Query for server state
- **API calls**: Centralized in `src/api/`, use axios instance
- **Styling**: Ant Design components + Tailwind utilities
- **Tests**: Colocate in `__tests__/` directories, use Testing Library

## Key Configuration Files

| File | Purpose | Caution Level |
|------|---------|---------------|
| `application.yml` | Spring Boot config | HIGH - contains DB/Redis connection |
| `application-*.yml` | Profile-specific config | HIGH |
| `pom.xml` | Maven dependencies | MEDIUM - version changes can break build |
| `db/migration/*.sql` | Flyway migrations | HIGH - never modify existing migrations |
| `package.json` | Frontend dependencies | MEDIUM |
| `vite.config.ts` | Build configuration | LOW |

## Safe Change Guidelines

1. **Explain before changing**: Describe what you plan to modify and why
2. **Small diffs preferred**: Make incremental changes, not sweeping refactors
3. **Never modify existing Flyway migrations**: Create new migration files instead
4. **Test after changes**: Run `mvn test` / `npm run test:run` to verify
5. **Preserve existing patterns**: Follow conventions already in the codebase
6. **No mass refactors**: Avoid renaming across many files without explicit request

## Common Issues

### Backend won't start
1. Check PostgreSQL is running: `docker ps | grep postgres`
2. Verify `application.yml` datasource config
3. Check Redis connection if caching enabled

### Frontend build fails
1. Clear node_modules: `rm -rf node_modules && npm install`
2. Check TypeScript errors: `npx tsc --noEmit`
3. Verify import paths match actual file locations

### Tests fail
- Backend: Check `application-test.yml` config, H2 in-memory DB used for tests
- Frontend: Check jsdom environment in vitest config
