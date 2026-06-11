plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":api"))
    api(project(":rule-api"))
    api(project(":reporter-api"))
    implementation(libs.sqldelight.compiler.env)
    implementation(libs.sqldelight.core)
    implementation(libs.sqldelight.dialect.api)
    implementation(libs.sqldelight.gradle.plugin)
    implementation(libs.sql.psi.environment)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.sqldelight.sqlite338.dialect)
}
