pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Focuno"

include(":app")

// Core
include(":core:common")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:datastore")
include(":core:usage")
include(":core:ml")
include(":core:designsystem")

// Features
include(":feature:focus")
include(":feature:analytics")
include(":feature:blocking")
include(":feature:onboarding")
