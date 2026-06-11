package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports set operators that are not placed at the start of their own line.
 */
public class SetOperatorLinePositionRule : Rule {
    override val id: String = "set-operator-line-position"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        if (!content.contains('\n')) return

        val lines = content.linesWithRanges()
        content
            .sqlTokens()
            .filter { token -> token.normalizedText in setOperators }
            .forEach { token ->
                val line = lines.lastOrNull { candidate -> candidate.startOffset <= token.startOffset } ?: return@forEach
                val prefix = content.substring(line.startOffset, token.startOffset)
                if (prefix.all { character -> character == ' ' || character == '\t' }) return@forEach

                reporter.report(
                    Diagnostic(
                        ruleId = RuleId(id),
                        severity = defaultSeverity,
                        message = "${token.text.uppercase()} should be at the start of its own line.",
                        file = context.file,
                        range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private val setOperators = setOf("except", "intersect", "union")
