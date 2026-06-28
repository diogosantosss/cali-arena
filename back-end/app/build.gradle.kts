plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

group = "com.caliarena"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

}

kotlin {
    jvmToolchain(21)
}