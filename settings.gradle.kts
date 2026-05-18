pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MorphoCore"

include(":app")
include(":core:domain")
include(":core:common")
include(":core:design-system")
include(":content:content-api")
include(":content:content-impl")
include(":content:content-sources")
include(":content:content-testing")
include(":rendering:rendering-api")
include(":rendering:rendering-scene-view")
include(":rendering:rendering-testing")
include(":theme:theme-api")
include(":theme:theme-impl")
include(":feature:feature-browse")
include(":feature:feature-movements")
include(":feature:feature-detail")
include(":feature:feature-settings")
