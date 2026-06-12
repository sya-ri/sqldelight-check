package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFileKind
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports explicit transaction statements in SQLDelight migration files.
 */
public class NoTransactionInMigrationRule : Rule {
    override val id: RuleId = RuleId("no-transaction-in-migration")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.file.kind != SourceFileKind.Migration) return

        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!content.isStatementStart(token.startOffset)) return@forEachIndexed

            val isTransactionStatement =
                transactionTerms.any { term -> token.isTerm(term) } ||
                    (token.isTerm(SqlDialectSourceTerm.End) &&
                        tokens.getOrNull(index + 1)?.isTerm(SqlDialectSourceTerm.Transaction) == true)
            if (!isTransactionStatement) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Do not wrap SQLDelight migration files in explicit transaction statements.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private val transactionTerms =
    setOf(
        SqlDialectSourceTerm.Begin,
        SqlDialectSourceTerm.Commit,
        SqlDialectSourceTerm.Rollback,
    )

private fun String.isStatementStart(offset: Int): Boolean {
    val previous = previousSqlCharacterBefore(offset) ?: return true
    return previous.value == ';'
}
