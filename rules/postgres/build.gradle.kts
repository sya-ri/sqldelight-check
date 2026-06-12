plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":dialects:postgres"))
    implementation(project(":api"))
    implementation(project(":rule-api"))
    testImplementation(kotlin("test"))
}
