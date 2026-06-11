package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.booleanOption
import dev.s7a.sqldelight.check.rule.api.commaSeparatedOption
import dev.s7a.sqldelight.check.rule.api.positiveIntOption

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

private const val DEFAULT_MAX_JOINS = 8

/**
 * Reports statements with too many JOIN clauses.
 */
public class MaxJoinsRule : Rule {
    override val id: String = "max-joins"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val max = context.options.positiveIntOption("max", DEFAULT_MAX_JOINS)
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("select")) return@forEachIndexed
            val selectDepth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val joins =
                tokens
                    .drop(index + 1)
                    .takeWhile { candidate -> candidate.startOffset < statementEnd }
                    .filter { candidate -> candidate.isKeyword("join") && content.sqlParenthesisDepthAt(candidate.startOffset) == selectDepth }
            if (joins.size <= max) return@forEachIndexed

            val firstExcessJoin = joins[max]
            reporter.report(
                Diagnostic(
                    ruleId = RuleId(id),
                    severity = defaultSeverity,
                    message = "Statement has more than $max JOIN clauses.",
                    file = context.file,
                    range = content.rangeAtOffsets(firstExcessJoin.startOffset, firstExcessJoin.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}
