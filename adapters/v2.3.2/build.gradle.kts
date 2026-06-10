plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":adapter-spi"))
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.sqldelight.gradle.plugin)
    testRuntimeOnly(libs.sqldelight.compiler.env)
    testRuntimeOnly(libs.sql.psi.environment)
    testRuntimeOnly(libs.sqldelight.sqlite338.dialect)
}
