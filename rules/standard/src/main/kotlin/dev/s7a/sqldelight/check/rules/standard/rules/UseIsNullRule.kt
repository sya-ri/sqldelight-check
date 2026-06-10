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
 * Reports equality comparisons to `NULL` that should use `IS NULL` or `IS NOT NULL`.
 */
public class UseIsNullRule : Rule {
    override val id: RuleId = RuleId("standard:use-is-null")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        content
            .sqlCharacters()
            .mapNotNull { character -> content.comparisonOperatorAt(character.offset) }
            .distinctBy { operator -> operator.startOffset }
            .forEach { operator ->
                val operatorText = content.substring(operator.startOffset, operator.endOffset)
                if (operatorText !in setOf("=", "!=", "<>")) return@forEach
                if (tokens.lastClauseKeywordBefore(operator.startOffset) == "set") return@forEach

                val rightStart = content.horizontalWhitespaceEndAfter(operator.endOffset)
                val rightToken = content.identifierTokenAt(rightStart) ?: return@forEach
                if (!rightToken.text.equals("null", ignoreCase = true)) return@forEach

                val replacement = replacementFor(operatorText, isUppercase = rightToken.text.first().isUpperCase())
                val range = content.rangeAtOffsets(operator.startOffset, operator.endOffset)
                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message = "Use ${replacement} NULL instead of $operatorText NULL.",
                        file = context.file,
                        range = range,
                        database = context.database,
                        fixes =
                            listOf(
                                Fix(
                                    title = "Replace NULL comparison operator",
                                    safety = FixSafety.Unsafe,
                                    edits = listOf(TextEdit(range = range, replacement = replacement)),
                                ),
                            ),
                    ),
                )
            }
    }

    private fun replacementFor(
        operatorText: String,
        isUppercase: Boolean,
    ): String =
        when {
            operatorText == "=" && isUppercase -> "IS"
            operatorText == "=" -> "is"
            isUppercase -> "IS NOT"
            else -> "is not"
        }
}

// FIXME: Replace this text-based clause detection with SQLDelight-derived expression facts once rule-api exposes them.
private fun List<SqlToken>.lastClauseKeywordBefore(offset: Int): String? =
    asSequence()
        .takeWhile { token -> token.startOffset < offset }
        .map { token -> token.text.lowercase() }
        .filter { token -> token in nullComparisonClauseKeywords }
        .lastOrNull()

private val nullComparisonClauseKeywords =
    setOf(
        "from",
        "having",
        "join",
        "on",
        "set",
        "then",
        "when",
        "where",
    )
