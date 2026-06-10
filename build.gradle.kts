import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.gradle.plugin.publish) apply false
}

group = "dev.s7a"
version = "0.1.0"

val publishedArtifacts =
    mapOf(
        ":adapter-spi" to "sqldelight-check-adapter-spi",
        ":adapters:v2.3.2" to "sqldelight-check-adapter-2-3-2",
        ":api" to "sqldelight-check-api",
        ":core" to "sqldelight-check-core",
        ":reporter-api" to "sqldelight-check-reporter-api",
        ":reporters:html" to "sqldelight-check-reporter-html",
        ":reporters:json" to "sqldelight-check-reporter-json",
        ":reporters:markdown" to "sqldelight-check-reporter-markdown",
        ":reporters:sarif" to "sqldelight-check-reporter-sarif",
        ":reporters:text" to "sqldelight-check-reporter-text",
        ":rule-api" to "sqldelight-check-rule-api",
        ":rules:standard" to "sqldelight-check-rules-standard",
    )

subprojects {
    group = rootProject.group
    version = rootProject.version

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(25)
            explicitApi()
        }
    }

    val publishedArtifactId = publishedArtifacts[path]
    if (publishedArtifactId != null) {
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "org.jetbrains.dokka-javadoc")
        apply(plugin = "com.vanniktech.maven.publish")

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
                publishToMavenCentral()
                if (providers.gradleProperty("signingInMemoryKey").isPresent) {
                    signAllPublications()
                }
                coordinates(rootProject.group.toString(), publishedArtifactId, rootProject.version.toString())
                configure(
                    KotlinJvm(
                        javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationJavadoc"),
                        sourcesJar = SourcesJar.Sources(),
                    ),
                )
                pom {
                    name.set(publishedArtifactId)
                    description.set("Formatter and rule-based linter for SQLDelight .sq and .sqm files.")
                    inceptionYear.set("2026")
                    url.set("https://github.com/sya-ri/sqldelight-check")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://github.com/sya-ri/sqldelight-check/blob/main/LICENSE")
                        }
                    }
                    developers {
                        developer {
                            id.set("sya-ri")
                            name.set("sya-ri")
                            email.set("contact@s7a.dev")
                        }
                    }
                    scm {
                        url.set("https://github.com/sya-ri/sqldelight-check")
                    }
                }
            }
        }
    }
}
