@file:OptIn(ExperimentalAbiValidation::class)

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

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

val dokkaOlderVersionsDir = layout.buildDirectory.dir("dokka/olderVersions")

val publishedArtifacts =
    mapOf(
        ":api" to "sqldelight-check-api",
        ":core" to "sqldelight-check-core",
        ":reporter-api" to "sqldelight-check-reporter-api",
        ":reporters:html" to "sqldelight-check-reporter-html",
        ":reporters:github-annotations" to "sqldelight-check-reporter-github-annotations",
        ":reporters:json" to "sqldelight-check-reporter-json",
        ":reporters:markdown" to "sqldelight-check-reporter-markdown",
        ":reporters:sarif" to "sqldelight-check-reporter-sarif",
        ":reporters:text" to "sqldelight-check-reporter-text",
        ":rule-api" to "sqldelight-check-rule-api",
        ":rules:hsql" to "sqldelight-check-rules-hsql",
        ":rules:mysql" to "sqldelight-check-rules-mysql",
        ":rules:postgres" to "sqldelight-check-rules-postgres",
        ":rules:sqlite" to "sqldelight-check-rules-sqlite",
        ":rules:standard" to "sqldelight-check-rules-standard",
    )

dependencies {
    publishedArtifacts.keys.forEach {
        dokka(project(it))
    }
    dokkaPlugin(libs.dokka.versioning.plugin)
}

dokka {
    pluginsConfiguration {
        versioning {
            version.set(project.version.toString())
            olderVersionsDir.set(dokkaOlderVersionsDir)
        }
    }
}

val prepareDokkaVersioning by tasks.registering {
    doLast {
        dokkaOlderVersionsDir.get().asFile.mkdirs()
    }
}

tasks.matching { it.name == "dokkaGenerateHtml" || it.name == "dokkaGeneratePublicationHtml" }.configureEach {
    dependsOn(prepareDokkaVersioning)
    inputs.dir(dokkaOlderVersionsDir)
        .withPropertyName("dokkaOlderVersions")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

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
            extensions.configure<KotlinJvmProjectExtension>("kotlin") {
                abiValidation()
            }
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
