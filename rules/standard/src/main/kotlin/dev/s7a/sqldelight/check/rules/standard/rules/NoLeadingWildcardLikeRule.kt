package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports LIKE patterns that start with a wildcard.
 */
public class NoLeadingWildcardLikeRule : Rule {
    override val id: RuleId = RuleId("standard:no-leading-wildcard-like")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.sqlTokens()
            .filter { token -> token.isKeyword("like") }
            .forEach { token ->
                val literalStart = content.nextNonWhitespaceOffset(token.endOffset)
                if (content.getOrNull(literalStart) != '\'') return@forEach
                val firstPatternChar = literalStart + 1
                if (content.getOrNull(firstPatternChar) != '%' && content.getOrNull(firstPatternChar) != '_') return@forEach

                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message = "Avoid LIKE patterns that start with a wildcard.",
                        file = context.file,
                        range = content.rangeAtOffsets(literalStart, firstPatternChar + 1),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun String.nextNonWhitespaceOffset(startOffset: Int): Int {
    var index = startOffset
    while (index < length && this[index].isWhitespace()) {
        index++
    }
    return index
}
