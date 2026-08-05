package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports multiline CREATE TABLE constraints that do not start their own line.
 */
public class ConstraintNewlineRule : Rule {
    override val id: RuleId = RuleId("constraint-newline")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val lines = content.linesWithRanges()
        val tokens = content.sqlTokens().toList()
        val parenthesisDepths = content.computeParenthesisDepths()
        val mappedTypes = content.mappedTypeNames(context.database.dialect.sourcePatterns).toList()
        content.createTableBodies(tokens, parenthesisDepths).forEach bodyLoop@{ body ->
            val items = content.commaSeparatedClauseItems(body.openOffset + 1, body.closeOffset, body.itemDepth, parenthesisDepths)
            if (!content.isMultilineItemList(items)) return@bodyLoop

            items.forEach { item ->
                val itemLine = lines.lineContaining(item.startOffset) ?: return@forEach
                val indentation = itemLine.text.takeWhile { character -> character == ' ' || character == '\t' }
                content.createTableConstraintTokens(item, body.itemDepth, context.database.dialect.sourcePatterns, mappedTypes, parenthesisDepths).forEach { constraint ->
                    val line = lines.lineContaining(constraint.startOffset) ?: return@forEach
                    if (line.firstNonWhitespaceOffset == constraint.startOffset) return@forEach
                    reporter.report(
                        RuleDiagnostic(
                            severity = defaultSeverity,
                            message = "Multiline CREATE TABLE constraints should start their own line.",
                            file = context.file,
                            range = content.rangeAtOffsets(constraint.startOffset, constraint.endOffset),
                            database = context.database,
                            fixes =
                                listOf(
                                    content.startOwnLineFix(
                                        constraint.startOffset,
                                        "Move constraint to its own line",
                                        indentation,
                                    ),
                                ),
                        ),
                    )
                }
            }
        }
    }
}

private fun String.createTableConstraintTokens(
    item: ClauseItem,
    itemDepth: Int,
    sourcePatterns: SqlDialectSourcePatterns,
    mappedTypes: List<MappedTypeName>,
    parenthesisDepths: IntArray,
): List<SqlToken> {
    val itemTokens =
        sqlTokens()
            .dropWhile { token -> token.startOffset < item.startOffset }
            .takeWhile { token -> token.startOffset < item.endOffset }
            .filter { token -> parenthesisDepths[token.startOffset] == itemDepth }
            .toList()
    val first = itemTokens.firstOrNull() ?: return emptyList()
    if (sourcePatterns.matches(SqlDialectSourcePatternRole.TableConstraintStart, itemTokens.normalizedTextsFrom(0))) {
        return listOf(first)
    }
    if (!hasNewlineBetween(item.startOffset, item.endOffset)) return emptyList()
    val firstLineEnd =
        indexOf('\n', item.startOffset).takeIf { offset -> offset in item.startOffset until item.endOffset }
            ?: return emptyList()
    val content = this
    return buildList {
        var index = 1
        while (index < itemTokens.size) {
            val token = itemTokens[index]
            val length =
                sourcePatterns.matchPrefix(SqlDialectSourcePatternRole.ColumnConstraintStart, itemTokens.normalizedTextsFrom(index))
            if (
                length != null &&
                token.startOffset > firstLineEnd &&
                parenthesisDepths[token.startOffset] == itemDepth &&
                !mappedTypes.hasTypeOnSameLine(content, token.startOffset)
            ) {
                add(token)
                index += length
            } else {
                index++
            }
        }
    }
}
