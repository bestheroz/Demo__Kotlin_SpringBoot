plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    idea
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.version.catalog.update)
}

group = "com.github.bestheroz"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    // Demo 정책: 사전 릴리스 선체험용 저장소. 스냅샷 저장소는 추가하지 않는다.
    maven { url = uri("https://repo.spring.io/milestone") }
}

// 좌표는 전부 gradle/libs.versions.toml 에 있고 여기서는 `libs.xxx` 로만 참조한다.
// 카탈로그에 버전 없이 등록한 항목은 Spring Boot BOM 이 버전을 정한다(규칙은 카탈로그 머리말 참고).
dependencies {
    // Kotlin
    implementation(libs.kotlin.noarg)
    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)

    // Spring
    // Boot 4 에서 spring-boot-starter-web 은 spring-boot-starter-webmvc 로 이름이 바뀌었다.
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.aspectjweaver)

    // Database
    // BOM 도 관리하는 좌표지만 원래부터 버전을 명시했다. BOM 보다 앞서 체험하려는 핀으로 보고 LATEST 로 올린다.
    implementation(libs.mysql.connector.j)
    implementation(libs.p6spy.spring.boot.starter)

    // Logging and Sentry
    implementation(libs.java.jwt)
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.sentry.spring.boot4)
    implementation(libs.sentry.logback)

    // OpenAPI (UI includes API dependency)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("demo.jar")
}

spotless {
    // ktlint 는 원래 버전을 명시하던 도구라 Demo 정책대로 명시한 채 둔다.
    // 카탈로그 밖이라 VCU 가 추적하지 않으니 com.pinterest.ktlint:ktlint-cli 최신을 보고 손으로 올린다.
    kotlin {
        ktlint("1.8.0").editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "ktlint_official",
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_max-line-length" to "disabled",
            ),
        )
    }

    kotlinGradle {
        ktlint("1.8.0")
    }
}

// `dependencyUpdates` 는 BOM 이 관리하는 의존성까지 포함해 전체 현황을 훑는 보고서다.
// 카탈로그를 실제로 고쳐 쓰는 것은 아래 versionCatalogUpdate 뿐이다.
// Demo 리포는 신규 버전 선체험과 변화점 발견이 목적이라 두 도구 모두 사전 릴리스(M·RC·Beta·Alpha 등)를 후보로 본다.
// 스냅샷 저장소는 두지 않으므로 SNAPSHOT 은 후보에 오르지 않는다.
// 올린 뒤 깨지면 먼저 코드를 고치고, 고칠 수 없는 좌표만 동작하는 최신 버전으로 내려 카탈로그 항목 위에 `# @pin` 을 단다.
// 그 사유는 이 파일의 해당 의존성 옆에 주석으로 남긴다.
tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates") {
    // 플러그인 내장 사전 릴리스 판정(alpha·beta·rc·M·preview 등)을 끄고 모든 버전을 후보로 보여 준다.
    rejectPreReleases = false
}

versionCatalogUpdate {
    // LATEST 는 형식을 가리지 않고 가장 높은 버전을 고른다. 사전 릴리스가 정식판보다 높으면 사전 릴리스를 쓴다.
    versionSelector(nl.littlerobots.vcu.plugin.resolver.VersionSelectors.LATEST)
}
