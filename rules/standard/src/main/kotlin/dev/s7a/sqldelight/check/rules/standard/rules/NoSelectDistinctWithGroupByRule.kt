package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports `SELECT DISTINCT` statements that also contain `GROUP BY`.
 */
public class NoSelectDistinctWithGroupByRule : Rule {
    override val id: RuleId = RuleId("standard:no-select-distinct-with-group-by")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.text.equals("select", ignoreCase = true)) return@forEachIndexed
            val distinct = tokens
                .getOrNull(index + 1)
                ?.takeIf { it.text.equals("distinct", ignoreCase = true) }
                ?: return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val statementTokens = tokens.drop(index + 2).takeWhile { candidate -> candidate.startOffset < statementEnd }
            if (!statementTokens.containsGroupBy()) return@forEachIndexed

            reporter.report(
                Diagnostic(
                    ruleId = id,
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

// FIXME: Replace this source-text statement scan with SQLDelight-derived select statement facts.
private fun List<SqlToken>.containsGroupBy(): Boolean =
    asSequence()
        .zipWithNext()
        .any { (first, second) ->
            first.text.equals("group", ignoreCase = true) && second.text.equals("by", ignoreCase = true)
        }
