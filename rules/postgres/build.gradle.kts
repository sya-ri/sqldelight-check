plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":dialects:dialect-postgres"))
    implementation(project(":api"))
    implementation(project(":rule-api"))
    testImplementation(project(":core"))
    testImplementation(project(":rules:standard"))
    testImplementation(kotlin("test"))
}
