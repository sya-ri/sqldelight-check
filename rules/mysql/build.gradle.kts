plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":dialects:mysql"))
    implementation(project(":api"))
    implementation(project(":rule-api"))
    testImplementation(kotlin("test"))
}
