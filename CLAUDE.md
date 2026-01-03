# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build              # Build the project
./gradlew bootRun            # Run the application
./gradlew test               # Run all tests
./gradlew test --tests "*TestClassName*"  # Run specific test
./gradlew spotlessApply      # Apply code formatting (ktfmt + ktlint)
./gradlew spotlessCheck      # Check code formatting
./gradlew bootJar            # Create production JAR (demo.jar)
./gradlew dependencyUpdates  # Check dependency updates
```

## Architecture Overview

Kotlin Spring Boot 4.0.1 application using Java 25 and Kotlin 2.3.0 with Virtual Threads enabled.

### Three-Layer Architecture

1. **Controller** (`demo/controller/`): RESTful APIs with OpenAPI, coroutine support via `runBlocking`
2. **Service** (`demo/services/`): Business logic with `@Transactional`
3. **Repository** (`demo/repository/`): Spring Data JPA

### Standard Framework (`standard/`)

| Package | Purpose |
|---------|---------|
| `common/authenticate/` | JWT auth (JwtAuthenticationFilter, JwtTokenProvider) |
| `common/exception/` | Centralized error handling (BadRequest400, Unauthorized401, Forbidden403, InternalServerError500, TooManyRequests429) |
| `common/domain/` | Base entities (IdCreated, IdCreatedUpdated) with audit fields |
| `common/dto/` | Response wrappers (ApiResult, ListResult, TokenDto) |
| `common/util/` | DateUtils, PasswordUtil, LogUtils, EnvironmentUtils |
| `config/` | Security, OpenAPI, Coroutine, P6Spy configurations |

## Authentication & Security

- **JWT**: Stateless auth with BCrypt, 5min access / 30min refresh tokens (1440min in local)
- **Authorization**: `@PreAuthorize` with authorities (ADMIN_VIEW, ADMIN_EDIT, USER_VIEW, USER_EDIT)
- **Endpoints**:
  - Admin: `POST /api/v1/admins/login`, `GET /api/v1/admins/renew-token`, `DELETE /api/v1/admins/logout`
  - User: `POST /api/v1/users/login`, `GET /api/v1/users/renew-token`, `DELETE /api/v1/users/logout`
- **CurrentUser**: Use `@CurrentUser operator: Operator` to access authenticated user context

## Database

- MySQL with HikariCP (3 pool local, 30 prod)
- P6Spy for SQL logging
- Soft delete: `removed_flag`, `removed_at` fields
- UTC timezone, migrations in `/migration/`

## Configuration

| Profile | Swagger | Pool Size | Notes |
|---------|---------|-----------|-------|
| local | ✅ | 3 | 1440min token expiry |
| sandbox/qa | ✅ | 10 | |
| prod | ❌ | 30 | |

- Default port: 8000, Swagger UI: http://localhost:8000/swagger-ui.html
- Code style: ktfmt Google style + ktlint (wildcards enabled, max-line-length disabled)

## Transaction Boundary Principles

✅ **올바른 패턴**:
- Controller → Service (with @Transactional) → Repository
- Controller → Service (with @Transactional) → Helper Service (without @Transactional)
- Service (with @Transactional) → Private methods (without @Transactional)

❌ **피해야 할 패턴**:
- Service (with @Transactional) → Service (with @Transactional)
- Helper Service에 @Transactional 사용
- Private 메서드에 @Transactional 사용

## Logging

```kotlin
private val logger = KotlinLogging.logger {}
```
- Framework: kotlin-logging-jvm
- TraceLogger: AOP 기반 메서드 실행 추적

# important-instruction-reminders
Do what has been asked; nothing more, nothing less.
NEVER create files unless they're absolutely necessary for achieving your goal.
ALWAYS prefer editing an existing file to creating a new one.
NEVER proactively create documentation files (*.md) or README files. Only create documentation files if explicitly requested by the User.
