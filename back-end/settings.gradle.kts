plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "cali-arena"
include(":app")
include("http")
include("service")
include("domain")
include("repo")
include("repo-jpa")
