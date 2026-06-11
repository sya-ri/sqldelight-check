package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.core.AnalysisInput
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

/**
 * Tests for SQLDelight Gradle model normalization before analysis.
 */
class SqlDelightProjectResolverTest {
    @Test
    fun `merge keeps every source folder dependency folder and classpath entry`() {
        val first =
            resolvedInput(
                files = listOf(SourceFile("src/main/sqldelight/com/example/A.sq", "SELECT 1;\n")),
                sourceFolders = listOf(file("src/main/sqldelight")),
                dependencyFolders = listOf(file("src/main/dependency-sqldelight")),
                compilerClasspath = listOf(file("compiler-a.jar")),
                dialectClasspath = listOf(file("dialect-a.jar")),
            )
        val second =
            resolvedInput(
                files =
                    listOf(
                        SourceFile("src/main/sqldelight/com/example/B.sq", "SELECT 2;\n"),
                        SourceFile("src/main/sqldelight/com/example/A.sq", "SELECT 1;\n"),
                    ),
                sourceFolders = listOf(file("src/main/extra-sqldelight")),
                dependencyFolders = listOf(file("src/main/extra-dependency-sqldelight")),
                compilerClasspath = listOf(file("compiler-b.jar"), file("compiler-a.jar")),
                dialectClasspath = listOf(file("dialect-b.jar")),
            )

        val merged = mergeResolvedSqlDelightInputs(listOf(first, second)).analysisInput

        assertContentEquals(
            listOf("src/main/sqldelight/com/example/A.sq", "src/main/sqldelight/com/example/B.sq"),
            merged.files.map { sourceFile -> sourceFile.path },
        )
        assertContentEquals(
            listOf(file("src/main/sqldelight"), file("src/main/extra-sqldelight")),
            merged.sourceFolders,
        )
        assertContentEquals(
            listOf(file("src/main/dependency-sqldelight"), file("src/main/extra-dependency-sqldelight")),
            merged.dependencyFolders,
        )
        assertContentEquals(
            listOf(file("compiler-a.jar"), file("compiler-b.jar")),
            merged.compilerClasspath,
        )
        assertContentEquals(
            listOf(file("dialect-a.jar"), file("dialect-b.jar")),
            merged.dialectClasspath,
        )
    }

    @Test
    fun `merge rejects incompatible database contexts`() {
        assertFailsWith<IllegalArgumentException> {
            mergeResolvedSqlDelightInputs(
                listOf(
                    resolvedInput(databaseName = "PrimaryDatabase"),
                    resolvedInput(databaseName = "ReportingDatabase"),
                ),
            )
        }
    }

    @Test
    fun `merge rejects incompatible package names`() {
        assertFailsWith<IllegalArgumentException> {
            mergeResolvedSqlDelightInputs(
                listOf(
                    resolvedInput(packageName = "com.example.primary"),
                    resolvedInput(packageName = "com.example.reporting"),
                ),
            )
        }
    }

    private fun resolvedInput(
        databaseName: String = "Database",
        packageName: String = "com.example",
        files: List<SourceFile> = emptyList(),
        sourceFolders: List<File> = emptyList(),
        dependencyFolders: List<File> = emptyList(),
        compilerClasspath: List<File> = emptyList(),
        dialectClasspath: List<File> = emptyList(),
    ): ResolvedSqlDelightInput =
        ResolvedSqlDelightInput(
            sqlDelightVersion = "2.3.2",
            analysisInput =
                AnalysisInput(
                    database =
                        DatabaseContext(
                            name = databaseName,
                            dialect =
                                SqlDialect(
                                    family = DialectFamily.SQLite,
                                    displayName = "sqlite 3 38",
                                ),
                        ),
                    files = files,
                    sqlDelightVersion = "2.3.2",
                    packageName = packageName,
                    sourceFolders = sourceFolders,
                    dependencyFolders = dependencyFolders,
                    compilerClasspath = compilerClasspath,
                    dialectClasspath = dialectClasspath,
                ),
        )

    private fun file(path: String): File = File(path).absoluteFile
}
