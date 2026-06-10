plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":api"))
    api(project(":rule-api"))
    api(project(":reporter-api"))
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.sqldelight.gradle.plugin)
    testRuntimeOnly(libs.sqldelight.compiler.env)
    testRuntimeOnly(libs.sql.psi.environment)
    testRuntimeOnly(libs.sqldelight.sqlite338.dialect)
}
