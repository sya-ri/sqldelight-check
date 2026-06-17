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

private const val DEFAULT_INDENT_SIZE = 4

/**
 * Reports SQL lines whose indentation does not match their source structure.
 *
 * The rule only checks multiline statements. Clause starts align with their
 * source block nesting, while clause contents and expression continuations are
 * indented one level deeper. Subquery bodies are indented inside their
 * parenthesized source block.
 */
public class SourceIndentationRule : Rule {
    private val indentSizeOption by positiveIntOption("indentSize", DEFAULT_INDENT_SIZE)

    override val id: RuleId = RuleId("source-indentation")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val indentSize = context.options[indentSizeOption]
        val lines = content.linesWithRanges()
        val structure = SqlSourceStructure.parse(content, context.database.dialect.sourcePatterns)
        val statementBlocks =
            structure.blocks
                .filter { block -> block.kind == SqlSourceBlockKind.Statement }
                .associateBy { block -> block.statementIndex }

        lines.forEach { line ->
            val firstContentOffset = line.firstNonWhitespaceOffset ?: return@forEach
            val tokenContext = structure.contextAtOffset(firstContentOffset) ?: return@forEach
            if (tokenContext.isCreateIndexOnContinuation(structure)) return@forEach
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

private fun SqlSourceTokenContext.expectedIndentationLevel(
    structure: SqlSourceStructure,
): Int {
    val blockIndentation = sourceBlockDepth(structure)
    val subqueryBodyIndentation = subqueryBodyDepth(structure)
    val clauseIndentation = if (isClauseContinuation(structure)) 1 else 0
    val continuationIndentation = if (isExpressionContinuation()) 1 else 0
    val columnConstraintIndentation = if (isColumnConstraintContinuation(structure)) 1 else 0
    val caseIndentation = if (isCaseContinuation(structure)) 1 else 0
    val standaloneSubqueryAdjustment = if (isStandaloneSubqueryBody(structure)) -1 else 0
    return blockIndentation +
        subqueryBodyIndentation +
        maxOf(clauseIndentation, continuationIndentation) +
        columnConstraintIndentation +
        caseIndentation +
        standaloneSubqueryAdjustment
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

private fun SqlSourceTokenContext.isClosingTokenOfSubquery(structure: SqlSourceStructure): Boolean =
    token.normalizedText == ")" &&
        structure.blocks.any { block -> block.kind == SqlSourceBlockKind.Subquery && isClosingTokenOf(block) }

private fun SqlSourceTokenContext.isCreateIndexOnContinuation(structure: SqlSourceStructure): Boolean {
    if (token.normalizedText != "on") return false
    val statementTokens = structure.tokensInStatement(statementIndex)
    val first = statementTokens.firstOrNull() ?: return false
    return first.matches(SqlDialectSourcePatternRole.CreateIndexStatementStart)
}

private fun SqlSourceTokenContext.isClauseContinuation(structure: SqlSourceStructure): Boolean {
    if (isClauseStart()) return false
    if (isClosingTokenOfSubquery(structure) && !isInsideCaseExpression(structure)) return false
    if (isCreateTableItemStart(structure)) return false
    if (isCreateTableClosingParenthesis(structure)) return false
    val clause = structure.innermostClauseContaining(this) ?: return false
    if (clause.startsInsertListClause(structure) && isInsideParenthesizedBlockAfter(structure, clause.startTokenIndex)) {
        return false
    }
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

private fun SqlSourceTokenContext.isStandaloneSubqueryBody(structure: SqlSourceStructure): Boolean =
    !isInsideCaseExpression(structure) &&
        structure.blocks.any { block ->
            block.kind == SqlSourceBlockKind.Subquery &&
                block.contains(this) &&
                index > block.startTokenIndex &&
                !isClosingTokenOf(block)
        }

private fun SqlSourceTokenContext.isInsideCaseExpression(structure: SqlSourceStructure): Boolean =
    structure.innermostCaseContaining(this) != null

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

private fun SqlSourceTokenContext.isCreateTableItemStart(structure: SqlSourceStructure): Boolean {
    val previous = structure.previousToken(this) ?: return false
    if (previous.token.normalizedText != "," || previous.parenthesisDepth != parenthesisDepth) return false

    val statementTokens = structure.tokensInStatement(statementIndex)
    val first = statementTokens.firstOrNull() ?: return false
    if (!first.matches(SqlDialectSourcePatternRole.CreateTableStatementStart)) return false

    return parenthesisDepth == statementTokens.dropWhile { context -> context.index <= first.index }
        .firstOrNull { context -> context.token.normalizedText == "(" }
        ?.let { open -> open.parenthesisDepth + 1 }
}

private fun SqlSourceTokenContext.isCreateTableClosingParenthesis(structure: SqlSourceStructure): Boolean {
    if (token.normalizedText != ")") return false

    val statementTokens = structure.tokensInStatement(statementIndex)
    val first = statementTokens.firstOrNull() ?: return false
    if (!first.matches(SqlDialectSourcePatternRole.CreateTableStatementStart)) return false

    val open =
        statementTokens
            .dropWhile { context -> context.index <= first.index }
            .firstOrNull { context -> context.token.normalizedText == "(" }
            ?: return false
    return parenthesisDepth == open.parenthesisDepth + 1
}

private fun SqlSourceTokenContext.isColumnConstraintContinuation(structure: SqlSourceStructure): Boolean {
    if (!matches(SqlDialectSourcePatternRole.ColumnConstraintStart)) return false
    val previous = structure.previousToken(this) ?: return false
    return previous.token.normalizedText != "," &&
        previous.token.normalizedText != "(" &&
        previous.parenthesisDepth == parenthesisDepth
}

private fun SqlSourceStructure.previousToken(context: SqlSourceTokenContext): SqlSourceTokenContext? =
    tokens.getOrNull(context.index - 1)

private fun SqlSourceBlock.startsInsertListClause(structure: SqlSourceStructure): Boolean {
    val start = structure.tokens.getOrNull(startTokenIndex) ?: return false
    return start.token.normalizedText == "into" || start.token.normalizedText == "values"
}

private fun SqlSourceTokenContext.isInsideParenthesizedBlockAfter(
    structure: SqlSourceStructure,
    startTokenIndex: Int,
): Boolean =
    structure.blocks.any { block ->
        block.kind == SqlSourceBlockKind.ParenthesizedExpression &&
            block.contains(this) &&
            block.startTokenIndex > startTokenIndex &&
            index > block.startTokenIndex
    }

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
