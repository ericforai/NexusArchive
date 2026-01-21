# Tech Stack: NexusArchive

## 1.0 Core Languages & Runtimes
- **Backend**: Java 17 (OpenJDK)
- **Frontend**: TypeScript (ESNext)
- **Runtime**: Node.js 18+ (for development/build)

## 2.0 Backend Stack
- **Framework**: Spring Boot 3.1.6
- **ORM/Data Access**: MyBatis-Plus 3.5.7
- **Database Migration**: Flyway
- **Security**: Spring Security 6.x
- **Authentication**: JWT (io.jsonwebtoken:jjwt 0.12.3)
- **Cryptography**: BouncyCastle (SM2/SM3/SM4 support), Argon2 (Password hashing)
- **Utilities**: Lombok, Hutool, Jackson, Apache Commons, Guava
- **API Documentation**: SpringDoc OpenAPI (Swagger UI) 2.3.0

## 3.0 Frontend Stack
- **Framework**: React 19
- **Build Tool**: Vite 6
- **UI Library**: Ant Design (antd) 6.x
- **Styling**: Tailwind CSS, Lucide React (Icons)
- **State Management**: 
  - **Server State**: TanStack Query (React Query) v5
  - **Client State**: Zustand
- **Routing**: React Router DOM v6
- **Data Fetching**: Axios
- **File Handling**: PDF.js, React-PDF, OFDRW (via backend)

## 4.0 Storage & Middleware
- **Relational Database**: PostgreSQL
- **Caching/Session**: Redis
- **Object Storage**: Local file system (structured via database records)

## 5.0 Quality & Infrastructure
- **Testing**:
  - **Backend**: JUnit 5, Mockito, ArchUnit (Architecture Testing)
  - **Frontend**: Vitest, React Testing Library
  - **E2E**: Playwright
- **Linting & Quality**: ESLint, TypeScript (tsc), Dependency Cruiser
- **Deployment**: Docker, Docker Compose
- **Version Control**: Git (Hybrid Monorepo structure)