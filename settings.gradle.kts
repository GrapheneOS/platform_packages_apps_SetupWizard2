pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven("https://repo.repsy.io/mvn/jitesh/public")
    }
}

rootProject.name = "SetupWizard"
include(":app")
include(":setupcompat")
include(":setupdesign")
