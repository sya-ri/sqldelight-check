package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports top-level JOIN clauses that do not start their own line in multiline statements.
 */
public class JoinNewlineRule : Rule {
    override val id: RuleId = RuleId("standard:join-newline")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnablement: Enablement = Enablement.Auto

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("join")) return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            if (depth != 0) return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val statementStart = tokens.statementStartOffsetBefore(index, content) ?: return@forEachIndexed
            if (!content.substring(statementStart, statementEnd).contains('\n')) return@forEachIndexed

            val clauseStart = tokens.joinClauseStartOffset(index, depth, content)
            val line = lines.lineContaining(clauseStart) ?: return@forEachIndexed
            if (line.firstNonWhitespaceOffset == clauseStart) return@forEachIndexed

            reporter.report(
                Diagnostic(
                    ruleId = id,
                    severity = defaultSeverity,
                    message = "JOIN clauses should start their own line in multiline statements.",
                    file = context.file,
                    range = content.rangeAtOffsets(clauseStart, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun List<SqlToken>.joinClauseStartOffset(
    joinIndex: Int,
    depth: Int,
    content: String,
): Int {
    val previous = getOrNull(joinIndex - 1)
    return if (
        previous != null &&
        previous.normalizedText in joinModifiers &&
        content.sqlParenthesisDepthAt(previous.startOffset) == depth &&
        content.onlyInlineWhitespaceBetween(previous.endOffset, this[joinIndex].startOffset)
    ) {
        previous.startOffset
    } else {
        this[joinIndex].startOffset
    }
}

private fun List<SqlToken>.statementStartOffsetBefore(
    tokenIndex: Int,
    content: String,
): Int? =
    asSequence()
        .take(tokenIndex + 1)
        .filter { token -> token.normalizedText in topLevelStatementStartKeywords }
        .filter { token -> content.sqlParenthesisDepthAt(token.startOffset) == 0 }
        .lastOrNull()
        ?.startOffset

private fun String.onlyInlineWhitespaceBetween(
    startOffset: Int,
    endOffset: Int,
): Boolean =
    substring(startOffset, endOffset).all { character -> character == ' ' || character == '\t' }

private val joinModifiers = setOf("cross", "full", "inner", "join", "left", "right")

private val topLevelStatementStartKeywords = setOf("delete", "insert", "select", "update", "with")
