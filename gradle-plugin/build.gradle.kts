import org.gradle.api.tasks.testing.Test
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.plugin.publish)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":rules:standard"))
    implementation(project(":reporters:json"))
    implementation(project(":reporters:sarif"))
    implementation(project(":reporters:text"))
    implementation(project(":reporters:html"))
    implementation(project(":reporters:markdown"))
    compileOnly(kotlin("gradle-plugin-api"))
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

tasks.withType<Test>().configureEach {
    val verifySnapshots = providers.gradleProperty("sqldelightCheck.verifySnapshots").orElse("false")
    inputs.property("sqldelightCheck.verifySnapshots", verifySnapshots)
    systemProperty("sqldelightCheck.verifySnapshots", verifySnapshots.get())
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(configurations.compileClasspath)
}

gradlePlugin {
    website.set("https://github.com/sya-ri/sqldelight-check")
    vcsUrl.set("https://github.com/sya-ri/sqldelight-check")
    plugins {
        create("sqldelightCheck") {
            id = "dev.s7a.sqldelight.check"
            displayName = "SQLDelight Check Gradle Plugin"
            description = "Formatter and rule-based linter for SQLDelight .sq and .sqm files."
            tags.set(listOf("sqldelight", "sql", "lint", "formatter", "gradle"))
            implementationClass = "dev.s7a.sqldelight.check.gradle.SqlDelightCheckGradlePlugin"
        }
    }
}
