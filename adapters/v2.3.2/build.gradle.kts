plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":adapter-spi"))
    testImplementation(kotlin("test"))
}
