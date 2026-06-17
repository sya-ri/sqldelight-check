package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourceTerm
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports SELECT result aliases that repeat the source column name.
 *
 * Self aliases do not add a stable API name in SQLDelight generated code.
 * The rule only checks simple column references and leaves computed
 * expressions to explicit aliasing rules.
 */
public class NoSelfColumnAliasRule : Rule {
    override val id: RuleId = RuleId("no-self-column-alias")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        content.resultColumnAliases(context.database.dialect.sourcePatterns).forEach { alias ->
            if (!alias.repeatsSourceColumnNameIn(content)) return@forEach

            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "Column aliases should not repeat the source column name.",
                    file = context.file,
                    range = content.rangeAtOffsets(alias.token.startOffset, alias.token.endOffset),
                    database = context.database,
                    fixes = listOf(content.deleteAliasFix(alias.token.startOffset, alias.token.endOffset, "Remove self column alias")),
                ),
            )
        }
    }
}

private fun ResultColumnAlias.repeatsSourceColumnNameIn(content: String): Boolean {
    val sourceExpressionEndOffset = sourceExpressionEndOffsetIn(content) ?: return false
    val sourceTokens = content.substring(targetStartOffset, sourceExpressionEndOffset).sqlTokens().toList()
    if (sourceTokens.isEmpty()) return false
    if (!content.isSimpleColumnReference(targetStartOffset, sourceExpressionEndOffset)) return false

    val sourceName = sourceTokens.last().text.normalizedIdentifier()
    val aliasName = token.text.normalizedIdentifier()
    return sourceName.equals(aliasName, ignoreCase = true)
}

private fun ResultColumnAlias.sourceExpressionEndOffsetIn(content: String): Int? {
    val targetTokens = content.substring(targetStartOffset, targetEndOffset).sqlTokens().toList()
    val aliasIndex =
        targetTokens.indexOfLast { candidate ->
            targetStartOffset + candidate.startOffset == token.startOffset &&
                targetStartOffset + candidate.endOffset == token.endOffset
        }
    if (aliasIndex <= 0) return null

    val previous = targetTokens[aliasIndex - 1]
    return if (previous.isTerm(SqlDialectSourceTerm.As)) {
        targetStartOffset + previous.startOffset
    } else {
        token.startOffset
    }
}

private fun String.isSimpleColumnReference(
    startOffset: Int,
    endOffset: Int,
): Boolean =
    substring(startOffset, endOffset)
        .all { character ->
            character.isLetterOrDigit() ||
                character == '_' ||
                character == '.' ||
                character == '"' ||
                character == '`' ||
                character == '[' ||
                character == ']' ||
                character.isWhitespace()
        }

private fun String.normalizedIdentifier(): String =
    removeSurrounding("\"")
        .removeSurrounding("`")
        .removeSurrounding("[", "]")
