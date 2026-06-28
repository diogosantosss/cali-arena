plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "cali-arena"
include(":app")
include("repo-jpa")
include("repo-jpa")
include("domain")
include("repo")