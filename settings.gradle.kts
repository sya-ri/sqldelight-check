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
    ":rules:postgres",
    ":rules:mysql",
    ":rules:sqlite",
    ":rules:hsql",
    ":dialects:sqldelight",
    ":reporters:json",
    ":reporters:sarif",
    ":reporters:text",
    ":reporters:html",
    ":reporters:markdown",
    ":reporters:github-annotations",
    ":gradle-plugin",
)
