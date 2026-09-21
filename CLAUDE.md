# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build              # Build the project
./gradlew bootRun            # Run the application
./gradlew test               # Run all tests
./gradlew test --tests "*TestClassName*"  # Run specific test
./gradlew spotlessApply      # Apply code formatting (ktlint)
./gradlew spotlessCheck      # Check code formatting
./gradlew bootJar            # Create production JAR (demo.jar)
```

## 전역 컨벤션

- 코드 스타일은 ktlint(`ktlint_official`). wildcard import 허용, max-line-length 미적용. 커밋 전 `./gradlew spotlessApply` 로 맞춘다
- Virtual Threads 가 켜져 있다(`application.yml` 의 `threads.virtual`). 스레드풀을 직접 만들거나 `ThreadLocal` 에 요청 상태를 쌓지 않는다 — 캐리어 스레드가 고정되지 않아 값이 새거나 유실된다
- 컨트롤러에서 코루틴을 쓸 때는 `runBlocking` 으로 감싼다
- 로거는 `private val logger = KotlinLogging.logger {}` 로 선언한다
- 메서드 진입/종료 로그를 직접 남기지 않는다 — TraceLogger 가 AOP 로 이미 기록하므로 중복된다

## 의존성

- pre-release(M/RC/Beta/Alpha/Preview)를 의도적으로 먼저 쓴다. 이 리포의 목적이 새 버전을 일찍 겪어보는 것이다
- 모든 플러그인·라이브러리 좌표는 `gradle/libs.versions.toml` 에만 둔다. `build.gradle.kts` 는 `libs.xxx` / `alias(libs.plugins.xxx)` 만 참조한다
- 버전 갱신 명령과 BOM·`@pin` 취급 규칙은 `.claude/rules/dependency-catalog.md` 참고

## 도메인 간 의존 규칙

- `demo` → `standard` 단방향만 허용한다. `standard` 에서 `demo` 를 참조하지 않는다 — 공용 프레임워크가 특정 도메인에 묶이면 재사용이 깨진다
- 도메인끼리는 서로의 Service 를 호출하지 않는다. 공통 로직이 필요하면 `standard` 로 올린다

## 트랜잭션 경계

- Service 클래스에 `@Transactional(readOnly = true)` 를 걸고, 쓰기 메서드에만 `@Transactional` 을 덧붙여 덮어쓴다
- Service(@Transactional) 에서 다른 Service(@Transactional) 를 호출하지 않는다 — 호출된 쪽이 호출자 트랜잭션에 합류해, 롤백 범위가 호출 경로마다 달라진다
- Helper Service 에는 `@Transactional` 을 붙이지 않는다 — 경계는 호출하는 Service 가 정한다
- private 메서드에 `@Transactional` 을 붙이지 않는다 — Spring AOP 는 프록시 기반이라 자기 호출을 가로채지 못해 애너테이션이 **조용히 무시된다**

## 인증

- JWT 스테이트리스 인증, 비밀번호는 BCrypt. access 5분 / refresh 30분, local 프로파일만 1440분
- 권한 검사는 `@PreAuthorize` + authority(ADMIN_VIEW, ADMIN_EDIT, USER_VIEW, USER_EDIT)
- 인증된 사용자 정보는 `@CurrentUser operator: Operator` 로 받는다

## Database

- MySQL + HikariCP, P6Spy 로 SQL 로깅
- soft delete 를 쓴다. 삭제는 `removed_flag` / `removed_at` 갱신이며 물리 삭제하지 않는다. 조회 시 `removed_flag` 필터를 빠뜨리지 않는다
- 타임존은 UTC. 마이그레이션 SQL 은 `/migration/` 에 `V{n}__{설명}.sql` 로 추가한다

## Configuration

| Profile | Swagger | Pool Size |
|---------|---------|-----------|
| local | ✅ | 3 |
| sandbox/qa | ✅ | 10 |
| prod | ❌ | 30 |

- 기본 포트 8000, Swagger UI: http://localhost:8000/swagger-ui.html

## CLAUDE.md 관리 규칙
- 이 파일은 200줄 이하 유지. 매 세션 필요한 내용만 둔다: 빌드/테스트 명령, 전역 컨벤션, 도메인 간 의존 규칙, 함정과 그 이유
- 코드에서 유추 가능한 내용(디렉터리 구조, 의존성 목록, 아키텍처 개요)은 쓰지 않는다
- 지시는 검증 가능한 수준으로 구체적으로 쓴다 (X "포맷 잘 맞춰라" / O "2-space 들여쓰기")
- 특정 도메인/경로에만 해당하는 규칙은 이 파일에 넣지 않는다
  - 도메인이 단일 폴더로 분리돼 있으면 → 해당 폴더의 CLAUDE.md
  - 여러 폴더에 흩어져 있으면 → `.claude/rules/<topic>.md` + `paths` frontmatter
  - 다단계 절차는 → 스킬
- 하위 CLAUDE.md 와 rules 에는 루트 규칙을 재진술하지 않는다. 충돌/중복 발견 시 사용자에게 알린다
- 도메인 규칙을 분리하면 아래 "도메인 인덱스"에 한 줄 추가한다
- 지시 파일을 추가/수정할 때는 변경 전 사용자에게 위치와 내용을 먼저 제안한다

## 도메인 인덱스
<!-- 형식: `경로/` — 한 줄 설명, 규칙 파일 위치 -->
<!-- 도메인은 controller/services/domain/repository/dtos 에 수평 분산돼 있어 단일 폴더 CLAUDE.md 가 불가능하다. 분리 시 .claude/rules/<domain>.md + paths 를 쓴다. -->
- `demo/**/Admin*` — 관리자 계정·로그인·토큰 재발급, ADMIN_* 권한. 규칙 파일 없음
- `demo/**/User*` — 일반 사용자 계정·로그인·토큰 재발급, USER_* 권한. 규칙 파일 없음
- `demo/**/Notice*` — 공지사항 CRUD, soft delete 사용. 규칙 파일 없음
- `standard/` — 인증·예외·공통 DTO·설정을 담은 공용 프레임워크. 규칙 파일 없음
