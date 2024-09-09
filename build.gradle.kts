import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.0.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("kapt") version kotlinVersion

    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.diffplug.spotless") version "7.0.0.BETA2"
    id("com.github.ben-manes.versions") version "0.51.0"
    idea
}

group = "com.github.bestheroz"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    // kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.aspectj:aspectjweaver")
    implementation("org.apache.commons:commons-lang3")
    implementation("com.mysql:mysql-connector-j:9.0.0")
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.9.2")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.0.0-alpha.4")
    implementation("io.sentry:sentry-logback:8.0.0-alpha.4")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.6.0")
    implementation("io.awspring.cloud:spring-cloud-aws-starter:3.2.0-M1")
    implementation("io.awspring.cloud:spring-cloud-aws-autoconfigure:3.2.0-M1")
    implementation("com.amazonaws.secretsmanager:aws-secretsmanager-jdbc:2.0.2")
    implementation("org.fusesource.jansi:jansi:2.4.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.withType<Test> {
    useJUnitPlatform()
    exclude("**/*")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
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
        ktlint("1.3.1")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}
