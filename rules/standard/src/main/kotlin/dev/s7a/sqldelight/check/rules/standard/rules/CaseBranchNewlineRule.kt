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
 * Reports multiline CASE expressions whose branch keywords do not start their own line.
 */
public class CaseBranchNewlineRule : Rule {
    override val id: RuleId = RuleId("case-branch-newline")
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
            if (!token.isKeyword("case")) return@forEachIndexed
            val depth = content.sqlParenthesisDepthAt(token.startOffset)
            val caseEnd = tokens.caseEndOffset(index + 1, depth, content) ?: return@forEachIndexed
            if (!content.substring(token.startOffset, caseEnd).contains('\n')) return@forEachIndexed

            tokens
                .drop(index + 1)
                .takeWhile { candidate -> candidate.startOffset < caseEnd }
                .filter { candidate -> content.sqlParenthesisDepthAt(candidate.startOffset) == depth }
                .asSequence()
                .branchTokensForOuterCase()
                .forEach { branch ->
                    val line = lines.lineContaining(branch.startOffset) ?: return@forEach
                    if (line.firstNonWhitespaceOffset == branch.startOffset) return@forEach
                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "Multiline CASE branch keywords should start their own line.",
                            file = context.file,
                            range = content.rangeAtOffsets(branch.startOffset, branch.endOffset),
                            database = context.database,
                        ),
                    )
                }
        }
    }
}

private fun List<SqlToken>.caseEndOffset(
    startIndex: Int,
    depth: Int,
    content: String,
): Int? {
    var nestedCases = 0
    asSequence()
        .drop(startIndex)
        .filter { token -> content.sqlParenthesisDepthAt(token.startOffset) == depth }
        .forEach { token ->
            when {
                token.isKeyword("case") -> nestedCases++
                token.isKeyword("end") && nestedCases > 0 -> nestedCases--
                token.isKeyword("end") -> return token.endOffset
            }
        }
    return null
}

private fun Sequence<SqlToken>.branchTokensForOuterCase(): Sequence<SqlToken> =
    sequence {
        var nestedCases = 0
        forEach { token ->
            when {
                token.isKeyword("case") -> nestedCases++
                token.isKeyword("end") && nestedCases > 0 -> nestedCases--
                nestedCases == 0 && token.normalizedText in caseBranchKeywords -> yield(token)
            }
        }
    }

private val caseBranchKeywords = setOf("when", "then", "else")
