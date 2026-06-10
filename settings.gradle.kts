pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "sqldelight-check"

include(
    ":api",
    ":rule-api",
    ":reporter-api",
    ":core",
    ":rules:standard",
    ":reporters:json",
    ":reporters:sarif",
    ":reporters:text",
    ":reporters:html",
    ":reporters:markdown",
    ":gradle-plugin",
)
