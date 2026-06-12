plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":rule-api"))
    runtimeOnly(project(":dialects:sqlite"))
    testImplementation(project(":dialects:sqlite"))
    testImplementation(kotlin("test"))
}
