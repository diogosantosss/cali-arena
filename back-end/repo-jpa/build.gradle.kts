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
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    environment("DB_URL", "jdbc:postgresql://localhost:5433/postgres?user=postgres&password=postgres")
    dependsOn(":repo-jpa:dbTestsWait")
    finalizedBy(":repo-jpa:dbTestsDown")
}

/**
 * Docker related tasks
 */

val composeFileDir: Directory = rootProject.layout.projectDirectory
val dockerComposePath = composeFileDir.file("repo-jpa/docker-compose.yml").toString()
val dockerExe =
    when (
        org.gradle.internal.os.OperatingSystem
            .current()
    ) {
        org.gradle.internal.os.OperatingSystem.MAC_OS -> "/usr/local/bin/docker"
        org.gradle.internal.os.OperatingSystem.WINDOWS -> "docker"
        else -> "docker" // Linux and others
    }

tasks.register<Exec>("dbTestsUp") {
    commandLine(dockerExe, "compose", "-f", dockerComposePath, "up", "-d", "--build", "--force-recreate", "db-tests")
}

tasks.register<Exec>("dbTestsWait") {
    commandLine(dockerExe, "exec", "db-tests", "/app/bin/wait-for-postgres.sh", "localhost")
    dependsOn("dbTestsUp")
}

tasks.register<Exec>("dbTestsDown") {
    commandLine(dockerExe, "compose", "-f", dockerComposePath, "down", "db-tests")
}
