package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports `RIGHT JOIN` clauses so queries can be written as `LEFT JOIN`.
 */
public class NoRightJoinRule : Rule {
    override val id: RuleId = RuleId("no-right-join")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.text.equals("right", ignoreCase = true)) return@forEachIndexed
            val joinToken = tokens.joinTokenAfterRight(index) ?: return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Use LEFT JOIN instead of ${content.substring(token.startOffset, joinToken.endOffset)}.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, joinToken.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun List<SqlToken>.joinTokenAfterRight(rightIndex: Int): SqlToken? {
    val next = getOrNull(rightIndex + 1) ?: return null
    if (next.text.equals("join", ignoreCase = true)) return next
    if (!next.text.equals("outer", ignoreCase = true)) return null
    val afterOuter = getOrNull(rightIndex + 2) ?: return null
    return if (afterOuter.text.equals("join", ignoreCase = true)) afterOuter else null
}
