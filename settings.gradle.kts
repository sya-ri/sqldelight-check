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
    ":dialects:hsql",
    ":dialects:mysql",
    ":dialects:postgres",
    ":dialects:sqlite",
    ":rules:standard",
    ":rules:postgres",
    ":rules:mysql",
    ":rules:sqlite",
    ":rules:hsql",
    ":reporters:json",
    ":reporters:sarif",
    ":reporters:text",
    ":reporters:html",
    ":reporters:markdown",
    ":reporters:github-annotations",
    ":gradle-plugin",
)
