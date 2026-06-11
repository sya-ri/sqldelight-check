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
 * Reports COUNT comparisons that are only checking for existence.
 */
public class PreferExistsOverCountForExistenceRule : Rule {
    override val id: RuleId = RuleId("prefer-exists-over-count-for-existence")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content
            .sqlTokens()
            .filter { token -> token.text.equals("count", ignoreCase = true) }
            .forEach { token ->
                if (!content.isCountStarCallFollowedByGreaterThanZero(token.endOffset)) return@forEach

                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Use EXISTS instead of COUNT(*) > 0 when only existence is needed.",
                        file = context.file,
                        range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun String.isCountStarCallFollowedByGreaterThanZero(offset: Int): Boolean {
    val open = skipSqlWhitespace(offset)
    if (getOrNull(open) != '(') return false
    val argument = skipSqlWhitespace(open + 1)
    if (getOrNull(argument) != '*') return false
    val close = skipSqlWhitespace(argument + 1)
    if (getOrNull(close) != ')') return false
    val comparison = skipSqlWhitespace(close + 1)
    if (getOrNull(comparison) != '>') return false
    val zero = skipSqlWhitespace(comparison + 1)
    if (getOrNull(zero) != '0') return false
    val afterZero = skipSqlWhitespace(zero + 1)
    return getOrNull(afterZero)?.isDigit() != true
}

private fun String.skipSqlWhitespace(offset: Int): Int {
    var index = offset
    while (index < length && this[index].isWhitespace()) {
        index++
    }
    return index
}
