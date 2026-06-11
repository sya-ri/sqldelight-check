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
 * Reports computed SELECT targets that do not have a result column alias.
 */
public class RequireResultColumnAliasRule : Rule {
    override val id: RuleId = RuleId("require-result-column-alias")
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
            val selectDepth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val fromToken =
                tokens
                    .drop(index + 1)
                    .firstOrNull { candidate ->
                        candidate.startOffset < statementEnd &&
                            content.sqlParenthesisDepthAt(candidate.startOffset) == selectDepth &&
                            candidate.isKeyword("from")
                    } ?: return@forEachIndexed

            content.selectTargets(token.endOffset, fromToken.startOffset, selectDepth).forEach { target ->
                if (!target.requiresAlias(content)) return@forEach
                if (target.hasAlias(content)) return@forEach

                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Computed SELECT result columns should have an alias.",
                        file = context.file,
                        range = content.rangeAtOffsets(target.startOffset, target.endOffset),
                        database = context.database,
                    ),
                )
            }
        }
    }
}

private data class SelectTarget(
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.selectTargets(
    startOffset: Int,
    endOffset: Int,
    depth: Int,
): List<SelectTarget> {
    val targets = mutableListOf<SelectTarget>()
    var targetStart = startOffset
    sqlCharacters()
        .dropWhile { character -> character.offset < startOffset }
        .takeWhile { character -> character.offset < endOffset }
        .forEach { character ->
            if (character.value == ',' && sqlParenthesisDepthAt(character.offset) == depth) {
                targets += SelectTarget(targetStart, character.offset).trimmedIn(this)
                targetStart = character.offset + 1
            }
        }
    targets += SelectTarget(targetStart, endOffset).trimmedIn(this)
    return targets.filter { target -> target.startOffset < target.endOffset }
}

private fun SelectTarget.trimmedIn(content: String): SelectTarget {
    var start = startOffset
    var end = endOffset
    while (start < end && content[start].isWhitespace()) start++
    while (end > start && content[end - 1].isWhitespace()) end--
    return SelectTarget(start, end)
}

private fun SelectTarget.requiresAlias(content: String): Boolean {
    val text = content.substring(startOffset, endOffset)
    if (text == "*") return false
    val tokens = text.sqlTokens().toList()
    if (tokens.size == 1 && text.isSimpleColumnReference()) return false
    return text.any { character -> character in "()+-*/|<>= " } || tokens.any { token -> token.isKeyword("case") }
}

private fun SelectTarget.hasAlias(content: String): Boolean {
    val text = content.substring(startOffset, endOffset)
    val tokens = text.sqlTokens().toList()
    if (tokens.any { token -> token.isKeyword("as") }) return true
    if (tokens.size < 2) return false
    val last = tokens.last()
    val previous = tokens[tokens.lastIndex - 1]
    return last.normalizedText !in aliasBoundaryKeywords && previous.endOffset < last.startOffset
}

private fun String.isSimpleColumnReference(): Boolean =
    all { character -> character.isLetterOrDigit() || character == '_' || character == '.' || character == '"' || character == '`' }

private val aliasBoundaryKeywords =
    setOf("case", "cast", "coalesce", "count", "else", "end", "false", "from", "null", "then", "true", "when")
