package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports major clause keywords that do not start their own line.
 */
public class ClauseKeywordNewlineRule : Rule {
    override val id: RuleId = RuleId("clause-keyword-newline")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("select")) return@forEachIndexed
            val selectDepth = content.sqlParenthesisDepthAt(token.startOffset)
            if (selectDepth != 0) return@forEachIndexed

            val statementEnd = content.statementEndAfter(token.startOffset)
            if (!content.substring(token.startOffset, statementEnd).contains('\n')) return@forEachIndexed

            tokens
                .drop(index + 1)
                .takeWhile { candidate -> candidate.startOffset < statementEnd }
                .filter { candidate -> content.sqlParenthesisDepthAt(candidate.startOffset) == selectDepth }
                .majorClauseKeywords()
                .forEach { clause ->
                    val line = lines.lineContaining(clause.startOffset) ?: return@forEach
                    if (line.firstNonWhitespaceOffset == clause.startOffset) return@forEach

                    reporter.report(
                        Diagnostic(
                            ruleId = id,
                            severity = defaultSeverity,
                            message = "${clause.name} should start its own line.",
                            file = context.file,
                            range = content.rangeAtOffsets(clause.startOffset, clause.endOffset),
                            database = context.database,
                        ),
                    )
                }
        }
    }
}

private data class ClauseKeyword(
    val name: String,
    val startOffset: Int,
    val endOffset: Int,
)

private fun List<SqlToken>.majorClauseKeywords(): List<ClauseKeyword> =
    buildList {
        this@majorClauseKeywords.forEachIndexed { index, token ->
            when {
                token.normalizedText in singleClauseKeywords ->
                    add(
                        ClauseKeyword(
                            name = token.text.uppercase(),
                            startOffset = token.startOffset,
                            endOffset = token.endOffset,
                        ),
                    )
                token.isKeyword("group") && this@majorClauseKeywords.getOrNull(index + 1)?.isKeyword("by") == true ->
                    add(
                        ClauseKeyword(
                            name = "GROUP BY",
                            startOffset = token.startOffset,
                            endOffset = this@majorClauseKeywords[index + 1].endOffset,
                        ),
                    )
                token.isKeyword("order") && this@majorClauseKeywords.getOrNull(index + 1)?.isKeyword("by") == true ->
                    add(
                        ClauseKeyword(
                            name = "ORDER BY",
                            startOffset = token.startOffset,
                            endOffset = this@majorClauseKeywords[index + 1].endOffset,
                        ),
                    )
            }
        }
    }

private val singleClauseKeywords = setOf("from", "where", "having", "limit", "offset")
