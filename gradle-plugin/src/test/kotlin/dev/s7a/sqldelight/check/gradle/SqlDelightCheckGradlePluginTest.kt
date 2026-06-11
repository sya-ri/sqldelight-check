package dev.s7a.sqldelight.check.gradle

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.tools.ToolProvider
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.UnexpectedBuildFailure

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
                            "sqldelightFix",
                        ).forEach { taskName ->
                            println("task." + taskName + "=" + (tasks.findByName(taskName) != null))
                        }
                    }
                }
                """.trimIndent(),
            )

        val result = project.run("printSqldelightCheckModel")

        assertEquals(SUCCESS, result.task(":printSqldelightCheckModel")?.outcome)
        val expectedOutput =
            listOf(
                "hasExtension=true",
                "hasRuleSetConfiguration=true",
                "hasReporterConfiguration=true",
                "task.sqldelightCheck=true",
                "task.sqldelightFix=true",
            )
        assertContentEquals(expectedOutput, result.outputLinesMatching(expectedOutput))
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
        assertEquals(false, project.file("build/reports/sqldelight-check/report.github-annotations").exists())
        assertEquals(EMPTY_JSON_REPORT, project.file("build/reports/sqldelight-check/report.json").readText())
        assertEquals(EMPTY_SARIF_REPORT, project.file("build/reports/sqldelight-check/report.sarif").readText())
        assertEquals("sqldelight-check diagnostics: 0\n", project.file("build/reports/sqldelight-check/report.text").readText())
    }

    @Test
    fun `check task enables github annotations report on GitHub Actions`() {
        val project =
            testProject(
                """
                plugins {
                    id("dev.s7a.sqldelight.check")
                }
                """.trimIndent(),
            )

        val result = project.runWithEnvironment(mapOf("GITHUB_ACTIONS" to "true"), "sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        assertEquals(true, project.file("build/reports/sqldelight-check/report.github-annotations").exists())
        assertEquals("", project.file("build/reports/sqldelight-check/report.github-annotations").readText())
    }

    @Test
    fun `check task respects disabled github annotations report on GitHub Actions`() {
        val project =
            testProject(
                """
                plugins {
                    id("dev.s7a.sqldelight.check")
                }

                sqldelightCheck {
                    reports {
                        githubAnnotations {
                            required.set(false)
                        }
                    }
                }
                """.trimIndent(),
            )

        val result = project.runWithEnvironment(mapOf("GITHUB_ACTIONS" to "true"), "sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        assertEquals(false, project.file("build/reports/sqldelight-check/report.github-annotations").exists())
    }

    @Test
    fun `extension supports nested configuration DSL`() {
        val project =
            testProject(
                """
                import dev.s7a.sqldelight.check.api.Enablement
                import dev.s7a.sqldelight.check.api.Severity

                plugins {
                    id("dev.s7a.sqldelight.check")
                }

                sqldelightCheck {
                    ruleSets {
                        standard {
                            enabled.set(Enablement.Auto)
                        }
                        postgres {
                            enabled.set(Enablement.Disabled)
                        }
                    }

                    rules {
                        rule("standard:final-newline") {
                            enabled.set(Enablement.Enabled)
                            severity.set(Severity.Error)
                        }
                    }

                    databases {
                        database("Database") {
                            ruleSets {
                                standard {
                                    enabled.set(Enablement.Disabled)
                                }
                            }
                            rules {
                                rule("standard:final-newline") {
                                    severity.set(Severity.Warning)
                                }
                            }
                        }
                    }

                    reports {
                        html {
                            required.set(true)
                        }
                        json {
                            prettyPrint.set(true)
                        }
                    }
                }
                """.trimIndent(),
            )

        val result = project.run("sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        assertEquals(true, project.file("build/reports/sqldelight-check/report.html").exists())
        assertEquals(EMPTY_PRETTY_JSON_REPORT, project.file("build/reports/sqldelight-check/report.json").readText())
    }

    @Test
    fun `check task resolves sqldelight database inputs`() {
        val project =
            testProject(
                sqlDelightBuildScript(),
            )
        project.write(
            "src/main/sqldelight/com/example/Player.sq",
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY,
              name TEXT NOT NULL
            );
            """.trimIndent() + "\n",
        )

        val result = project.run("sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        val expectedOutput = listOf("sqldelight-check analyzed 1 SQLDelight database(s).")
        assertContentEquals(expectedOutput, result.outputLinesMatching(expectedOutput))
        assertEquals(EMPTY_JSON_REPORT, project.file("build/reports/sqldelight-check/report.json").readText())
    }

    @Test
    fun `check task logs resolved files at verbose level`() {
        val project =
            testProject(
                sqlDelightBuildScript(
                    extraImports =
                        """
                        import dev.s7a.sqldelight.check.api.LogLevel
                        """.trimIndent(),
                    extraConfiguration =
                        """
                        sqldelightCheck {
                            logLevel.set(LogLevel.Verbose)
                        }
                        """.trimIndent(),
                ),
            )
        project.write(
            "src/main/sqldelight/com/example/Player.sq",
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.trimIndent(),
        )

        val result = project.run("sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        val expectedOutput =
            listOf(
                "sqldelight-check [Database] files (1):",
                "sqldelight-check [Database]   - src/main/sqldelight/com/example/Player.sq",
                "sqldelight-check analyzed 1 SQLDelight database(s).",
            )
        assertContentEquals(expectedOutput, result.outputLinesMatching(expectedOutput))
    }

    @Test
    fun `check task logs per file rules at debug level`() {
        val project =
            testProject(
                sqlDelightBuildScript(
                    extraImports =
                        """
                        import dev.s7a.sqldelight.check.api.Enablement
                        import dev.s7a.sqldelight.check.api.LogLevel
                        """.trimIndent(),
                    extraConfiguration =
                        """
                        sqldelightCheck {
                            logLevel.set(LogLevel.Debug)
                            ruleSets {
                                standard {
                                    enabled.set(Enablement.Disabled)
                                }
                            }
                            rules {
                                rule("standard:final-newline") {
                                    enabled.set(Enablement.Enabled)
                                }
                            }
                        }
                        """.trimIndent(),
                ),
            )
        project.write(
            "src/main/sqldelight/com/example/Player.sq",
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.trimIndent(),
        )

        val result = project.run("sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        val expectedOutput =
            listOf(
                "sqldelight-check [Database] files (1):",
                "sqldelight-check [Database]   - src/main/sqldelight/com/example/Player.sq",
                "sqldelight-check [Database] src/main/sqldelight/com/example/Player.sq rules (1):",
                "sqldelight-check [Database] - [x] standard:final-newline",
                "sqldelight-check analyzed 1 SQLDelight database(s).",
            )
        assertContentEquals(expectedOutput, result.outputLinesMatching(expectedOutput))
    }

    @Test
    fun `check task overrides log level from gradle property`() {
        val project =
            testProject(
                sqlDelightBuildScript(
                    extraImports =
                        """
                        import dev.s7a.sqldelight.check.api.LogLevel
                        """.trimIndent(),
                    extraConfiguration =
                        """
                        sqldelightCheck {
                            logLevel.set(LogLevel.Info)
                        }
                        """.trimIndent(),
                ),
            )
        project.write(
            "src/main/sqldelight/com/example/Player.sq",
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.trimIndent() + "\n",
        )

        val result = project.run("-PsqldelightCheck.logLevel=verbose", "sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        val expectedOutput =
            listOf(
                "sqldelight-check [Database] files (1):",
                "sqldelight-check [Database]   - src/main/sqldelight/com/example/Player.sq",
                "sqldelight-check analyzed 1 SQLDelight database(s).",
            )
        assertContentEquals(expectedOutput, result.outputLinesMatching(expectedOutput))
    }

    @Test
    fun `check task resolves multiple sqldelight databases`() {
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
                        create("PrimaryDatabase") {
                            packageName.set("com.example.primary")
                            srcDirs("src/main/primarysqldelight")
                            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.3.2")
                        }
                        create("ReportingDatabase") {
                            packageName.set("com.example.reporting")
                            srcDirs("src/main/reportingsqldelight")
                            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.3.2")
                        }
                    }
                }
                """.trimIndent(),
            )
        project.write(
            "src/main/primarysqldelight/com/example/Player.sq",
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY,
              name TEXT NOT NULL
            );
            """.trimIndent() + "\n",
        )
        project.write(
            "src/main/reportingsqldelight/com/example/Report.sq",
            """
            CREATE TABLE report (
              id INTEGER NOT NULL PRIMARY KEY,
              title TEXT NOT NULL
            );
            """.trimIndent() + "\n",
        )

        val result = project.run("sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        val expectedOutput = listOf("sqldelight-check analyzed 2 SQLDelight database(s).")
        assertContentEquals(expectedOutput, result.outputLinesMatching(expectedOutput))
        assertEquals(EMPTY_JSON_REPORT, project.file("build/reports/sqldelight-check/report.json").readText())
    }

    @Test
    fun `sarif report uses root project relative paths for subprojects`() {
        val project =
            testProject(
                buildScript = "",
                settingsScript =
                    """
                    rootProject.name = "sqldelight-check-test"
                    include(":app")
                    """.trimIndent(),
            )
        project.write(
            "app/build.gradle.kts",
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
                    standard {
                        enabled.set(Enablement.Disabled)
                    }
                }
                rules {
                    rule("standard:final-newline") {
                        enabled.set(Enablement.Enabled)
                        severity.set(Severity.Error)
                    }
                }
            }
            """.trimIndent(),
        )
        project.write(
            "app/src/main/sqldelight/com/example/Player.sq",
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.trimIndent(),
        )

        project.runAndFail(":app:sqldelightCheck")

        val sarif = project.file("app/build/reports/sqldelight-check/report.sarif").readText()
        assertEquals(true, """"uri":"app/src/main/sqldelight/com/example/Player.sq"""" in sarif)
    }

    @Test
    fun `sarif report uses configured report root relative paths when Gradle root is nested`() {
        val workspace =
            testProject(
                buildScript = "",
                settingsScript = """rootProject.name = "workspace"""",
            )
        workspace.write(
            "app/settings.gradle.kts",
            """rootProject.name = "sqldelight-check-nested-test"""",
        )
        workspace.write(
            "app/build.gradle.kts",
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
                    standard {
                        enabled.set(Enablement.Disabled)
                    }
                }
                rules {
                    rule("standard:final-newline") {
                        enabled.set(Enablement.Enabled)
                        severity.set(Severity.Warning)
                    }
                }
            }
            """.trimIndent(),
        )
        workspace.write(
            "app/src/main/sqldelight/com/example/Player.sq",
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );
            """.trimIndent(),
        )

        workspace
            .project("app")
            .run("-PsqldelightCheck.reportRoot=${workspace.file("")}", "sqldelightCheck")

        val sarif = workspace.file("app/build/reports/sqldelight-check/report.sarif").readText()
        assertEquals(true, """"uri":"app/src/main/sqldelight/com/example/Player.sq"""" in sarif)
    }

    @Test
    fun `check task supports published stable sqldelight 2 versions`() {
        stableSqlDelight2Versions.forEach { version ->
            val project = testProject(sqlDelightBuildScript(sqlDelightVersion = version))
            project.write(
                "src/main/sqldelight/com/example/Player.sq",
                """
                CREATE TABLE player (
                  id INTEGER NOT NULL PRIMARY KEY,
                  name TEXT NOT NULL
                );
                """.trimIndent() + "\n",
            )

            val result =
                try {
                    project.run("sqldelightCheck")
                } catch (failure: UnexpectedBuildFailure) {
                    throw AssertionError("SQLDelight $version should be supported.", failure)
                }

            assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome, "SQLDelight $version should be supported.")
            val expectedOutput = listOf("sqldelight-check analyzed 1 SQLDelight database(s).")
            assertContentEquals(expectedOutput, result.outputLinesMatching(expectedOutput))
        }
    }

    @Test
    fun `check task supports sqldelight snapshot versions when enabled`() {
        if (!verifySnapshots) return

        snapshotSqlDelight2Versions.forEach { version ->
            val project =
                testProject(
                    buildScript =
                        sqlDelightBuildScript(
                            sqlDelightVersion = version,
                            extraRepositories =
                                """
                                maven("$SQLDELIGHT_SNAPSHOT_REPOSITORY_URL")
                                """.trimIndent(),
                        ),
                    settingsScript = snapshotSettingsScript(),
                )
            project.write(
                "src/main/sqldelight/com/example/Player.sq",
                """
                CREATE TABLE player (
                  id INTEGER NOT NULL PRIMARY KEY,
                  name TEXT NOT NULL
                );
                """.trimIndent() + "\n",
            )

            val result =
                try {
                    project.run("sqldelightCheck")
                } catch (failure: UnexpectedBuildFailure) {
                    throw AssertionError("SQLDelight $version should be supported.", failure)
                }

            assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome, "SQLDelight $version should be supported.")
            val expectedOutput = listOf("sqldelight-check analyzed 1 SQLDelight database(s).")
            assertContentEquals(expectedOutput, result.outputLinesMatching(expectedOutput))
        }
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
                        standard {
                            enabled.set(Enablement.Disabled)
                        }
                    }
                    rules {
                        rule("standard:final-newline") {
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

        val expectedOutput =
            listOf(
                "sqldelight-check analyzed 1 SQLDelight database(s).",
            )
        assertContentEquals(expectedOutput, result.outputLinesMatching(expectedOutput))
        val report = project.file("build/reports/sqldelight-check/report.json").readText()
        assertEquals(finalNewlineErrorJsonReport(), report)
    }

    @Test
    fun `fix task applies safe fixes and reports remaining diagnostics`() {
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
            "CREATE TABLE player (  \n  id INTEGER NOT NULL PRIMARY KEY  \n);",
        )

        val result = project.run("sqldelightFix")

        assertEquals(SUCCESS, result.task(":sqldelightFix")?.outcome)
        assertEquals(
            "CREATE TABLE player (\n  id INTEGER NOT NULL PRIMARY KEY\n);\n",
            project.file("src/main/sqldelight/com/example/Player.sq").readText(),
        )
        assertEquals(EMPTY_JSON_REPORT, project.file("build/reports/sqldelight-check/report.json").readText())
    }

    @Test
    fun `fix task does not apply unsafe fixes by default`() {
        val project =
            testProject(
                sqlDelightBuildScript(
                    extraImports =
                        """
                        import dev.s7a.sqldelight.check.api.Enablement
                        """.trimIndent(),
                    extraConfiguration =
                        """
                        sqldelightCheck {
                            rules {
                                rule("standard:prefer-named-parameters") {
                                    enabled.set(Enablement.Disabled)
                                }
                            }
                        }
                        """.trimIndent(),
                ),
            )
        val path = "src/main/sqldelight/com/example/Player.sq"
        val content =
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );

            selectById:
            SELECT id
            FROM player
            WHERE id=?;
            """.trimIndent() + "\n"
        project.write(path, content)

        val result = project.run("sqldelightFix")

        assertEquals(SUCCESS, result.task(":sqldelightFix")?.outcome)
        assertEquals(content, project.file(path).readText())
        assertEquals(unsafeComparisonSpacingJsonReport(), project.file("build/reports/sqldelight-check/report.json").readText())
    }

    @Test
    fun `fix task applies unsafe fixes when enabled`() {
        val project =
            testProject(
                sqlDelightBuildScript(
                    extraImports =
                        """
                        import dev.s7a.sqldelight.check.api.Enablement
                        """.trimIndent(),
                    extraConfiguration =
                        """
                    sqldelightCheck {
                        rules {
                            rule("standard:prefer-named-parameters") {
                                enabled.set(Enablement.Disabled)
                            }
                        }
                        fix {
                            unsafe.set(true)
                        }
                    }
                    """.trimIndent(),
                ),
            )
        val path = "src/main/sqldelight/com/example/Player.sq"
        project.write(
            path,
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );

            selectById:
            SELECT id
            FROM player
            WHERE id=?;
            """.trimIndent() + "\n",
        )

        val result = project.run("sqldelightFix")

        assertEquals(SUCCESS, result.task(":sqldelightFix")?.outcome)
        assertEquals(
            """
            CREATE TABLE player (
              id INTEGER NOT NULL PRIMARY KEY
            );

            selectById:
            SELECT id
            FROM player
            WHERE id = ?;
            """.trimIndent() + "\n",
            project.file(path).readText(),
        )
        assertEquals(EMPTY_JSON_REPORT, project.file("build/reports/sqldelight-check/report.json").readText())
    }

    @Test
    fun `check task loads external reporters from configuration`() {
        val project =
            testProject(
                """
                plugins {
                    id("dev.s7a.sqldelight.check")
                }

                dependencies {
                    add("sqldelightCheckReporter", files("external-reporter.jar"))
                }

                sqldelightCheck {
                    reports {
                        report("external") {
                            required.set(true)
                            options.put("mode", "ci")
                            options.put("asset", "details.txt")
                            outputFile.set(layout.buildDirectory.file("reports/sqldelight-check/external.txt"))
                            outputDirectory.set(layout.buildDirectory.dir("reports/sqldelight-check/external"))
                        }
                    }
                }
                """.trimIndent(),
            )
        project.createExternalReporterJar()

        val result = project.run("sqldelightCheck")

        assertEquals(SUCCESS, result.task(":sqldelightCheck")?.outcome)
        assertEquals("external diagnostics=0 mode=ci", project.file("build/reports/sqldelight-check/external.txt").readText())
        assertEquals("external asset=details.txt", project.file("build/reports/sqldelight-check/external/details.txt").readText())
    }

    /**
     * Returns a build script with SQLDelight configured for the functional test project.
     */
    private fun sqlDelightBuildScript(
        sqlDelightVersion: String = "2.3.2",
        extraRepositories: String = "",
        extraImports: String = "",
        extraConfiguration: String = "",
    ): String =
        """
        $extraImports

        plugins {
            kotlin("jvm") version "2.4.0"
            id("app.cash.sqldelight") version "$sqlDelightVersion"
            id("dev.s7a.sqldelight.check")
        }

        repositories {
            mavenCentral()
            $extraRepositories
        }

        sqldelight {
            databases {
                create("Database") {
                    packageName.set("com.example")
                    srcDirs("src/main/sqldelight")
                    dialect("app.cash.sqldelight:sqlite-3-38-dialect:$sqlDelightVersion")
                }
            }
        }

        $extraConfiguration
        """.trimIndent()

    /**
     * Returns a settings script that can resolve SQLDelight snapshot plugin markers.
     */
    private fun snapshotSettingsScript(): String =
        """
        pluginManagement {
            repositories {
                gradlePluginPortal()
                maven("$SQLDELIGHT_SNAPSHOT_REPOSITORY_URL")
            }
        }

        rootProject.name = "sqldelight-check-test"
        """.trimIndent()

    /**
     * Creates a temporary Gradle project for a TestKit run.
     */
    private fun testProject(
        buildScript: String,
        settingsScript: String = """rootProject.name = "sqldelight-check-test"""",
    ): TestProject {
        val directory = Files.createTempDirectory("sqldelight-check-gradle-plugin-test")
        directory.resolve("settings.gradle.kts").writeText(settingsScript)
        directory.resolve("build.gradle.kts").writeText(buildScript)
        return TestProject(directory)
    }

    private companion object {
        private const val SQLDELIGHT_SNAPSHOT_REPOSITORY_URL = "https://central.sonatype.com/repository/maven-snapshots/"
        private const val EMPTY_JSON_REPORT =
            """{"summary":{"diagnostics":0,"errors":0,"warnings":0,"infos":0},"diagnostics":[]}"""
        private const val EMPTY_PRETTY_JSON_REPORT =
            "{\n" +
                "    \"summary\": {\n" +
                "        \"diagnostics\": 0,\n" +
                "        \"errors\": 0,\n" +
                "        \"warnings\": 0,\n" +
                "        \"infos\": 0\n" +
                "    },\n" +
                "    \"diagnostics\": []\n" +
                "}"
        private const val EMPTY_SARIF_REPORT =
            """{"version":"2.1.0","${'$'}schema":"https://json.schemastore.org/sarif-2.1.0.json","runs":[{"tool":{"driver":{"name":"sqldelight-check","semanticVersion":"0.1.0","rules":[]}},"results":[]}]}"""

        val stableSqlDelight2Versions = listOf("2.0.0", "2.0.2", "2.1.0", "2.2.1", "2.3.1", "2.3.2")
        val snapshotSqlDelight2Versions = listOf("2.4.0-SNAPSHOT")
        val verifySnapshots = System.getProperty("sqldelightCheck.verifySnapshots").toBoolean()

        fun finalNewlineErrorJsonReport(): String =
            """{"summary":{"diagnostics":1,"errors":1,"warnings":0,"infos":0},"diagnostics":[{"ruleId":"standard:final-newline","severity":"error","message":"File should end with a newline.","file":"src/main/sqldelight/com/example/Player.sq","range":{"start":{"line":3,"column":3},"end":{"line":3,"column":3}},"database":${sqliteDatabaseJson()},"fixes":[{"title":"Insert final newline","safety":"safe","edits":[{"range":{"start":{"line":3,"column":3},"end":{"line":3,"column":3}},"replacement":"\n"}]}]}]}"""

        fun unsafeComparisonSpacingJsonReport(): String =
            """{"summary":{"diagnostics":1,"errors":0,"warnings":1,"infos":0},"diagnostics":[{"ruleId":"standard:space-around-comparison-operators","severity":"warning","message":"Comparison operator '=' should have one space on both sides.","file":"src/main/sqldelight/com/example/Player.sq","range":{"start":{"line":8,"column":9},"end":{"line":8,"column":10}},"database":${sqliteDatabaseJson()},"fixes":[{"title":"Normalize comparison operator spacing","safety":"unsafe","edits":[{"range":{"start":{"line":8,"column":9},"end":{"line":8,"column":10}},"replacement":" = "}]}]}]}"""

        fun sqliteDatabaseJson(version: String = "2.3.2"): String =
            """{"name":"Database","dialect":{"family":"SQLite","displayName":"sqlite 3 38","artifact":"app.cash.sqldelight:sqlite-3-38-dialect","version":"$version","implementationClass":null,"capabilities":["sqlite"]}}"""
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
         * Returns a nested Gradle project fixture inside this temporary project.
         */
        fun project(path: String): TestProject = TestProject(file(path))

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
         * Creates a reporter provider jar used to verify external provider discovery.
         */
        fun createExternalReporterJar() {
            val sourceDirectory = directory.resolve("external-reporter-src")
            val classesDirectory = directory.resolve("external-reporter-classes")
            Files.createDirectories(sourceDirectory.resolve("com/example"))
            Files.createDirectories(classesDirectory)
            val sourceFile = sourceDirectory.resolve("com/example/ExternalReporterProvider.java")
            sourceFile.writeText(
                """
                package com.example;

                import dev.s7a.sqldelight.check.reporter.api.Report;
                import dev.s7a.sqldelight.check.reporter.api.ReportOutput;
                import dev.s7a.sqldelight.check.reporter.api.ReporterId;
                import dev.s7a.sqldelight.check.reporter.api.Reporter;
                import dev.s7a.sqldelight.check.reporter.api.ReporterProvider;
                import java.io.IOException;
                import java.io.OutputStream;
                import java.nio.charset.StandardCharsets;
                import java.util.Map;

                public final class ExternalReporterProvider implements ReporterProvider {
                    @Override
                    public ReporterId getId() {
                        return new ReporterId("external");
                    }

                    @Override
                    public Reporter create(Map<String, String> options) {
                        return new ExternalReporter(options);
                    }

                    private static final class ExternalReporter implements Reporter {
                        private final Map<String, String> options;

                        private ExternalReporter(Map<String, String> options) {
                            this.options = options;
                        }

                        @Override
                        public void write(Report report, ReportOutput output) {
                            try {
                                String text = "external diagnostics=" + report.getDiagnostics().size() + " mode=" + options.get("mode");
                                try (OutputStream file = output.file()) {
                                    file.write(text.getBytes(StandardCharsets.UTF_8));
                                }
                                String asset = options.get("asset");
                                if (asset != null) {
                                    try (OutputStream file = output.file(asset)) {
                                        file.write(("external asset=" + asset).getBytes(StandardCharsets.UTF_8));
                                    }
                                }
                            } catch (IOException exception) {
                                throw new RuntimeException(exception);
                            }
                        }
                    }
                }
                """.trimIndent(),
            )

            val compiler = ToolProvider.getSystemJavaCompiler() ?: error("JDK compiler is required for this test.")
            val compileResult =
                compiler.run(
                    null,
                    null,
                    null,
                    "-classpath",
                    System.getProperty("java.class.path"),
                    "-d",
                    classesDirectory.toString(),
                    sourceFile.toString(),
                )
            check(compileResult == 0) { "Failed to compile external reporter fixture." }

            val serviceDirectory =
                classesDirectory.resolve(
                    "META-INF/services",
                )
            Files.createDirectories(serviceDirectory)
            serviceDirectory
                .resolve("dev.s7a.sqldelight.check.reporter.api.ReporterProvider")
                .writeText("com.example.ExternalReporterProvider\n")

            JarOutputStream(Files.newOutputStream(file("external-reporter.jar"))).use { jar ->
                Files.walk(classesDirectory).use { paths ->
                    paths
                        .filter { path -> Files.isRegularFile(path) }
                        .forEach { path ->
                            val entryName =
                                classesDirectory
                                    .relativize(path)
                                    .toString()
                                    .replace('\\', '/')
                            jar.putNextEntry(JarEntry(entryName))
                            Files.copy(path, jar)
                            jar.closeEntry()
                        }
                }
            }
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
         * Runs Gradle with additional environment variables.
         */
        fun runWithEnvironment(
            environment: Map<String, String>,
            vararg arguments: String,
        ) = GradleRunner
            .create()
            .withProjectDir(directory.toFile())
            .withArguments(*arguments, "--stacktrace")
            .withPluginClasspath()
            .withEnvironment(System.getenv() + environment)
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

private fun BuildResult.outputLinesMatching(expectedLines: List<String>): List<String> =
    output
        .lines()
        .filter { line -> line in expectedLines }
