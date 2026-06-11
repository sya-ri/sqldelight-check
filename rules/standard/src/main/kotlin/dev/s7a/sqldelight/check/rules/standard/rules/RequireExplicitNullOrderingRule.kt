package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports ORDER BY items with explicit direction but no explicit NULL ordering.
 */
public class RequireExplicitNullOrderingRule : Rule {
    override val id: RuleId = RuleId("require-explicit-null-ordering")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("order")) return@forEachIndexed
            if (tokens.getOrNull(index + 1)?.isKeyword("by") != true) return@forEachIndexed
            val statementEnd = content.statementEndAfter(token.startOffset)
            val boundary = tokens.firstBoundaryOffsetAfter(index + 2, statementEnd, orderByBoundaryKeywords)
            tokens
                .drop(index + 2)
                .takeWhile { candidate -> candidate.startOffset < boundary }
                .filter { candidate -> candidate.isKeyword("asc") || candidate.isKeyword("desc") }
                .filterNot { direction -> tokens.hasNullOrderingAfter(direction, boundary, content) }
                .forEach { direction ->
                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "Specify NULLS FIRST or NULLS LAST with explicit ORDER BY direction.",
                            file = context.file,
                            range = content.rangeAtOffsets(direction.startOffset, direction.endOffset),
                            database = context.database,
                        ),
                    )
                }
        }
    }
}

private val orderByBoundaryKeywords = setOf("except", "intersect", "limit", "offset", "union", "window")

private fun List<SqlToken>.hasNullOrderingAfter(
    direction: SqlToken,
    boundary: Int,
    content: String,
): Boolean {
    val directionDepth = content.sqlParenthesisDepthAt(direction.startOffset)
    val itemEnd = content.orderByItemEndAfter(direction.endOffset, boundary, directionDepth)
    return asSequence()
        .dropWhile { token -> token.startOffset <= direction.startOffset }
        .takeWhile { token ->
            token.startOffset < itemEnd &&
                token.startOffset < boundary
        }
        .filter { token -> content.sqlParenthesisDepthAt(token.startOffset) == directionDepth }
        .any { token -> token.isKeyword("nulls") }
}

private fun String.orderByItemEndAfter(
    offset: Int,
    boundary: Int,
    depth: Int,
): Int {
    var index = offset
    while (index < length && index < boundary) {
        if (this[index] == ',' && sqlParenthesisDepthAt(index) == depth) return index
        index++
    }
    return boundary
}
