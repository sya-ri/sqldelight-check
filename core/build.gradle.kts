plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":api"))
    api(project(":rule-api"))
    api(project(":reporter-api"))
    testImplementation(kotlin("test"))
}
