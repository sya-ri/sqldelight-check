plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":api"))
    api(project(":rule-api"))
    api(project(":reporter-api"))
    api(project(":adapter-spi"))
    testImplementation(kotlin("test"))
}
