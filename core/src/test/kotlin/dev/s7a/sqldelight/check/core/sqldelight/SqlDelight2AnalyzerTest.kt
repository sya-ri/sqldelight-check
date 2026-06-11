package dev.s7a.sqldelight.check.core.sqldelight

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.core.AnalysisInput
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.ServiceConfigurationError
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the built-in SQLDelight 2.x analyzer.
 */
class SqlDelight2AnalyzerTest {
    @Test
    fun `analyzer declares stable sqldelight 2 compatibility range`() {
        listOf("2.0.0", "2.0.2", "2.1.0", "2.2.1", "2.3.1", "2.3.2", "2.4.0-SNAPSHOT").forEach { version ->
            assertEquals(true, SqlDelight2VersionSupport.supports(version), version)
        }
        listOf("2.0.0-rc02", "1.5.5", "2.4.0", "3.0.0").forEach { version ->
            assertEquals(false, SqlDelight2VersionSupport.supports(version), version)
        }
    }

    @Test
    fun `analyzer returns sql diagnostics from sqldelight`() {
        val sourceRoot = sourceRoot("CREATE TABL broken;")

        val result =
            SqlDelight2Analyzer
                .analyze(
                    input =
                        testInput(
                            sourceFolders = listOf(sourceRoot.toFile()),
                            compilerClasspath = testRuntimeClasspath(),
                        ),
                )

        assertEquals(1, result.files.size)
        assertTrue(result.diagnostics.isNotEmpty())
        val diagnostic = result.diagnostics.single()
        assertEquals("src/main/sqldelight/com/example/Broken.sq", diagnostic.file?.path)
        assertEquals(1, diagnostic.range?.start?.line)
        assertTrue((diagnostic.range?.start?.column ?: 0) >= 1)
        assertTrue(diagnostic.message.isNotBlank(), diagnostic.message)
    }

    @Test
    fun `analyzer reports unresolved package name`() {
        val result =
            SqlDelight2Analyzer.analyze(
                testInput(
                    packageName = null,
                    sourceFolders = listOf(sourceRoot("CREATE TABLE player (id INTEGER);\n").toFile()),
                ),
            )

        assertEquals("SQLDelight package name was not resolved.", result.diagnostics.single().message)
    }

    @Test
    fun `analyzer reports unsupported sqldelight version`() {
        val result =
            SqlDelight2Analyzer.analyze(
                testInput(
                    sqlDelightVersion = "3.0.0",
                    sourceFolders = listOf(sourceRoot("CREATE TABLE player (id INTEGER);\n").toFile()),
                ),
            )

        assertEquals("SQLDelight 3.0.0 is not supported by sqldelight-check 0.1.0.", result.diagnostics.single().message)
    }

    @Test
    fun `analyzer reports unresolved dialect classpath`() {
        val result =
            SqlDelight2Analyzer.analyze(
                testInput(
                    sourceFolders = listOf(sourceRoot("CREATE TABLE player (id INTEGER);\n").toFile()),
                    compilerClasspath = emptyList(),
                    dialectClasspath = emptyList(),
                ),
            )

        assertEquals("SQLDelight dialect classpath was not resolved.", result.diagnostics.single().message)
    }

    @Test
    fun `failure message identifies dialect service loading failure`() {
        val message =
            sqlDelightAnalysisFailureMessage(
                testInput(),
                ServiceConfigurationError("broken service"),
            )

        assertEquals("SQLDelight dialect service loading failed for Database: broken service", message)
    }

    @Test
    fun `failure message identifies missing runtime classes`() {
        val message =
            sqlDelightAnalysisFailureMessage(
                testInput(),
                NoClassDefFoundError("app/cash/sqldelight/core/Missing"),
            )

        assertEquals(
            "SQLDelight 2.x analysis failed because a required class was not found for Database: " +
                "app/cash/sqldelight/core/Missing",
            message,
        )
    }

    @Test
    fun `failure message identifies incompatible runtime classes`() {
        val message =
            sqlDelightAnalysisFailureMessage(
                testInput(),
                LinkageError("method changed"),
            )

        assertEquals(
            "SQLDelight 2.x analysis failed because loaded SQLDelight classes are incompatible for Database: method changed",
            message,
        )
    }

    private fun sourceRoot(content: String): Path {
        val root = Files.createTempDirectory("sqldelight-check-analyzer-test")
        val sourceRoot = root.resolve("src/main/sqldelight")
        val sourceFile = sourceRoot.resolve("com/example/Broken.sq")
        sourceFile.parent.createDirectories()
        sourceFile.writeText(content)
        return sourceRoot
    }

    private fun testInput(
        sqlDelightVersion: String = "2.3.2",
        packageName: String? = "com.example",
        sourceFolders: List<File> = emptyList(),
        compilerClasspath: List<File> = emptyList(),
        dialectClasspath: List<File> = emptyList(),
    ): AnalysisInput =
        AnalysisInput(
            database =
                DatabaseContext(
                    name = "Database",
                    dialect =
                        SqlDialect(
                            family = DialectFamily.SQLite,
                            displayName = "sqlite 3 38",
                        ),
                ),
            files =
                listOf(
                    SourceFile(
                        path = "src/main/sqldelight/com/example/Broken.sq",
                        content = "CREATE TABL broken;",
                    ),
                ),
            sqlDelightVersion = sqlDelightVersion,
            packageName = packageName,
            sourceFolders = sourceFolders,
            compilerClasspath = compilerClasspath,
            dialectClasspath = dialectClasspath,
        )

    private fun testRuntimeClasspath(): List<File> =
        System
            .getProperty("java.class.path")
            .split(File.pathSeparator)
            .map(::File)
            .filter { file -> file.exists() }
}
