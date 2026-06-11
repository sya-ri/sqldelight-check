package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports redundant `ELSE NULL` branches in `CASE` expressions.
 */
public class NoElseNullRule : Rule {
    override val id: RuleId = RuleId("no-else-null")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        var caseDepth = 0
        tokens.forEachIndexed { index, token ->
            when {
                token.isKeyword("case") -> caseDepth++
                token.isKeyword("end") && caseDepth > 0 -> caseDepth--
                token.isKeyword("else") && caseDepth > 0 -> {
                    val next = tokens.getOrNull(index + 1)
                    if (next?.isKeyword("null") != true) return@forEachIndexed

                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "Omit ELSE NULL from CASE expressions.",
                            file = context.file,
                            range = content.rangeAtOffsets(token.startOffset, next.endOffset),
                            database = context.database,
                        ),
                    )
                }
            }
        }
    }
}
