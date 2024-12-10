import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.1.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion

    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.diffplug.spotless") version "7.0.0.BETA4"
    id("com.github.ben-manes.versions") version "0.51.0"
    idea
}

group = "com.github.bestheroz"
version = "0.0.1-SNAPSHOT"
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.0")
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.2.1")
    }
}

dependencies {
    // Kotlin
    implementation(kotlin("noarg"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.google.dagger:dagger-compiler:2.53.1")
    ksp("com.google.dagger:dagger-compiler:2.53.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.aspectj:aspectjweaver")
    implementation("org.apache.commons:commons-lang3")

    // Database
    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.10.0")

    // AWS
    implementation("io.awspring.cloud:spring-cloud-aws-starter")
    implementation("io.awspring.cloud:spring-cloud-aws-autoconfigure")
    implementation("com.amazonaws.secretsmanager:aws-secretsmanager-jdbc:2.0.2")

    // Logging and Sentry
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.0.0-rc.2")
    implementation("io.sentry:sentry-logback:8.0.0-rc.2")

    // OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.6.0")

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
    jvmToolchain(21)
}
tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
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
        ktfmt("0.53").googleStyle()
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

configurations {
    compileOnly {
        extendsFrom(configurations["annotationProcessor"])
    }
}
