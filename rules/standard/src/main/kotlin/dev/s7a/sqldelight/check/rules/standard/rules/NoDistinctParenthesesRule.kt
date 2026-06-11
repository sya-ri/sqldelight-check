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
 * Reports `SELECT DISTINCT(...)` syntax.
 */
public class NoDistinctParenthesesRule : Rule {
    override val id: String = "no-distinct-parentheses"
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("select")) return@forEachIndexed
            val distinct = tokens.getOrNull(index + 1)?.takeIf { it.isKeyword("distinct") }
                ?: return@forEachIndexed
            val openOffset = content.nextNonHorizontalWhitespaceOffset(distinct.endOffset) ?: return@forEachIndexed
            if (content[openOffset] != '(') return@forEachIndexed
            val closeOffset = content.matchingParenthesisOffset(openOffset) ?: return@forEachIndexed

            val range = content.rangeAtOffsets(distinct.startOffset, closeOffset + 1)
            reporter.report(
                Diagnostic(
                    ruleId = RuleId(id),
                    severity = defaultSeverity,
                    message = "Do not wrap SELECT DISTINCT arguments in parentheses.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes = content.distinctParenthesesFix(openOffset, closeOffset),
                ),
            )
        }
    }
}

private fun String.distinctParenthesesFix(
    openOffset: Int,
    closeOffset: Int,
): List<Fix> {
    val argument = substring(openOffset + 1, closeOffset)
    if (!argument.isSafeDistinctArgument()) return emptyList()
    val openRange = rangeAtOffsets(openOffset, openOffset + 1)
    val closeRange = rangeAtOffsets(closeOffset, closeOffset + 1)
    val openReplacement = if (getOrNull(openOffset - 1)?.isWhitespace() == true) "" else " "
    return listOf(
        Fix(
            title = "Remove DISTINCT parentheses",
            safety = FixSafety.Safe,
            edits =
                listOf(
                    TextEdit(range = openRange, replacement = openReplacement),
                    TextEdit(range = closeRange, replacement = ""),
                ),
        ),
    )
}

private fun String.isSafeDistinctArgument(): Boolean {
    if (contains('\n') || contains('\r')) return false
    val trimmed = trim()
    return trimmed == "*" || trimmed.matches(simpleDistinctArgumentRegex)
}

private fun String.matchingParenthesisOffset(openOffset: Int): Int? {
    var depth = 0
    sqlCharacters()
        .filter { character -> character.offset >= openOffset }
        .forEach { character ->
            when (character.value) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return character.offset
                }
            }
        }
    return null
}

private val simpleDistinctArgumentRegex =
    Regex("[A-Za-z_][A-Za-z0-9_]*(\\s*\\.\\s*[A-Za-z_][A-Za-z0-9_]*)*")
