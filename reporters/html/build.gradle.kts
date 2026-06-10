plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":reporter-api"))
    implementation(libs.kotlinx.html.jvm)
    testImplementation(kotlin("test"))
}
