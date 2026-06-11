package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports `IFNULL` and `NVL` calls that can be written as `COALESCE`.
 */
public class PreferCoalesceRule : Rule {
    override val id: String = "prefer-coalesce"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlTokens()
            .filter { token -> token.text.lowercase() in coalesceAlternatives }
            .forEach { token ->
                val parenthesisOffset = content.nextNonHorizontalWhitespaceOffset(token.endOffset) ?: return@forEach
                if (content[parenthesisOffset] != '(') return@forEach

                val range = content.rangeAtOffsets(token.startOffset, token.endOffset)
                reporter.report(
                    Diagnostic(
                        ruleId = RuleId(id),
                        severity = defaultSeverity,
                        message = "Use COALESCE instead of ${token.text.uppercase()}.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Replace ${token.text.uppercase()} with COALESCE",
                                    safety = FixSafety.Unsafe,
                                    edits = listOf(TextEdit(range = range, replacement = "COALESCE")),
                                ),
                            ),
                    ),
                )
            }
    }
}

private val coalesceAlternatives = setOf("ifnull", "nvl")
