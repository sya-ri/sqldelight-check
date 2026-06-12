package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports NOT IN subqueries that do not exclude NULL values.
 */
public class NoNotInNullableSubqueryRule : Rule {
    override val id: RuleId = RuleId("no-not-in-nullable-subquery")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Not)) return@forEachIndexed
            val inToken = tokens.getOrNull(index + 1)?.takeIf { it.isTerm(SqlDialectSourceTerm.In) } ?: return@forEachIndexed
            val openOffset = content.nextSqlCharacterAfter(inToken.endOffset)?.offset ?: return@forEachIndexed
            if (content.getOrNull(openOffset) != '(') return@forEachIndexed
            val closeOffset = content.matchingClosingParenthesisOffset(openOffset) ?: return@forEachIndexed
            val innerTokens =
                tokens.filter { candidate -> candidate.startOffset in (openOffset + 1) until closeOffset }
            if (innerTokens.none { it.isTerm(SqlDialectSourceTerm.Select) }) return@forEachIndexed
            if (innerTokens.containsIsNotNullPredicate()) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Prefer NOT EXISTS or exclude NULL values when using NOT IN with a subquery.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, inToken.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun List<SqlToken>.containsIsNotNullPredicate(): Boolean =
    windowed(size = 3, step = 1)
        .any { window ->
            window[0].isTerm(SqlDialectSourceTerm.Is) &&
                window[1].isTerm(SqlDialectSourceTerm.Not) &&
                window[2].isTerm(SqlDialectSourceTerm.Null)
        }
