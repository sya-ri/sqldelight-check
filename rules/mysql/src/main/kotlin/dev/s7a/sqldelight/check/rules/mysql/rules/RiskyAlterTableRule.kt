package dev.s7a.sqldelight.check.rules.mysql.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AlterTableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnChangeOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnDropOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnModifyOperation
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports MySQL ALTER TABLE statements with operations that commonly rebuild or lock a table.
 *
 * The rule focuses on migration patterns that can cause long blocking work on
 * live MySQL databases.
 */
public class RiskyAlterTableRule : Rule {
    override val id: RuleId = RuleId("risky-alter-table")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.MySql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        content.sqlTokens(hashLineComments = true)
            .toList()
            .sqlStatements()
            .forEach { statement ->
                val alterTable = statement.findSourcePattern(
                    AlterTableStatementStart,
                    context.database.dialect.sourcePatterns,
                ) ?: return@forEach
                if (!statement.hasRiskyAlterTableOperation(context)) return@forEach

                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message =
                            "Review this MySQL ALTER TABLE operation because it can rebuild or strongly lock " +
                                "the table.",
                        file = context.file,
                        range = content.rangeAtOffsets(alterTable.startToken.startOffset, alterTable.endToken.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun List<SqlToken>.hasRiskyAlterTableOperation(context: RuleContext): Boolean {
    val sourcePatterns = context.database.dialect.sourcePatterns
    return findSourcePattern(ColumnModifyOperation, sourcePatterns) != null ||
        findSourcePattern(ColumnChangeOperation, sourcePatterns) != null ||
        findSourcePattern(ColumnDropOperation, sourcePatterns) != null
}
