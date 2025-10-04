# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests (if implemented)
./gradlew test

# Apply code formatting (ktfmt Google style + ktlint)
./gradlew spotlessApply

# Check code formatting
./gradlew spotlessCheck

# Create production JAR (creates demo.jar)
./gradlew bootJar

# Check dependency updates
./gradlew dependencyUpdates

# Run single test (if needed)
./gradlew test --tests "*TestClassName*"

# Clean build
./gradlew clean build
```

## Architecture Overview

This is a Kotlin Spring Boot application (v3.5.6) using Java 21 and Kotlin 2.2.20-RC with a three-layer architecture:

1. **Controller Layer** (`src/main/kotlin/com/github/bestheroz/demo/controller/`)
   - RESTful APIs with OpenAPI/Swagger documentation
   - Coroutine support with `runBlocking` (Spring MVC)
   - Separate controllers for Admin, User, and Notice domains

2. **Service Layer** (`src/main/kotlin/com/github/bestheroz/demo/services/`)
   - Business logic implementation
   - Transaction management

3. **Repository Layer** (`src/main/kotlin/com/github/bestheroz/demo/repository/`)
   - Spring Data JPA repositories
   - Custom query methods

## Key Framework Components

The `standard` package contains reusable framework code:

- **Authentication** (`standard/common/authenticate/`): JWT-based auth with JwtAuthenticationFilter and JwtTokenProvider
- **Exception Handling** (`standard/common/exception/`): Centralized error handling with proper HTTP status codes
- **Base Entities** (`standard/common/domain/`): IdCreated, IdCreatedUpdated for audit fields with converters
- **Logging** (`standard/common/log/`): Custom Logger and TraceLogger with aspect support
- **DTOs** (`standard/common/dto/`): Common data transfer objects and response wrappers
- **Utilities** (`standard/common/util/`): DateUtils, PasswordUtil, LogUtils, EnvironmentUtils

## Authentication & Security

- **JWT Authentication**: Stateless JWT-based auth with BCrypt password encoding
- **Token Configuration**: 5min access token, 30min refresh token by default (1440min in local profile)
- **Authorization**: Method-level security with `@PreAuthorize` using authorities (ADMIN_VIEW, ADMIN_EDIT, USER_VIEW, USER_EDIT)
- **Public Endpoints**: Login endpoints, health checks, notice listing (read-only), check-login-id, renew-token
- **CORS**: Configured for `http://localhost:3000` with credentials support
- **Authentication Endpoints**:
  - Admin: `POST /api/v1/admins/login`, `GET /api/v1/admins/renew-token`, `DELETE /api/v1/admins/logout`
  - User: `POST /api/v1/users/login`, `GET /api/v1/users/renew-token`, `DELETE /api/v1/users/logout`
- **CurrentUser Injection**: Use `@CurrentUser operator: Operator` parameter to access authenticated user context
- **Security Filter**: JwtAuthenticationFilter validates JWT tokens before UsernamePasswordAuthenticationFilter

## Database

- MySQL with HikariCP connection pooling
- P6Spy for SQL query logging
- Soft delete pattern using `removed_flag` and `removed_at`
- UTC timezone for all timestamps
- Migration scripts in `/migration/` directory

## Configuration

- **Profiles**: local (dev), sandbox, qa, prod (Swagger disabled in prod)
- **JWT Configuration**: 5min access token, 30min refresh token by default (1440min in local)
- **Database**: MySQL with HikariCP connection pooling (3 pool size for local, 30 for prod)
- **Session Cookie**: Custom name `JSESSIONID_DEMO`
- **OpenAPI**: Enabled for all profiles except prod, available at `/swagger-ui.html`
- **P6Spy**: SQL query logging with multiline format

## Development Tips

1. **Running locally**: Default port 8000, Swagger UI at http://localhost:8000/swagger-ui.html
2. **Code style**: ktfmt Google style + ktlint with custom rules (wildcards enabled, max-line-length disabled)
3. **Main class**: `com.github.bestheroz.Application`
4. **Docker**: Dockerfile included for containerized deployment
5. **Error responses**: Standardized format via ApiExceptionHandler with custom exception classes (BadRequest400, Unauthorized401, Forbidden403, InternalServerError500, TooManyRequests429)
6. **Monitoring**: Sentry integration with configurable sampling (disabled in local)
7. **Coroutines**: Used with `runBlocking` in Spring MVC controllers for async operations (requires kotlinx-coroutines-core and kotlinx-coroutines-reactor dependencies)
8. **Database Migration**: SQL scripts located in `/migration/` directory (V1, V2, V3)
9. **Testing**: Use `./gradlew test --tests "*TestClassName*"` for running specific tests
10. **Operator Context**: Access authenticated user via `@CurrentUser operator: Operator` injected by CurrentUserAspect
11. **Soft Delete**: Entities with `removed_flag` and `removed_at` fields follow soft delete pattern
12. **API Responses**: Use `ApiResult` wrapper for consistent response format, `ListResult` for paginated lists

## Transaction Boundary Principles

✅ **올바른 패턴**:
- Controller → Service (with @Transactional) → Repository
- Controller → Service (with @Transactional) → Helper Service (without @Transactional)
- Service (with @Transactional) → Private methods (without @Transactional)

❌ **피해야 할 패턴**:
- Service (with @Transactional) → Service (with @Transactional)
- Helper Service에 @Transactional 사용
- Private 메서드에 @Transactional 사용

## Logging Standards

- **Framework**: kotlin-logging-jvm (io.github.oshai:kotlin-logging-jvm)
- **Pattern**: `private val logger = KotlinLogging.logger {}`
- **Custom Logger**: standard/common/log/Logger 활용
- **TraceLogger**: AOP 기반 메서드 실행 추적

# important-instruction-reminders
Do what has been asked; nothing more, nothing less.
NEVER create files unless they're absolutely necessary for achieving your goal.
ALWAYS prefer editing an existing file to creating a new one.
NEVER proactively create documentation files (*.md) or README files. Only create documentation files if explicitly requested by the User.
