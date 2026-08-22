plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
}

group = "com.caliarena"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":service"))

    // To use Spring MVC
    implementation("org.springframework:spring-webmvc:7.0.7")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.1")

    implementation("org.springframework:spring-messaging:7.0.7")
    implementation("org.springframework:spring-websocket:7.0.7")

    // To use Servlet API
    implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")

    // To get password encode
    api("org.springframework.security:spring-security-core:7.0.5")

    testImplementation(platform("org.junit:junit-bom:5.12.0"))
    testImplementation("org.springframework:spring-test:6.2.11")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}
