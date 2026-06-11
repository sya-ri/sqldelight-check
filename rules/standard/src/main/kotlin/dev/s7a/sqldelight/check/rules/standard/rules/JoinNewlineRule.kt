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
 * Reports top-level JOIN clauses that do not start their own line in multiline statements.
 */
public class JoinNewlineRule : Rule {
    override val id: RuleId = RuleId("join-newline")
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
            if (!token.isTerm(SqlDialectSourceTerm.Join)) return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            if (depth != 0) return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val statementStart =
                tokens.statementStartOffsetBefore(index, content, context.database.dialect.sourcePatterns)
                    ?: return@forEachIndexed
            if (!content.substring(statementStart, statementEnd).contains('\n')) return@forEachIndexed

            val clauseStart = tokens.joinClauseStartOffset(index, depth, content, context.database.dialect.sourcePatterns)
            val line = lines.lineContaining(clauseStart) ?: return@forEachIndexed
            if (line.firstNonWhitespaceOffset == clauseStart) return@forEachIndexed

            reporter.report(
                RuleDiagnostic(
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
    sourcePatterns: SqlDialectSourcePatterns,
): Int {
    val previous = getOrNull(joinIndex - 1)
    return if (
        previous != null &&
        previous.matches(sourcePatterns, SqlDialectSourcePatternRole.JoinModifier) &&
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
    sourcePatterns: SqlDialectSourcePatterns,
): Int? =
    asSequence()
        .take(tokenIndex + 1)
        .filter { token -> token.matches(sourcePatterns, SqlDialectSourcePatternRole.SqlDelightStatementStart) }
        .lastOrNull { token -> content.sqlParenthesisDepthAt(token.startOffset) == 0 }
        ?.startOffset

private fun String.onlyInlineWhitespaceBetween(
    startOffset: Int,
    endOffset: Int,
): Boolean =
    substring(startOffset, endOffset).all { character -> character == ' ' || character == '\t' }
