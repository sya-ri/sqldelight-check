plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":rule-api"))
    runtimeOnly(project(":dialects:mysql"))
    testImplementation(project(":dialects:mysql"))
    testImplementation(kotlin("test"))
}
