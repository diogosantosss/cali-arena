plugins {
    kotlin("jvm") version "2.1.20"

    id("io.spring.dependency-management") version "1.1.7"
}

group = "cali.arena"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":repo"))
    implementation(project(":domain"))

    // Spring Data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Jakarta Persistence API
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")

    // Driver
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}