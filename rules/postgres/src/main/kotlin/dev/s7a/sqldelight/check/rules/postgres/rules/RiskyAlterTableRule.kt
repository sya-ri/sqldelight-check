package dev.s7a.sqldelight.check.rules.postgres.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AlterTableStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnAlterOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnDropOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnSetNotNullOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ColumnTypeChangeOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.ConstraintAddOperation
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.NotValidConstraintClause
import dev.s7a.sqldelight.check.rule.api.containsSourcePattern
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.findSourcePatternsInOrder
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
 * Reports PostgreSQL ALTER TABLE statements with operations that commonly take strong locks.
 *
 * The rule focuses on migration patterns that should be planned separately for
 * live PostgreSQL databases.
 */
public class RiskyAlterTableRule : Rule {
    override val id: RuleId = RuleId("risky-alter-table")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = DialectCapability.PostgreSql

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        content.sqlTokens()
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
                            "Review this PostgreSQL ALTER TABLE operation because it commonly takes strong locks.",
                        file = context.file,
                        range = content.rangeAtOffsets(alterTable.startToken.startOffset, alterTable.endToken.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun List<SqlToken>.hasRiskyAlterTableOperation(context: RuleContext): Boolean =
    hasDropColumn(context) ||
        hasAlterColumnType(context) ||
        hasAlterColumnSetNotNull(context) ||
        hasAddConstraintWithoutNotValid(context)

private fun List<SqlToken>.hasDropColumn(context: RuleContext): Boolean =
    findSourcePattern(ColumnDropOperation, context.database.dialect.sourcePatterns) != null

private fun List<SqlToken>.hasAlterColumnType(context: RuleContext): Boolean =
    findSourcePatternsInOrder(
        context.database.dialect.sourcePatterns,
        ColumnAlterOperation,
        ColumnTypeChangeOperation,
    ) != null

private fun List<SqlToken>.hasAlterColumnSetNotNull(context: RuleContext): Boolean =
    findSourcePatternsInOrder(
        context.database.dialect.sourcePatterns,
        ColumnAlterOperation,
        ColumnSetNotNullOperation,
    ) != null

private fun List<SqlToken>.hasAddConstraintWithoutNotValid(context: RuleContext): Boolean =
    findSourcePattern(ConstraintAddOperation, context.database.dialect.sourcePatterns) != null &&
        !containsSourcePattern(NotValidConstraintClause, context.database.dialect.sourcePatterns)
