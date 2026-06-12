package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.rule.api.rangeAtOffsets

import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SqlSourceBlock
import dev.s7a.sqldelight.check.api.SqlSourceBlockKind
import dev.s7a.sqldelight.check.api.SqlSourceStructure
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext

/**
 * Reports redundant statement-level parentheses around top-level `SELECT` statements.
 */
public class NoUnnecessaryStatementParenthesesRule : Rule {
    override val id: RuleId = RuleId("no-unnecessary-statement-parentheses")
    override val defaultSeverity: Severity = Severity.Warning
    override val defaultEnable: Boolean = true

    override fun run(
        context: RuleContext,
        reporter: DiagnosticReporter,
    ) {
        val content = context.file.content
        val structure = SqlSourceStructure.parse(content, context.database.dialect.sourcePatterns)
        structure.blocks
            .filter { block -> block.kind == SqlSourceBlockKind.Subquery }
            .filter { block -> structure.isRedundantStatementParenthesizedSubquery(content, block) }
            .forEach { block ->
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = "Remove redundant parentheses around the SELECT statement.",
                        file = context.file,
                        range = content.rangeAtOffsets(block.startOffset, block.endOffset),
                        database = context.database,
                    ),
                )
            }
    }
}

private fun SqlSourceStructure.isRedundantStatementParenthesizedSubquery(
    content: String,
    block: SqlSourceBlock,
): Boolean =
    tokens[block.startTokenIndex].parenthesisDepth == 0 &&
        parentBlock(block)?.kind == SqlSourceBlockKind.Statement &&
        content.hasStatementBoundaryBefore(block.startOffset) &&
        content.hasStatementBoundaryAfter(block.endOffset)

private fun String.hasStatementBoundaryBefore(offset: Int): Boolean {
    val previous = previousSqlCharacterBefore(offset) ?: return true
    return previous.value == ';' || previous.value == ':'
}

private fun String.hasStatementBoundaryAfter(offset: Int): Boolean {
    val next = nextSqlCharacterAfter(offset) ?: return true
    return next.value == ';'
}
