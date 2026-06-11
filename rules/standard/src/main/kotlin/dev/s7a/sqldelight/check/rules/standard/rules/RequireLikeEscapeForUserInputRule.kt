package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports parameterized `LIKE` predicates that do not declare an `ESCAPE` clause.
 */
public class RequireLikeEscapeForUserInputRule : Rule {
    override val id: RuleId = RuleId("require-like-escape-for-user-input")
    override val defaultSeverity: Severity = Severity.Info
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val tokens = content.sqlTokens().toList()
        tokens.forEachIndexed { index, token ->
            if (!token.isKeyword("like")) return@forEachIndexed

            val statementEnd = content.statementEndAfter(token.startOffset)
            val predicateEnd = tokens.firstBoundaryOffsetAfter(index + 1, statementEnd, likePredicateBoundaryKeywords)
            if (!content.hasNamedParameterBetween(token.endOffset, predicateEnd)) return@forEachIndexed
            if (tokens.any { candidate ->
                    candidate.startOffset in token.endOffset until predicateEnd && candidate.isKeyword("escape")
                }
            ) {
                return@forEachIndexed
            }

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Parameterized LIKE predicates should specify ESCAPE so user input wildcards are explicit.",
                    file = context.file,
                    range = content.rangeAtOffsets(token.startOffset, token.endOffset),
                    database = context.database,
                ),
            )
        }
    }
}

private fun String.hasNamedParameterBetween(
    startOffset: Int,
    endOffset: Int,
): Boolean =
    sqlCharacters()
        .any { character ->
            character.offset in startOffset until endOffset &&
                character.value == ':' &&
                getOrNull(character.offset + 1) != ':' &&
                getOrNull(character.offset + 1).isLikeParameterNameStart()
        }

private fun Char?.isLikeParameterNameStart(): Boolean = this == '_' || this?.isLetter() == true

private val likePredicateBoundaryKeywords =
    setOf("and", "except", "group", "having", "intersect", "limit", "offset", "or", "order", "union", "where")
