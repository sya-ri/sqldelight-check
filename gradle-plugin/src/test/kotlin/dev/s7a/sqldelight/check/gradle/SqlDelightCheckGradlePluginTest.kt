package dev.s7a.sqldelight.check.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * Functional tests for applying the sqldelight-check Gradle plugin.
 */
class SqlDelightCheckGradlePluginTest {
    @Test
    fun `plugin registers extension configurations and tasks`() {
        val project =
            testProject(
                """
                plugins {
                    id("dev.s7a.sqldelight.check")
                }

                tasks.register("printSqldelightCheckModel") {
                    doLast {
                        println("hasExtension=" + (project.extensions.findByName("sqldelightCheck") != null))
                        println("hasRuleSetConfiguration=" + (configurations.findByName("sqldelightCheckRuleSet") != null))
                        println("hasReporterConfiguration=" + (configurations.findByName("sqldelightCheckReporter") != null))
                        listOf(
                            "sqldelightCheck",
                            "sqldelightCheckWrite",
                            "sqldelightLint",
                            "sqldelightLintWrite",
                            "sqldelightFormat",
                            "sqldelightFormatWrite",
                        ).forEach { taskName ->
                            println("task." + taskName + "=" + (tasks.findByName(taskName) != null))
                        }
                    }
                }
                """.trimIndent(),
            )

        val result = project.run("printSqldelightCheckModel")

        assertEquals(SUCCESS, result.task(":printSqldelightCheckModel")?.outcome)
        assertContains(result.output, "hasExtension=true")
        assertContains(result.output, "hasRuleSetConfiguration=true")
        assertContains(result.output, "hasReporterConfiguration=true")
        assertContains(result.output, "task.sqldelightCheck=true")
        assertContains(result.output, "task.sqldelightCheckWrite=true")
        assertContains(result.output, "task.sqldelightLint=true")
        assertContains(result.output, "task.sqldelightLintWrite=true")
        assertContains(result.output, "task.sqldelightFormat=true")
        assertContains(result.output, "task.sqldelightFormatWrite=true")
    }

    @Test
    fun `check task writes default reports`() {
        val project =
            testProject(
                """
                plugins {
                    id("dev.s7a.sqldelight.check")
                }
                """.trimIndent(),
            )

        val result = project.run("sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        assertEquals(true, project.file("build/reports/sqldelight-check/report.json").exists())
        assertEquals(true, project.file("build/reports/sqldelight-check/report.sarif").exists())
        assertEquals(true, project.file("build/reports/sqldelight-check/report.text").exists())
        assertContains(project.file("build/reports/sqldelight-check/report.json").readText(), "diagnostics")
    }

    @Test
    fun `check task resolves sqldelight database inputs`() {
        val project =
            testProject(
                """
                plugins {
                    kotlin("jvm") version "2.4.0"
                    id("app.cash.sqldelight") version "2.3.2"
                    id("dev.s7a.sqldelight.check")
                }

                repositories {
                    mavenCentral()
                }

                sqldelight {
                    databases {
                        create("Database") {
                            packageName.set("com.example")
                            srcDirs("src/main/sqldelight")
                            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.3.2")
                        }
                    }
                }
                """.trimIndent(),
            )
        project.write(
            "src/main/sqldelight/com/example/Player.sq",
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY,
              name TEXT NOT NULL
            );
            """.trimIndent(),
        )

        val result = project.run("sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        assertContains(result.output, "sqldelight-check analyzed 1 SQLDelight database(s).")
        assertEquals(true, project.file("build/reports/sqldelight-check/report.json").exists())
    }

    @Test
    fun `check task fails after writing reports when sqldelight reports errors`() {
        val project =
            testProject(
                """
                plugins {
                    kotlin("jvm") version "2.4.0"
                    id("app.cash.sqldelight") version "2.3.2"
                    id("dev.s7a.sqldelight.check")
                }

                repositories {
                    mavenCentral()
                }

                sqldelight {
                    databases {
                        create("Database") {
                            packageName.set("com.example")
                            srcDirs("src/main/sqldelight")
                            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.3.2")
                        }
                    }
                }
                """.trimIndent(),
            )
        project.write("src/main/sqldelight/com/example/Broken.sq", "CREATE TABL broken;")

        val result = project.runAndFail("sqldelightCheck")

        assertEquals(true, project.file("build/reports/sqldelight-check/report.json").exists())
        assertContains(result.output, "sqldelight-check found 1 error diagnostic(s).")
        assertContains(project.file("build/reports/sqldelight-check/report.json").readText(), """"errors":1""")
    }

    @Test
    fun `check task applies rule overrides from the extension`() {
        val project =
            testProject(
                """
                import dev.s7a.sqldelight.check.api.Enablement
                import dev.s7a.sqldelight.check.api.Severity

                plugins {
                    kotlin("jvm") version "2.4.0"
                    id("app.cash.sqldelight") version "2.3.2"
                    id("dev.s7a.sqldelight.check")
                }

                repositories {
                    mavenCentral()
                }

                sqldelight {
                    databases {
                        create("Database") {
                            packageName.set("com.example")
                            srcDirs("src/main/sqldelight")
                            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.3.2")
                        }
                    }
                }

                sqldelightCheck {
                    ruleSets {
                        maybeCreate("standard").enabled.set(Enablement.Disabled)
                    }
                    rules {
                        maybeCreate("standard:final-newline").apply {
                            enabled.set(Enablement.Enabled)
                            severity.set(Severity.Error)
                        }
                    }
                }
                """.trimIndent(),
            )
        project.write(
            "src/main/sqldelight/com/example/Player.sq",
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.trimIndent(),
        )

        val result = project.runAndFail("sqldelightCheck")

        assertContains(result.output, "sqldelight-check found 1 error diagnostic(s).")
        val report = project.file("build/reports/sqldelight-check/report.json").readText()
        assertContains(report, """"ruleId":"standard:final-newline"""")
        assertContains(report, """"errors":1""")
    }

    /**
     * Creates a temporary Gradle project for a TestKit run.
     */
    private fun testProject(buildScript: String): TestProject {
        val directory = Files.createTempDirectory("sqldelight-check-gradle-plugin-test")
        directory.resolve("settings.gradle.kts").writeText("""rootProject.name = "sqldelight-check-test"""")
        directory.resolve("build.gradle.kts").writeText(buildScript)
        return TestProject(directory)
    }

    /**
     * Gradle project fixture used by the functional tests.
     */
    private class TestProject(
        private val directory: Path,
    ) {
        /**
         * Returns a file inside this temporary project.
         */
        fun file(path: String): Path = directory.resolve(path)

        /**
         * Writes [content] to a file inside this temporary project.
         */
        fun write(
            path: String,
            content: String,
        ) {
            val file = file(path)
            Files.createDirectories(file.parent)
            file.writeText(content)
        }

        /**
         * Runs Gradle with the plugin-under-test classpath.
         */
        fun run(vararg arguments: String) =
            GradleRunner
                .create()
                .withProjectDir(directory.toFile())
                .withArguments(*arguments, "--stacktrace")
                .withPluginClasspath()
                .build()

        /**
         * Runs Gradle and expects the build to fail.
         */
        fun runAndFail(vararg arguments: String) =
            GradleRunner
                .create()
                .withProjectDir(directory.toFile())
                .withArguments(*arguments, "--stacktrace")
                .withPluginClasspath()
                .buildAndFail()
    }
}
