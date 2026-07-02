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
    api(project(":repo"))

    // For dependency injection
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    // To get password encode
    implementation("org.springframework.security:spring-security-core:7.0.5")

    // To use annotations like @PostConstruct and @PreDestroy
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // To use SLF4J
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.12.0"))
    testImplementation("org.springframework:spring-test:6.2.11")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
