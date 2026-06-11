package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.core.AnalysisInput
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

/**
 * Tests for SQLDelight Gradle model normalization before rule execution.
 */
class SqlDelightProjectResolverTest {
    @Test
    fun `merge keeps every source file once in path order`() {
        val first =
            resolvedInput(
                files = listOf(SourceFile("src/main/sqldelight/com/example/A.sq", "SELECT 1;\n")),
            )
        val second =
            resolvedInput(
                files =
                    listOf(
                        SourceFile("src/main/sqldelight/com/example/B.sq", "SELECT 2;\n"),
                        SourceFile("src/main/sqldelight/com/example/A.sq", "SELECT 1;\n"),
                    ),
            )

        val merged = mergeResolvedSqlDelightInputs(listOf(first, second)).analysisInput

        assertContentEquals(
            listOf("src/main/sqldelight/com/example/A.sq", "src/main/sqldelight/com/example/B.sq"),
            merged.files.map { sourceFile -> sourceFile.path },
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

    private fun resolvedInput(
        databaseName: String = "Database",
        files: List<SourceFile> = emptyList(),
    ): ResolvedSqlDelightInput =
        ResolvedSqlDelightInput(
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
                ),
        )
}
