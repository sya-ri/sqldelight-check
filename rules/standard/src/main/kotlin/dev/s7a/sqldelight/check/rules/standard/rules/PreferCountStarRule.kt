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
 * Reports row-counting calls that use `COUNT(1)` or `COUNT(0)` instead of `COUNT(*)`.
 */
public class PreferCountStarRule : Rule {
    override val id: RuleId = RuleId("standard:prefer-count-star")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlTokens()
            .filter { token -> token.text.equals("count", ignoreCase = true) }
            .forEach { token ->
                val parenthesisOffset = content.nextNonHorizontalWhitespaceOffset(token.endOffset) ?: return@forEach
                if (content[parenthesisOffset] != '(') return@forEach
                val argument = content.countRowArgument(parenthesisOffset + 1) ?: return@forEach

                val range = content.rangeAtOffsets(argument.startOffset, argument.endOffset)
                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message = "Use COUNT(*) for row counts instead of COUNT(${argument.text}).",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Replace COUNT(${argument.text}) argument with *",
                                    safety = FixSafety.Unsafe,
                                    edits = listOf(TextEdit(range = range, replacement = "*")),
                                ),
                            ),
                    ),
                )
            }
    }
}

private data class CountRowArgument(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.countRowArgument(offset: Int): CountRowArgument? {
    val argumentStart = skipWhitespace(offset)
    if (argumentStart !in indices || this[argumentStart] !in setOf('0', '1')) return null
    val argumentEnd = argumentStart + 1
    val closeOffset = skipWhitespace(argumentEnd)
    if (getOrNull(closeOffset) != ')') return null
    return CountRowArgument(
        text = substring(argumentStart, argumentEnd),
        startOffset = argumentStart,
        endOffset = argumentEnd,
    )
}

private fun String.skipWhitespace(offset: Int): Int {
    var index = offset
    while (index < length && this[index].isWhitespace()) {
        index++
    }
    return index
}
