package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports common table expressions that are not referenced by the main query.
 *
 * The rule handles explicit `WITH name AS (...)` clauses and leaves recursive or
 * ambiguous CTE layouts alone until SQLDelight-derived facts are richer.
 */
public class NoUnusedCteRule : Rule {
    override val id: RuleId = RuleId("no-unused-cte")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.cteBlocks().forEach { block ->
            block.ctes.forEach { cte ->
                if (content.hasTokenAfter(cte.name, block.mainQueryStartOffset)) return@forEach
                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message = "CTE '${cte.name}' is not referenced by the main query.",
                        file = context.file,
                        range = content.rangeAtOffsets(cte.startOffset, cte.endOffset),
                        database = context.database,
                    ),
                )
            }
        }
    }
}

private data class CteBlock(
    val ctes: List<CteDefinition>,
    val mainQueryStartOffset: Int,
)

private data class CteDefinition(
    val name: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun String.cteBlocks(): List<CteBlock> {
    val tokens = sqlTokens().toList()
    return tokens
        .filter { token -> token.isKeyword("with") && sqlParenthesisDepthAt(token.startOffset) == 0 }
        .mapNotNull { withToken -> cteBlockAfter(withToken, tokens) }
}

private fun String.cteBlockAfter(
    withToken: SqlToken,
    tokens: List<SqlToken>,
): CteBlock? {
    val ctes = mutableListOf<CteDefinition>()
    var index = tokens.indexOf(withToken) + 1
    while (index < tokens.size) {
        val name = tokens.getOrNull(index) ?: return null
        val asToken = tokens.getOrNull(index + 1) ?: return null
        if (!asToken.isKeyword("as")) return null
        val open = nextSqlCharacterAfter(asToken.endOffset) ?: return null
        if (open.value != '(') return null
        val close = matchingClosingParenthesisOffset(open.offset) ?: return null
        ctes += CteDefinition(name = name.text, startOffset = name.startOffset, endOffset = close + 1)

        val next = nextSqlCharacterAfter(close + 1) ?: return null
        if (next.value != ',') {
            return CteBlock(ctes = ctes, mainQueryStartOffset = next.offset)
        }
        index = tokens.indexOfFirst { token -> token.startOffset > next.offset }
        if (index == -1) return null
    }
    return null
}

private fun String.hasTokenAfter(
    tokenText: String,
    offset: Int,
): Boolean =
    sqlTokens()
        .dropWhile { token -> token.startOffset < offset }
        .any { token -> token.text.equals(tokenText, ignoreCase = true) }
