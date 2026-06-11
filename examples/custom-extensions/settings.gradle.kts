pluginManagement {
    includeBuild("../..")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

includeBuild("custom-ruleset") {
    dependencySubstitution {
        substitute(module("com.example:custom-ruleset")).using(project(":"))
    }
}
includeBuild("custom-reporter") {
    dependencySubstitution {
        substitute(module("com.example:custom-reporter")).using(project(":"))
    }
}

rootProject.name = "sqldelight-check-custom-extensions-example"
