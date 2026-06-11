package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports redundant `ELSE NULL` branches in `CASE` expressions.
 */
public class NoElseNullRule : Rule {
    override val id: RuleId = RuleId("standard:no-else-null")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

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
                        Diagnostic(
                            ruleId = id,
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
