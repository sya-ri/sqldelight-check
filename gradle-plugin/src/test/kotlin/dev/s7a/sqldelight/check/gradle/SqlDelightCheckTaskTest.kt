package dev.s7a.sqldelight.check.gradle

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * Tests for task-level diagnostic grouping helpers.
 */
class SqlDelightCheckTaskTest {
    @Test
    fun `rule hit grouping keeps databases with matching file paths separate`() {
        val hits =
            diagnosticRuleHitsByFile(
                listOf(
                    diagnostic(
                        databaseName = "PrimaryDatabase",
                        ruleId = qualifiedRuleId("standard:final-newline"),
                    ),
                    diagnostic(
                        databaseName = "ReportingDatabase",
                        ruleId = qualifiedRuleId("standard:no-select-star"),
                    ),
                ),
            )

        assertContentEquals(
            listOf("standard:final-newline"),
            hits[FileRuleKey(databaseName = "PrimaryDatabase", filePath = "src/main/sqldelight/com/example/Query.sq")],
        )
        assertContentEquals(
            listOf("standard:no-select-star"),
            hits[FileRuleKey(databaseName = "ReportingDatabase", filePath = "src/main/sqldelight/com/example/Query.sq")],
        )
    }

    private fun diagnostic(
        databaseName: String,
        ruleId: QualifiedRuleId,
    ): Diagnostic =
        Diagnostic(
            ruleId = ruleId,
            severity = Severity.Warning,
            message = "test",
            file = SourceFile(path = "src/main/sqldelight/com/example/Query.sq", content = "SELECT * FROM player;\n"),
            range = null,
            database =
                DatabaseContext(
                    name = databaseName,
                    dialect =
                        SqlDialect(
                            family = DialectFamily.SQLite,
                            displayName = "sqlite 3 38",
                        ),
                ),
        )
}

private fun qualifiedRuleId(value: String): QualifiedRuleId {
    val delimiter = value.indexOf(':')
    require(delimiter > 0 && delimiter < value.lastIndex)
    return QualifiedRuleId(
        ruleSetId = RuleSetId(value.substring(0, delimiter)),
        ruleId = RuleId(value.substring(delimiter + 1)),
    )
}
