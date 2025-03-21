plugins {
    val kotlinVersion = "2.1.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion

    id("com.google.devtools.ksp") version "2.1.20-RC3-1.0.31"
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.0.2"
    id("com.github.ben-manes.versions") version "0.52.0"
    idea
}

group = "com.github.bestheroz"
version = "0.0.1-SNAPSHOT"
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.3")
    }
}

dependencies {
    // Kotlin
    implementation(kotlin("noarg"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.google.dagger:dagger-compiler:2.56")
    ksp("com.google.dagger:dagger-compiler:2.56")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.1")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.aspectj:aspectjweaver")
    implementation("org.apache.commons:commons-lang3")

    // Database
    implementation("com.mysql:mysql-connector-j:9.2.0")
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.10.0")

    // Logging and Sentry
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.5.0")
    implementation("io.sentry:sentry-logback:8.5.0")

    // OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.5")

    // Utility
    implementation("org.fusesource.jansi:jansi:2.4.1")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.bootJar {
    archiveFileName.set("demo.jar")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktfmt("0.54").googleStyle()
        ktlint("1.5.0").editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "ktlint_official",
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_max-line-length" to "disabled",
            ),
        )
    }

    kotlinGradle {
        ktlint("1.5.0")
    }
}

configurations.compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
}
