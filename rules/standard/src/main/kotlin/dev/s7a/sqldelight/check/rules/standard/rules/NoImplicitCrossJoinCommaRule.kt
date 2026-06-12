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
 * Reports comma-separated FROM sources that should be written as explicit joins.
 */
public class NoImplicitCrossJoinCommaRule : Rule {
    override val id: RuleId = RuleId("no-implicit-cross-join-comma")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isTerm(SqlDialectSourceTerm.From)) return@forEachIndexed

            val fromDepth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val segmentEnd =
                tokens.fromSegmentEnd(index + 1, statementEnd, fromDepth, content, context.database.dialect.sourcePatterns)
            content
                .sqlCharacters()
                .dropWhile { character -> character.offset <= token.endOffset }
                .takeWhile { character -> character.offset < segmentEnd }
                .filter { character -> character.value == ',' && content.sqlParenthesisDepthAt(character.offset) == fromDepth }
                .forEach { comma ->
                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "Use explicit JOIN syntax instead of comma-separated FROM sources.",
                            file = context.file,
                            range = content.rangeAtOffsets(comma.offset, comma.offset + 1),
                            database = context.database,
                        ),
                    )
                }
        }
    }
}

private fun List<SqlToken>.fromSegmentEnd(
    startIndex: Int,
    statementEnd: Int,
    fromDepth: Int,
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
): Int =
    asSequence()
        .drop(startIndex)
        .withIndex()
        .firstOrNull { (relativeIndex, token) ->
            token.startOffset < statementEnd &&
                content.sqlParenthesisDepthAt(token.startOffset) == fromDepth &&
                sourcePatterns.matches(SqlDialectSourcePatternRole.TableReferenceBoundary, normalizedTextsFrom(startIndex + relativeIndex))
        }
        ?.value
        ?.startOffset
        ?: statementEnd
