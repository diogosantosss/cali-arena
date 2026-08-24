plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.ktlint)
}

group = "com.caliarena"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":domain"))

    // TransactionManager, entities and Spring Data repositories
    implementation(project(":repo-jpa"))

    // For dependency injection
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    // To get password encode
    implementation("org.springframework.security:spring-security-core:7.0.5")

    // To use annotations like @PostConstruct and @PreDestroy
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // To use SLF4J
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

kotlin {
    jvmToolchain(21)
}
