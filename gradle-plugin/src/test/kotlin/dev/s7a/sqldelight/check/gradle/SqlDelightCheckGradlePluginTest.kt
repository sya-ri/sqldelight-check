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
         * Runs Gradle with the plugin-under-test classpath.
         */
        fun run(vararg arguments: String) =
            GradleRunner
                .create()
                .withProjectDir(directory.toFile())
                .withArguments(*arguments, "--stacktrace")
                .withPluginClasspath()
                .build()
    }
}
