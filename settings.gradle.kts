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
    ":dialects:dialect-hsql",
    ":dialects:dialect-mysql",
    ":dialects:dialect-postgres",
    ":dialects:dialect-sqlite",
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

project(":dialects:dialect-hsql").projectDir = file("dialects/hsql")
project(":dialects:dialect-mysql").projectDir = file("dialects/mysql")
project(":dialects:dialect-postgres").projectDir = file("dialects/postgres")
project(":dialects:dialect-sqlite").projectDir = file("dialects/sqlite")
