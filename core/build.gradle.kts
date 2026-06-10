plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":api"))
    implementation(project(":rule-api"))
    implementation(project(":reporter-api"))
    implementation(project(":adapter-spi"))
    testImplementation(kotlin("test"))
}
