package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.AliasBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports computed SELECT targets that do not have a result column alias.
 */
public class RequireResultColumnAliasRule : Rule {
    override val id: RuleId = RuleId("require-result-column-alias")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.sourceSelectClauseTargets().forEach { clause ->
            clause.targets.forEach { target ->
                if (!target.requiresAlias(content)) return@forEach
                if (target.hasAlias(content, context.database.dialect.sourcePatterns)) return@forEach

                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Computed SELECT result columns should have an alias.",
                        file = context.file,
                        range = content.rangeAtOffsets(target.startOffset, target.endOffset),
                        database = context.database,
                    ),
                )
            }
        }
    }
}

private fun SourceSelectTarget.requiresAlias(content: String): Boolean {
    val text = content.substring(startOffset, endOffset)
    if (text == "*") return false
    val tokens = text.sqlTokens().toList()
    if (tokens.size == 1 && text.isSimpleColumnReference()) return false
    return text.any { character -> character in "()+-*/|<>= " } || tokens.any { token -> token.isTerm(SqlDialectSourceTerm.Case) }
}

private fun SourceSelectTarget.hasAlias(
    content: String,
    sourcePatterns: SqlDialectSourcePatterns,
): Boolean {
    val text = content.substring(startOffset, endOffset)
    val tokens = text.sqlTokens().toList()
    if (tokens.any { token -> token.isTerm(SqlDialectSourceTerm.As) }) return true
    if (tokens.size < 2) return false
    val last = tokens.last()
    val aliasOffset = startOffset + last.startOffset
    val previousSqlCharacter = content.sqlCharacters().takeWhile { character -> character.offset < aliasOffset }.lastOrNull()
    return last.text.isIdentifierLike() &&
        content.sqlParenthesisDepthAt(aliasOffset) == content.sqlParenthesisDepthAt(startOffset) &&
        previousSqlCharacter?.value?.isWhitespace() == true &&
        previousSqlCharacter.previousNonWhitespaceIn(content)?.value !in setOf('+', '-', '*', '/', '|', '<', '>', '=') &&
        !sourcePatterns.matches(AliasBoundary, listOf(last.normalizedText))
}

private fun String.isSimpleColumnReference(): Boolean =
    all { character -> character.isLetterOrDigit() || character == '_' || character == '.' || character == '"' || character == '`' }

private fun String.isIdentifierLike(): Boolean =
    firstOrNull()?.let { character -> character == '_' || character.isLetter() } == true

private fun SqlCharacter.previousNonWhitespaceIn(content: String): SqlCharacter? =
    content.sqlCharacters()
        .takeWhile { character -> character.offset < offset }
        .lastOrNull { character -> !character.value.isWhitespace() }
