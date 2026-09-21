---
paths:
  - "gradle/libs.versions.toml"
  - "gradle/**"
  - "build.gradle.kts"
  - "settings.gradle.kts"
  - "gradle.properties"
---

# 버전 카탈로그 운영 기준

## 갱신 명령

```bash
./gradlew dependencyUpdates                    # Update report incl. BOM-managed deps (pre-releases included)
./gradlew versionCatalogUpdate --interactive   # Write update candidates to gradle/libs.versions.updates.toml
./gradlew versionCatalogApplyUpdates           # Apply only the entries left in that file to the catalog
```

## 버전 표기
- 원래 버전이 없던 좌표는 versionless(`{ module = "g:a" }`) 로 둔다. Spring Boot BOM 을 따라 함께 움직이게 하기 위해서다
- BOM 이 관리하는데도 명시 버전을 가진 좌표(`mysql-connector-j`)는 BOM 보다 앞서 가려는 의도이므로 그대로 둔다. 명시 버전이 BOM 을 이기므로, BOM 관리 좌표에 버전을 붙이는 것은 앞서 가겠다는 결정이다. 그럴 의도가 아니면 versionless 로 둔다
- Kotlin JVM / Spring / JPA 플러그인은 `[versions] kotlin` 을 공유한다
- 최신 버전이 빌드를 깨뜨리고 고칠 수 없으면, 그 좌표만 동작하는 최신 버전으로 내리고 `# @pin` 을 달되 이유는 `build.gradle.kts` 에 적는다

## versionCatalogUpdate(VCU) 주의
- VCU 는 버전이 있는 항목만 갱신하고 versionless 항목은 건너뛴다
- VCU 가 카탈로그를 다시 쓸 때 항목 옆 주석이 지워질 수 있다. 설명은 `build.gradle.kts` 에 두고 카탈로그에는 `@pin` / `@keep` 만 쓴다
- VCU 는 configuration cache 와 호환되지 않아 "Configuration cache entry discarded" 를 출력한다. 빌드는 그대로 성공하므로 무시한다

## 저장소·빌드 설정
- `repo.spring.io/milestone` 저장소는 `settings.gradle.kts` 와 `build.gradle.kts` 에 유지한다. snapshot 저장소는 추가하지 않는다
- Spring Boot 플러그인, Kotlin, Gradle wrapper 버전은 Demo 리포들과 같은 값을 쓴다
- `gradle.properties` 에서 configuration cache / build cache / 병렬 실행을 켜 두었다. CI 에서 해당 플래그를 따로 넘기지 않는다
