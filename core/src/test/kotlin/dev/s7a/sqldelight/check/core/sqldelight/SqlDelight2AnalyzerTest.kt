package dev.s7a.sqldelight.check.core.sqldelight

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.core.AnalysisInput
import java.io.File
import java.nio.file.Files
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
        val root = Files.createTempDirectory("sqldelight-check-analyzer-test")
        val sourceRoot = root.resolve("src/main/sqldelight")
        val sourceFile = sourceRoot.resolve("com/example/Broken.sq")
        sourceFile.parent.createDirectories()
        sourceFile.writeText("CREATE TABL broken;")

        val result =
            SqlDelight2Analyzer
                .analyze(
                    input =
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
                            sqlDelightVersion = "2.3.2",
                            packageName = "com.example",
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

    private fun testRuntimeClasspath(): List<File> =
        System
            .getProperty("java.class.path")
            .split(File.pathSeparator)
            .map(::File)
            .filter { file -> file.exists() }
}
