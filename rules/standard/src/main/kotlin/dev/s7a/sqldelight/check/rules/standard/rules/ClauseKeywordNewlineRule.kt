package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlSourceTokenContext
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
        val structure = context.sourceStructure
        structure.tokens.forEach { token ->
            if (!token.isTerm(SqlDialectSourceTerm.Select)) return@forEach
            if (token.parenthesisDepth != 0) return@forEach

            val statementTokens = structure.tokensInStatement(token.statementIndex)
            val statementEnd = statementTokens.lastOrNull()?.token?.endOffset ?: return@forEach
            if (!content.hasNewlineBetween(token.token.startOffset, statementEnd)) return@forEach

            statementTokens
                .asSequence()
                .dropWhile { candidate -> candidate.index <= token.index }
                .filter { candidate -> candidate.parenthesisDepth == 0 }
                .majorClauseKeywords(structure.tokens)
                .forEach { clause ->
                    if (content.isFirstNonWhitespaceAt(clause.startOffset)) return@forEach

                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "${clause.name} should start its own line.",
                            file = context.file,
                            range = content.rangeAtOffsets(clause.startOffset, clause.endOffset),
                            database = context.database,
                            fixes = listOf(content.startOwnLineFix(clause.startOffset, "Move clause keyword to its own line")),
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

private fun Sequence<SqlSourceTokenContext>.majorClauseKeywords(allTokens: List<SqlSourceTokenContext>): Sequence<ClauseKeyword> =
    mapNotNull { token ->
        val length = token.matchLength(SqlDialectSourcePatternRole.MajorClauseStart) ?: return@mapNotNull null
        val clauseTokens = allTokens.subList(token.index, (token.index + length).coerceAtMost(allTokens.size))
        ClauseKeyword(
            name = clauseTokens.joinToString(separator = " ") { clausePart -> clausePart.token.text.uppercase() },
            startOffset = token.token.startOffset,
            endOffset = clauseTokens.last().token.endOffset,
        )
    }

private fun SqlSourceTokenContext.isTerm(term: SqlDialectSourceTerm): Boolean =
    token.normalizedText == term.normalizedText
