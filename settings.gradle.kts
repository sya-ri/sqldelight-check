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
    ":adapter-spi",
    ":core",
    ":rules:standard",
    ":reporters:json",
    ":reporters:sarif",
    ":reporters:text",
    ":reporters:html",
    ":reporters:markdown",
    ":adapters:v2.3.2",
    ":gradle-plugin",
)
