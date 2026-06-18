package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens

/**
 * Reports table rename operations in SQLDelight migration files.
 */
public class NoRenameTableInMigrationRule : Rule {
    override val id: RuleId = RuleId("no-rename-table-in-migration")
    override val defaultSeverity: Severity = Severity.Error
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Migration) return

        val content = context.file.content
        content.sqlTokens(hashLineComments = true)
            .toList()
            .sqlStatements()
            .mapNotNull { statement -> statement.findRenameTableOperation() }
            .forEach { match ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Avoid table renames in SQLDelight migrations because they can break live application compatibility.",
                        file = context.file,
                        range = content.rangeAtOffsets(match.first.startOffset, match.second.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun List<SqlToken>.findRenameTableOperation(): Pair<SqlToken, SqlToken>? {
    return findAlterTableOperation("rename", "to")
}
