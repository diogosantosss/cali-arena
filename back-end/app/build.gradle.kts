plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
}

group = "com.caliarena"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":http"))
    implementation(project(":repo-jpa"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // to serve Swagger UI and the OpenAPI spec
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")

    // for Postgres
    implementation("org.postgresql:postgresql:42.7.2")

    // To get password encode
    api("org.springframework.security:spring-security-core:7.0.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootRun {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile
            .readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .forEach { line ->
                val (key, value) = line.split("=", limit = 2)
                environment(key.trim(), value.trim())
            }
    }
}

tasks.processResources {
    from("$rootDir/docs") {
        include("openapi.yaml")
    }
}

kotlin {
    jvmToolchain(21)
}

/**
 * Docker related tasks
 */
val dockerImageJvm = "caliarena-jvm"
val dockerImagePostgres = "caliarena-postgres"
val dockerExe =
    when (
        org.gradle.internal.os.OperatingSystem
            .current()
    ) {
        org.gradle.internal.os.OperatingSystem.MAC_OS -> "/usr/local/bin/docker"
        org.gradle.internal.os.OperatingSystem.WINDOWS -> "docker"
        else -> "docker" // Linux and others
    }

tasks.register<Copy>("extractUberJar") {
    description = "Extracts the contents of the Uber JAR (fat JAR) into the 'build/dependency' directory for use in Docker builds."
    dependsOn("assemble")
    // opens the JAR containing everything...
    from(
        zipTree(
            layout.buildDirectory
                .file("libs/app-$version.jar")
                .get()
                .toString(),
        ),
    )
    // ... into the 'build/dependency' folder
    into("build/dependency")
}

tasks.register<Exec>("buildImageJvm") {
    description = "Builds the Docker image for the JVM application using the specified Dockerfile and build context."
    dependsOn("extractUberJar")
    commandLine(dockerExe, "build", "-t", dockerImageJvm, "-f", "docker/Dockerfile-jvm", ".")
}

tasks.register<Exec>("buildImagePostgres") {
    description = "Builds the Docker image for Postgres using the specified Dockerfile and build context."
    commandLine(
        dockerExe,
        "build",
        "-t", // Flag to assign a tag to the image
        dockerImagePostgres, // Name:tag of the image to be built (e.g., "my-postgres:test")
        "-f", // Flag to specify a custom Dockerfile
        "docker/Dockerfile-postgres", // Path to the Dockerfile used to build the image
        "../repo-jpa", // Build context directory containing files referenced by the Dockerfile
    )
}

tasks.register<Exec>("buildImageNginx") {
    description = "Builds the Docker image for Nginx using the specified Dockerfile and build context."
    workingDir(rootProject.projectDir.parentFile) // corre a partir da raiz do projeto
    commandLine(dockerExe, "build", "-t", "caliarena-nginx", "-f", "back-end/nginx/Dockerfile-nginx", ".")
}

tasks.register("buildImageAll") {
    description = "Builds all Docker images (JVM, Postgres) by executing the individual build tasks for each image."
    dependsOn("buildImageJvm")
    dependsOn("buildImagePostgres")
    dependsOn("buildImageNginx")
}

// .env file
val envFile = rootProject.file(".env")

tasks.register<Exec>("allUp") {
    description = "Starts all Docker containers defined in the Docker Compose file using the specified .env file for environment variables."
    dependsOn("buildImageAll")
    // the argument `--env-file` specifies the path to the .env file that contains environment variables for Docker Compose
    commandLine(dockerExe, "compose", "--env-file", envFile, "up", "--force-recreate", "-d")
}

tasks.register<Exec>("allDown") {
    description = "Stops all Docker containers defined in the Docker Compose file."
    commandLine(dockerExe, "compose", "--env-file", envFile, "down")
}
