package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.Fix
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlSourceBlock
import dev.s7a.sqldelight.check.api.SqlSourceBlockKind
import dev.s7a.sqldelight.check.api.SqlSourceStructure
import dev.s7a.sqldelight.check.api.SqlSourceTokenContext
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.positiveIntOption
import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

/**
 * Reports SQL lines whose indentation does not match their source structure.
 *
 * The rule only checks multiline statements. Clause starts align with their
 * source block nesting, while clause contents and expression continuations are
 * indented one level deeper. Subquery bodies are indented inside their
 * parenthesized source block.
 */
public class SourceIndentationRule : Rule {
    override val id: RuleId = RuleId("source-indentation")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val indentSize = context.options.positiveIntOption("indentSize", DEFAULT_INDENT_SIZE)
        val lines = content.linesWithRanges()
        val structure = SqlSourceStructure.parse(content, context.database.dialect.sourcePatterns)
        val statementBlocks =
            structure.blocks
                .filter { block -> block.kind == SqlSourceBlockKind.Statement }
                .associateBy { block -> block.statementIndex }

        lines.forEach { line ->
            val firstContentOffset = line.firstNonWhitespaceOffset ?: return@forEach
            val tokenContext = structure.contextAtOffset(firstContentOffset) ?: return@forEach
            val statementBlock = statementBlocks[tokenContext.statementIndex] ?: return@forEach
            if (!content.substring(statementBlock.startOffset, statementBlock.endOffset).contains('\n')) return@forEach

            val expectedIndentation = " ".repeat(tokenContext.expectedIndentationLevel(structure) * indentSize)
            val actualIndentation = line.text.takeWhile { character -> character == ' ' || character == '\t' }
            if (actualIndentation == expectedIndentation) return@forEach

            val range = content.rangeAtOffsets(line.startOffset, line.startOffset + actualIndentation.length)
            reporter.report(
                RuleDiagnostic(
                    severity = defaultSeverity,
                    message = "SQL source indentation should match its layout.",
                    file = context.file,
                    range = range,
                    database = context.database,
                    fixes =
                        listOf(
                            Fix(
                                title = "Align SQL source indentation",
                                safety = FixSafety.Safe,
                                edits = listOf(TextEdit(range = range, replacement = expectedIndentation)),
                            ),
                        ),
                ),
            )
        }
    }
}

private const val DEFAULT_INDENT_SIZE = 4

private fun SqlSourceTokenContext.expectedIndentationLevel(
    structure: SqlSourceStructure,
): Int {
    val blockIndentation = sourceBlockDepth(structure)
    val subqueryBodyIndentation = subqueryBodyDepth(structure)
    val clauseIndentation = if (isClauseContinuation(structure)) 1 else 0
    val continuationIndentation = if (isExpressionContinuation()) 1 else 0
    val caseIndentation = if (isCaseContinuation(structure)) 1 else 0
    return blockIndentation + subqueryBodyIndentation + maxOf(clauseIndentation, continuationIndentation) + caseIndentation
}

private fun SqlSourceTokenContext.sourceBlockDepth(structure: SqlSourceStructure): Int =
    structure.blocks.count { block ->
        block.isIndentingBlock &&
            block.contains(this) &&
            index > block.startTokenIndex &&
            !isClosingTokenOf(block)
    }

private fun SqlSourceTokenContext.subqueryBodyDepth(structure: SqlSourceStructure): Int =
    structure.blocks.count { block ->
        block.kind == SqlSourceBlockKind.Subquery &&
            block.contains(this) &&
            index > block.startTokenIndex &&
            !isClosingTokenOf(block)
    }

private val SqlSourceBlock.isIndentingBlock: Boolean
    get() =
        kind == SqlSourceBlockKind.ParenthesizedExpression ||
            kind == SqlSourceBlockKind.Subquery

private fun SqlSourceTokenContext.isClosingTokenOf(block: SqlSourceBlock): Boolean =
    index == block.endTokenIndex - 1 && token.normalizedText == ")"

private fun SqlSourceTokenContext.isClauseContinuation(structure: SqlSourceStructure): Boolean {
    if (isClauseStart()) return false
    val clause = structure.innermostClauseContaining(this) ?: return false
    return index > clause.startTokenIndex
}

private fun SqlSourceStructure.innermostClauseContaining(context: SqlSourceTokenContext): SqlSourceBlock? {
    var result: SqlSourceBlock? = null
    blocks.forEach { block ->
        val current = result
        if (
            block.kind == SqlSourceBlockKind.Clause &&
            block.contains(context) &&
            (current == null || block.size < current.size)
        ) {
            result = block
        }
    }
    return result
}

private fun SqlSourceTokenContext.isExpressionContinuation(): Boolean =
    !isClauseStart() &&
        (
            matches(SqlDialectSourcePatternRole.BooleanOperator) ||
                matches(SqlDialectSourcePatternRole.ExpressionContinuation) ||
                matches(SqlDialectSourcePatternRole.ParenthesizedExpressionContinuation)
        )

private fun SqlSourceTokenContext.isClauseStart(): Boolean =
    matches(SqlDialectSourcePatternRole.ClauseBoundary) ||
        matches(SqlDialectSourcePatternRole.MajorClauseStart)

private fun SqlSourceTokenContext.isCaseContinuation(structure: SqlSourceStructure): Boolean {
    val case = structure.innermostCaseContaining(this) ?: return false
    return index > case.startTokenIndex && token.normalizedText != "end"
}

private fun SqlSourceStructure.innermostCaseContaining(context: SqlSourceTokenContext): SqlSourceBlock? {
    var result: SqlSourceBlock? = null
    blocks.forEach { block ->
        val current = result
        if (
            block.kind == SqlSourceBlockKind.CaseExpression &&
            block.contains(context) &&
            (current == null || block.size < current.size)
        ) {
            result = block
        }
    }
    return result
}
