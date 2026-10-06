plugins {
    alias(libs.plugins.kotlin.jvm)

    // Spring
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
}

group = "cali.arena"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    // exposed because entities and Spring Data repository interfaces
    // are part of this module's public API (used directly by services)
    api(project(":domain"))
    api("org.springframework.data:spring-data-jpa:3.5.4")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Spring Data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.5.6")

    // Jakarta Persistence API
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")

    // Driver
    runtimeOnly("org.postgresql:postgresql:42.7.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers:2.0.5")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.5")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
}

/**
 * Exposes src/sql to the test classpath so Testcontainers can load the same
 * schema and seed scripts the docker-compose database uses.
 */
sourceSets {
    test {
        resources.srcDir("src/sql")
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
