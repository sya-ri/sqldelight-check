plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":reporter-api"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}
