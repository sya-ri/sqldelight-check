package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
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
            if (!token.isTerm(SqlDialectSourceTerm.Select)) return@forEachIndexed
            if (content.sqlParenthesisDepthAt(token.startOffset) != 0) return@forEachIndexed

            val statementEnd = content.statementEndAfter(token.startOffset)
            if (!content.substring(token.startOffset, statementEnd).contains('\n')) return@forEachIndexed

            tokens
                .drop(index + 1)
                .takeWhile { candidate -> candidate.startOffset < statementEnd }
                .filter { candidate -> content.sqlParenthesisDepthAt(candidate.startOffset) == 0 }
                .majorClauseKeywords(context.database.dialect.sourcePatterns)
                .forEach { clause ->
                    val line = lines.lineContaining(clause.startOffset) ?: return@forEach
                    if (line.firstNonWhitespaceOffset == clause.startOffset) return@forEach

                    reporter.report(
                        RuleDiagnostic(
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

private fun List<SqlToken>.majorClauseKeywords(sourcePatterns: SqlDialectSourcePatterns): List<ClauseKeyword> =
    buildList {
        this@majorClauseKeywords.forEachIndexed { index, token ->
            val length =
                sourcePatterns.matchPrefix(SqlDialectSourcePatternRole.MajorClauseStart, normalizedTextsFrom(index))
                    ?: return@forEachIndexed
            val endToken = this@majorClauseKeywords.getOrNull(index + length - 1) ?: return@forEachIndexed
            add(
                ClauseKeyword(
                    name =
                        this@majorClauseKeywords
                            .subList(index, index + length)
                            .joinToString(separator = " ") { clausePart -> clausePart.text.uppercase() },
                    startOffset = token.startOffset,
                    endOffset = endToken.endOffset,
                ),
            )
        }
    }
