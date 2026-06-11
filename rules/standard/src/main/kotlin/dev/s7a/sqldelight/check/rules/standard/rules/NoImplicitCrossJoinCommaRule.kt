package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
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
            if (!token.isKeyword("from")) return@forEachIndexed

            val fromDepth = content.sqlParenthesisDepthAt(token.startOffset)
            val statementEnd = content.statementEndAfter(token.startOffset)
            val segmentEnd = tokens.fromSegmentEnd(index + 1, statementEnd, fromDepth, content)
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
): Int =
    asSequence()
        .drop(startIndex)
        .firstOrNull { token ->
            token.startOffset < statementEnd &&
                content.sqlParenthesisDepthAt(token.startOffset) == fromDepth &&
                token.normalizedText in fromBoundaryKeywords
        }
        ?.startOffset
        ?: statementEnd

private val fromBoundaryKeywords =
    setOf(
        "except",
        "group",
        "having",
        "intersect",
        "limit",
        "offset",
        "order",
        "union",
        "where",
        "window",
    )
