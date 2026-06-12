plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":rule-api"))
    runtimeOnly(project(":dialects:hsql"))
    testImplementation(project(":dialects:hsql"))
    testImplementation(kotlin("test"))
}
