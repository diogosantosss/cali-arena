plugins {
    kotlin("jvm") version "2.1.20"
}

group = "cali.arena"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":domain"))

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}