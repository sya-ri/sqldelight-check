package dev.s7a.sqldelight.check.rules.sqlite.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets
import dev.s7a.sqldelight.check.rule.api.SqlToken
import dev.s7a.sqldelight.check.dialects.sqlite.DoUpdateClause
import dev.s7a.sqldelight.check.dialects.sqlite.InsertOrReplaceStatementStart
import dev.s7a.sqldelight.check.dialects.sqlite.OnConflictClause
import dev.s7a.sqldelight.check.dialects.sqlite.ReplaceIntoStatementStart
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.rule.api.findSourcePattern
import dev.s7a.sqldelight.check.rule.api.findSourcePatternsInOrder
import dev.s7a.sqldelight.check.rule.api.sqlStatements
import dev.s7a.sqldelight.check.rule.api.sqlTokens

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.dialects.sqlite.SQLiteDialectCapability
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SQLite files that mix multiple conflict-resolution upsert styles.
 *
 * Mixing `OR REPLACE` and `ON CONFLICT` styles makes migration intent harder to
 * audit.
 */
public class ConsistentConflictResolutionRule : Rule {
    override val id: RuleId = RuleId("consistent-conflict-resolution")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true
    override val targetCapability: DialectCapability = SQLiteDialectCapability

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (!isApplicable(context)) return

        val content = context.file.content
        val conflictStyles =
            content.sqlTokens()
                .toList()
                .sqlStatements()
                .flatMap { statement -> statement.conflictStyles(context.database.dialect.sourcePatterns) }
        if (conflictStyles.map { conflictStyle -> conflictStyle.kind }.toSet().size <= 1) return

        conflictStyles.forEach { conflictStyle ->
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message =
                        "Use one SQLite conflict-resolution style consistently within the same file.",
                    file = context.file,
                    range =
                        content.rangeAtOffsets(
                            conflictStyle.startToken.startOffset,
                            conflictStyle.endToken.endOffset,
                        ),
                    database = context.database,
                ),
            )
        }
    }
}

private data class ConflictStyle(
    val kind: ConflictStyleKind,
    val startToken: SqlToken,
    val endToken: SqlToken,
)

private enum class ConflictStyleKind {
    InsertOrReplace,
    ReplaceInto,
    OnConflictDoUpdate,
}

private fun List<SqlToken>.conflictStyles(sourcePatterns: SqlDialectSourcePatterns): List<ConflictStyle> =
    buildList {
        val insertOrReplace = findSourcePattern(InsertOrReplaceStatementStart, sourcePatterns)
        if (insertOrReplace != null) {
            add(insertOrReplace.toConflictStyle(ConflictStyleKind.InsertOrReplace))
        } else {
            findSourcePattern(ReplaceIntoStatementStart, sourcePatterns)
                ?.toConflictStyle(ConflictStyleKind.ReplaceInto)
                ?.let(::add)
        }
        onConflictDoUpdate(sourcePatterns)?.let(::add)
    }

private fun dev.s7a.sqldelight.check.rule.api.SqlTokenMatch.toConflictStyle(kind: ConflictStyleKind): ConflictStyle =
    ConflictStyle(
        kind = kind,
        startToken = startToken,
        endToken = endToken,
    )

private fun List<SqlToken>.onConflictDoUpdate(sourcePatterns: SqlDialectSourcePatterns): ConflictStyle? {
    val update = findSourcePatternsInOrder(sourcePatterns, OnConflictClause, DoUpdateClause) ?: return null
    return ConflictStyle(
        kind = ConflictStyleKind.OnConflictDoUpdate,
        startToken = update.startToken,
        endToken = update.endToken,
    )
}
