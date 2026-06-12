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
 * Reports `SELECT DISTINCT` statements that also contain `GROUP BY`.
 */
public class NoSelectDistinctWithGroupByRule : Rule {
    override val id: RuleId = RuleId("no-select-distinct-with-group-by")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.Select)) return@forEachIndexed
            val distinct = tokens
                .getOrNull(index + 1)
                ?.takeIf { it.isTerm(SqlDialectSourceTerm.Distinct) }
                ?: return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val statementTokens = tokens.drop(index + 2).takeWhile { candidate -> candidate.startOffset < statementEnd }
            if (!statementTokens.containsTermPair(SqlDialectSourceTerm.Group, SqlDialectSourceTerm.By)) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SELECT DISTINCT with GROUP BY is ambiguous; remove one of them.",
                    file = context.file,
                    range = content.rangeAtOffsets(distinct.startOffset, distinct.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
